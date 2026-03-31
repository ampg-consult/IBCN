require('dotenv').config();
const { Worker } = require('bullmq');
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs-extra');
const path = require('path');
const { spawn } = require('child_process');
const OpenAI = require('openai');
const axios = require('axios');
const Redis = require('ioredis');
const { connection } = require('./queue');

console.log("🛠️ Worker Module Loading...");

// 1. Initialize OpenAI
let openai;
if (process.env.OPENAI_API_KEY) {
    try {
        openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
        console.log("✅ Worker: OpenAI Initialized");
    } catch (e) {
        console.error("❌ Worker: OpenAI Init Failed:", e.message);
    }
}

// 2. Initialize Supabase
const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseKey = process.env.SUPABASE_KEY || '';
const supabase = (supabaseUrl && supabaseKey) ? createClient(supabaseUrl, supabaseKey) : null;

const JOBS_TABLE = 'Video_jobs';
const TEMP_DIR = path.resolve(__dirname, 'storage', 'temp');
const OUTPUT_DIR = path.resolve(__dirname, 'storage', 'output');

// 3. Status Updater
async function updateJobStatus(jobId, updateData) {
    const status = updateData.status || 'processing';
    const stage = updateData.stage || 'working';
    const progress = updateData.progress || 0;
    
    console.log(`[JOB ${jobId}] ${stage}: ${progress}% - ${status}`);

    const payload = {
        status, stage, progress,
        video_url: updateData.video_url || updateData.videoUrl || null,
        result: updateData.result || null,
        error: updateData.error || null,
        updated_at: new Date().toISOString()
    };

    // Update Redis for polling
    if (process.env.REDIS_URL) {
        try {
            const redis = new Redis(process.env.REDIS_URL, { maxRetriesPerRequest: null });
            await redis.set(`job:${jobId}`, JSON.stringify(payload), 'EX', 3600);
            await redis.quit();
        } catch (e) { console.error("Redis status update failed:", e.message); }
    }

    // Update Supabase for persistence
    if (supabase) {
        try {
            await supabase.from(JOBS_TABLE).update(payload).eq('id', jobId);
        } catch (e) { console.error("Supabase status update failed:", e.message); }
    }
}

// 4. Initialize the Worker
const worker = new Worker('video-generation', async job => {
    if (!openai) throw new Error("OpenAI API Key is missing or invalid.");
    
    const { jobId, prompt, userId, type } = job.data;
    console.log(`🚀 Processing ${type} job: ${jobId}`);
    
    if (type === 'video') await processVideo(jobId, prompt, userId);
    else if (type === 'launchpad') await processLaunchpad(jobId, prompt, userId);
}, { 
    connection,
    concurrency: 1 // Process one video at a time to save memory
});

worker.on('failed', (job, err) => {
    console.error(`❌ Job ${job.id} failed:`, err.message);
});

// --- AI PIPELINES ---

async function processLaunchpad(jobId, prompt, userId) {
    try {
        await updateJobStatus(jobId, { status: 'processing', stage: 'idea', progress: 10 });
        const idea = await generateLaunchpadStep(prompt, "Refine into 1-sentence value prop: ");
        
        await updateJobStatus(jobId, { stage: 'validation', progress: 30 });
        const validation = await generateLaunchpadStep(idea, "Provide market validation for: ");
        
        await updateJobStatus(jobId, { stage: 'plan', progress: 60 });
        const plan = await generateLaunchpadStep(idea, "Generate 3-phase execution plan for: ");

        await updateJobStatus(jobId, {
            status: 'completed', stage: 'done', progress: 100,
            result: { appName: "AI Startup", description: idea, validation, plan }
        });
    } catch (error) {
        await updateJobStatus(jobId, { status: "failed", error: error.message });
    }
}

async function processVideo(jobId, prompt, userId) {
    const jobWorkDir = path.resolve(TEMP_DIR, jobId);
    const finalOutputFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);
    try {
        await fs.ensureDir(jobWorkDir);
        await fs.ensureDir(OUTPUT_DIR);
        
        await updateJobStatus(jobId, { status: 'processing', stage: 'script', progress: 10 });
        const scriptData = await generateVideoScript(prompt);
        
        // Simplified loop for reliability
        const sceneAssets = [];
        for (let i = 0; i < scriptData.scenes.length; i++) {
            const imgPath = path.resolve(jobWorkDir, `img_${i}.png`);
            const audPath = path.resolve(jobWorkDir, `aud_${i}.mp3`);
            
            await generateImage(scriptData.scenes[i].visual, imgPath);
            await generateAudio(scriptData.scenes[i].text, audPath);
            
            sceneAssets.push({ ...scriptData.scenes[i], imgPath, audPath, index: i });
            await updateJobStatus(jobId, { progress: 10 + ((i+1)/scriptData.scenes.length) * 50 });
        }

        await updateJobStatus(jobId, { stage: 'rendering', progress: 70 });
        const sceneVideos = [];
        for (const asset of sceneAssets) {
            const sceneVideoPath = path.resolve(jobWorkDir, `scene_${asset.index}.mp4`);
            await renderCinematicScene(asset, sceneVideoPath);
            sceneVideos.push(sceneVideoPath);
        }

        await updateJobStatus(jobId, { stage: 'merging', progress: 90 });
        await mergeScenes(sceneVideos, finalOutputFile, jobWorkDir);

        if (supabase) {
            const fileBuffer = await fs.readFile(finalOutputFile);
            const fileName = `video_${jobId}.mp4`;
            await supabase.storage.from('videos').upload(fileName, fileBuffer, { contentType: 'video/mp4', upsert: true });
            const { data: { publicUrl } } = supabase.storage.from('videos').getPublicUrl(fileName);
            await updateJobStatus(jobId, { status: 'completed', stage: 'done', progress: 100, videoUrl: publicUrl });
        }
    } catch (error) {
        console.error("Video processing error:", error);
        await updateJobStatus(jobId, { status: "failed", error: error.message });
    } finally {
        await fs.remove(jobWorkDir).catch(() => {});
    }
}

async function renderCinematicScene(asset, outputPath) {
    const duration = asset.duration || 5;
    const zoompan = `zoompan=z='min(zoom+0.0015,1.5)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=${duration*30}:s=1080x1920`;
    return new Promise((resolve, reject) => {
        const args = ["-loop", "1", "-i", asset.imgPath, "-i", asset.audPath, "-filter_complex", `[0:v]scale=iw*2:ih*2,${zoompan},format=yuv420p[v];[1:a]loudnorm[a]`, "-map", "[v]", "-map", "[a]", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "30", "-t", duration.toString(), "-c:a", "aac", "-shortest", "-y", outputPath];
        spawn('ffmpeg', args).on('close', (code) => code === 0 ? resolve() : reject(new Error("FFMPEG failed")));
    });
}

async function mergeScenes(videos, outputPath, workDir) {
    const listFile = path.resolve(workDir, 'list.txt');
    fs.writeFileSync(listFile, videos.map(v => `file '${path.resolve(v).replace(/\\/g, '/')}'`).join('\n'));
    return new Promise((resolve, reject) => {
        spawn('ffmpeg', ["-f", "concat", "-safe", "0", "-i", listFile, "-c", "copy", "-y", outputPath]).on('close', (code) => code === 0 ? resolve() : reject(new Error("Merge failed")));
    });
}

async function generateLaunchpadStep(context, systemPrompt) {
    const response = await openai.chat.completions.create({ model: "gpt-4o-mini", messages: [{ role: "user", content: systemPrompt + context }] });
    return response.choices[0].message.content;
}

async function generateVideoScript(prompt) {
    const res = await openai.chat.completions.create({ model: "gpt-4o-mini", messages: [{ role: "system", content: "Return JSON: { \"scenes\": [{ \"text\": \"...\", \"visual\": \"...\", \"duration\": 5 }] }" }, { role: "user", content: prompt }], response_format: { type: "json_object" } });
    return JSON.parse(res.choices[0].message.content);
}

async function generateImage(visual, outputPath) {
    const response = await openai.images.generate({ model: "dall-e-3", prompt: `Cinematic 9:16 portrait: ${visual}`, size: "1024x1792" });
    const res = await axios.get(response.data[0].url, { responseType: 'arraybuffer' });
    await fs.writeFile(outputPath, Buffer.from(res.data));
}

async function generateAudio(text, outputPath) {
    const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: text });
    await fs.writeFile(outputPath, Buffer.from(await mp3.arrayBuffer()));
}

console.log('🚀 WORKER MODULE READY');
