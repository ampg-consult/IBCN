require('dotenv').config();
const dns = require('node:dns');

// DEVOPS FIX: Force IPv4 first to prevent "Connection error" in cloud environments
try {
    dns.setDefaultResultOrder('ipv4first');
} catch (e) {
    console.warn("DNS setting not supported");
}

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
if (process.env.OPENAI_API_KEY && process.env.OPENAI_API_KEY.startsWith('sk-')) {
    try {
        openai = new OpenAI({ 
            apiKey: process.env.OPENAI_API_KEY,
            timeout: 60000,
            maxRetries: 3
        });
        console.log("✅ Worker: OpenAI Initialized");
    } catch (e) {
        console.error("❌ Worker: OpenAI Init Failed:", e.message);
    }
}

// 2. Persistent Redis Client for status updates
const statusRedis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL, { maxRetriesPerRequest: null }) : null;

// 3. Initialize Supabase
const supabaseUrl = process.env.SUPABASE_URL || '';
const supabaseKey = process.env.SUPABASE_KEY || '';
const supabase = (supabaseUrl && supabaseKey) ? createClient(supabaseUrl, supabaseKey) : null;

const JOBS_TABLE = 'Video_jobs';
const TEMP_DIR = path.resolve(__dirname, 'storage', 'temp');
const OUTPUT_DIR = path.resolve(__dirname, 'storage', 'output');

/**
 * Robust Status Updater - Standardized for completed status
 */
async function updateJobStatus(jobId, updateData) {
    const status = updateData.status || 'processing';
    const stage = updateData.stage || 'working';
    const progress = updateData.progress || 0;
    
    console.log(`JOB UPDATE: ${jobId} - ${stage}: ${progress}% - ${status}`);

    const payload = {
        status: status === 'READY' || status === 'done' ? 'completed' : status,
        stage, 
        progress,
        videoUrl: updateData.videoUrl || null,
        error: updateData.error || null,
        updated_at: new Date().toISOString()
    };

    // Update Redis Cache
    if (statusRedis) {
        try {
            await statusRedis.set(`job:${jobId}`, JSON.stringify(payload), 'EX', 3600);
        } catch (e) { console.error("Redis sync failed:", e.message); }
    }

    // Update Supabase DB
    if (supabase) {
        try {
            await supabase.from(JOBS_TABLE).update({
                status: payload.status,
                stage: payload.stage,
                progress: payload.progress,
                video_url: payload.videoUrl,
                error: payload.error,
                updated_at: payload.updated_at
            }).eq('id', jobId);
        } catch (e) { console.error("Supabase sync failed:", e.message); }
    }
    
    if (payload.status === 'completed') {
        console.log("🏁 FINAL JOB DATA:", jobId, payload);
    }
}

// 4. Initialize the Worker
const worker = new Worker('video-generation', async job => {
    if (!openai) throw new Error("OpenAI API Key is missing or invalid.");
    
    const { jobId, prompt, userId, type } = job.data;
    console.log(`🚀 Processing ${type} job: ${jobId}`);
    
    try {
        if (type === 'video') await processVideo(jobId, prompt, userId);
        else if (type === 'launchpad') await processLaunchpad(jobId, prompt, userId);
    } catch (err) {
        console.error(`💥 Fatal error in job ${jobId}:`, err);
        await updateJobStatus(jobId, { status: "failed", error: err.message, stage: 'failed' });
    }
}, { 
    connection,
    concurrency: 1 
});

// --- AI PIPELINES ---

async function processVideo(jobId, prompt, userId) {
    const jobWorkDir = path.resolve(TEMP_DIR, jobId);
    const finalOutputFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);
    
    try {
        await fs.ensureDir(jobWorkDir);
        await fs.ensureDir(OUTPUT_DIR);
        
        // 1. Script Stage
        await updateJobStatus(jobId, { stage: 'script', progress: 10 });
        const scriptData = await generateVideoScript(prompt);
        
        const sceneAssets = [];
        for (let i = 0; i < scriptData.scenes.length; i++) {
            const imgPath = path.resolve(jobWorkDir, `img_${i}.png`);
            const audPath = path.resolve(jobWorkDir, `aud_${i}.mp3`);
            
            // 2. Image Stage
            await updateJobStatus(jobId, { stage: 'image', progress: 10 + Math.floor((i / scriptData.scenes.length) * 20) });
            await generateImage(scriptData.scenes[i].visual, imgPath);
            
            // 3. Audio Stage
            await updateJobStatus(jobId, { stage: 'audio', progress: 30 + Math.floor((i / scriptData.scenes.length) * 20) });
            await generateAudio(scriptData.scenes[i].text, audPath);
            
            sceneAssets.push({ ...scriptData.scenes[i], imgPath, audPath, index: i });
        }

        // 4. Rendering Stage
        await updateJobStatus(jobId, { stage: 'rendering', progress: 70 });
        const sceneVideos = [];
        for (const asset of sceneAssets) {
            const sceneVideoPath = path.resolve(jobWorkDir, `scene_${asset.index}.mp4`);
            await renderCinematicScene(asset, sceneVideoPath);
            sceneVideos.push(sceneVideoPath);
        }

        // 5. Merging Stage
        await updateJobStatus(jobId, { stage: 'merging', progress: 85 });
        await mergeScenes(sceneVideos, finalOutputFile, jobWorkDir);

        // 6. Uploading Stage
        if (supabase) {
            await updateJobStatus(jobId, { stage: 'uploading', progress: 95 });
            const fileBuffer = await fs.readFile(finalOutputFile);
            const fileName = `video_${jobId}.mp4`;
            
            const { error: uploadError } = await supabase.storage.from('videos').upload(fileName, fileBuffer, { contentType: 'video/mp4', upsert: true });
            if (uploadError) throw uploadError;

            // Generate Public URL correctly
            const { data } = supabase.storage.from('videos').getPublicUrl(fileName);
            const publicUrl = data.publicUrl;
            
            // 7. Done / Completed Stage
            await updateJobStatus(jobId, { 
                status: 'completed', 
                stage: 'done', 
                progress: 100, 
                videoUrl: publicUrl 
            });
        }
    } catch (err) {
        throw err;
    } finally {
        await fs.remove(jobWorkDir).catch(() => {});
    }
}

// helpers
async function renderCinematicScene(asset, outputPath) {
    const duration = asset.duration || 5;
    const args = ["-loop", "1", "-i", asset.imgPath, "-i", asset.audPath, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "30", "-t", duration.toString(), "-c:a", "aac", "-shortest", "-y", outputPath];
    return new Promise((resolve, reject) => {
        const proc = spawn('ffmpeg', args);
        proc.on('close', (code) => code === 0 ? resolve() : reject(new Error(`FFMPEG error ${code}`)));
    });
}

async function mergeScenes(videos, outputPath, workDir) {
    const listFile = path.resolve(workDir, 'list.txt');
    fs.writeFileSync(listFile, videos.map(v => `file '${v}'`).join('\n'));
    return new Promise((resolve, reject) => {
        const proc = spawn('ffmpeg', ["-f", "concat", "-safe", "0", "-i", listFile, "-c", "copy", "-y", outputPath]);
        proc.on('close', (code) => code === 0 ? resolve() : reject(new Error(`Merge error ${code}`)));
    });
}

async function generateVideoScript(prompt) {
    const res = await openai.chat.completions.create({ model: "gpt-4o-mini", messages: [{ role: "system", content: "Return JSON: { \"scenes\": [{ \"text\": \"...\", \"visual\": \"...\", \"duration\": 5 }] }" }, { role: "user", content: prompt }], response_format: { type: "json_object" } });
    return JSON.parse(res.choices[0].message.content);
}

async function generateImage(visual, outputPath) {
    const response = await openai.images.generate({ model: "dall-e-3", prompt: `Cinematic 9:16 portrait: ${visual}`, size: "1024x1792" });
    const res = await axios.get(response.data[0].url, { responseType: 'arraybuffer', timeout: 30000 });
    await fs.writeFile(outputPath, Buffer.from(res.data));
}

async function generateAudio(text, outputPath) {
    const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: text });
    await fs.writeFile(outputPath, Buffer.from(await mp3.arrayBuffer()));
}

async function processLaunchpad(jobId, prompt, userId) {
    await updateJobStatus(jobId, { status: 'processing', stage: 'idea', progress: 10 });
    const res = await openai.chat.completions.create({ model: "gpt-4o-mini", messages: [{ role: "user", content: "Startup Idea: " + prompt }] });
    await updateJobStatus(jobId, { status: 'completed', stage: 'done', progress: 100, result: { appName: "AI Startup", description: res.choices[0].message.content } });
}

console.log('🚀 WORKER MODULE READY');
