require('dotenv').config();
const { Worker } = require('bullmq');
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs-extra');
const path = require('path');
const { spawn } = require('child_process');
const ffmpegPath = require('ffmpeg-static');
const OpenAI = require('openai');
const axios = require('axios');
const { connection } = require('./queue');

const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

const JOBS_TABLE = 'Video_jobs';
const TEMP_DIR = path.resolve(__dirname, 'storage', 'temp');
const OUTPUT_DIR = path.resolve(__dirname, 'storage', 'output');

// Shared status updater
async function updateJobStatus(jobId, updateData) {
    const status = updateData.status || 'processing';
    const stage = updateData.stage || 'working';
    const progress = updateData.progress || 0;
    
    console.log(`[JOB ${jobId}] ${stage}: ${progress}% - ${status}`);

    const payload = {
        status,
        stage,
        progress,
        video_url: updateData.video_url || updateData.videoUrl || null,
        result: updateData.result || null,
        error: updateData.error || null,
        updated_at: new Date().toISOString()
    };

    // Update Redis
    const Redis = require('ioredis');
    const redis = new Redis(process.env.REDIS_URL, { maxRetriesPerRequest: null });
    await redis.set(`job:${jobId}`, JSON.stringify(payload), 'EX', 3600);
    await redis.quit();

    // Update Supabase
    try {
        await supabase.from(JOBS_TABLE).update(payload).eq('id', jobId);
    } catch (e) {
        console.error("Supabase Sync Failed:", e.message);
    }
}

const worker = new Worker('video-generation', async job => {
    const { jobId, prompt, userId, type } = job.data;
    if (type === 'video') await processVideo(jobId, prompt, userId);
    else if (type === 'launchpad') await processLaunchpad(jobId, prompt, userId);
}, { connection });

// --- AI STARTUP LAUNCHPAD PIPELINE ---
async function processLaunchpad(jobId, prompt, userId) {
    try {
        // Stage 1: Idea Refinement (10%)
        await updateJobStatus(jobId, { status: 'processing', stage: 'idea', progress: 10 });
        const idea = await retryOperation(() => generateLaunchpadStep(prompt, "You are a Startup Architect. Refine this idea into a 1-sentence value proposition: "), 3);

        // Stage 2: Market Validation (25%)
        await updateJobStatus(jobId, { stage: 'validation', progress: 25 });
        const validation = await retryOperation(() => generateLaunchpadStep(idea, "Provide a brief market validation and target audience analysis for: "), 3);

        // Stage 3: Business Plan (40%)
        await updateJobStatus(jobId, { stage: 'plan', progress: 40 });
        const plan = await retryOperation(() => generateLaunchpadStep(idea, "Generate a 3-phase execution plan for: "), 3);

        // Stage 4: Technical Specs (60%)
        await updateJobStatus(jobId, { stage: 'build', progress: 60 });
        const buildSpecs = await retryOperation(() => generateLaunchpadStep(idea, "List the core technical features and tech stack required for: "), 3);

        // Stage 5: Deployment (80%)
        await updateJobStatus(jobId, { stage: 'deploy', progress: 80 });
        const appName = idea.split(' ').slice(0, 3).join(' ').replace(/[^a-zA-Z ]/g, "").trim();
        
        const finalResult = {
            appName: appName || "AI Startup",
            description: idea,
            validation: validation,
            plan: plan,
            features: buildSpecs,
            deploymentLink: `https://ibcn.site/apps/${jobId}`
        };

        await updateJobStatus(jobId, {
            status: 'completed',
            stage: 'done',
            progress: 100,
            result: finalResult
        });

    } catch (error) {
        console.error(`[LAUNCHPAD ${jobId}] Failed:`, error.message);
        await updateJobStatus(jobId, { status: "failed", error: error.message });
    }
}

// --- AI VIDEO PIPELINE ---
async function processVideo(jobId, prompt, userId) {
    const jobWorkDir = path.resolve(TEMP_DIR, jobId);
    const finalOutputFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);

    try {
        await fs.ensureDir(jobWorkDir);
        await fs.ensureDir(OUTPUT_DIR);

        await updateJobStatus(jobId, { status: 'processing', stage: 'script', progress: 10 });
        const scriptData = await retryOperation(() => generateVideoScript(prompt), 3);

        const sceneAssets = [];
        for (let i = 0; i < scriptData.scenes.length; i++) {
            await updateJobStatus(jobId, { stage: 'image', progress: 10 + (i / scriptData.scenes.length) * 20 });
            const imgPath = path.resolve(jobWorkDir, `img_${i}.png`);
            await retryOperation(() => generateImage(scriptData.scenes[i].visual, imgPath), 3).catch(async () => {
                const simple = await simplifyVisualPrompt(scriptData.scenes[i].visual);
                await generateImage(simple, imgPath);
            });

            await updateJobStatus(jobId, { stage: 'audio', progress: 30 + (i / scriptData.scenes.length) * 20 });
            const audPath = path.resolve(jobWorkDir, `aud_${i}.mp3`);
            await retryOperation(() => generateAudio(scriptData.scenes[i].text, audPath), 3);
            sceneAssets.push({ ...scriptData.scenes[i], imgPath, audPath, index: i });
        }

        await updateJobStatus(jobId, { stage: 'rendering', progress: 70 });
        const sceneVideos = [];
        for (const asset of sceneAssets) {
            const sceneVideoPath = path.resolve(jobWorkDir, `scene_${asset.index}.mp4`);
            await renderCinematicScene(asset, sceneVideoPath);
            sceneVideos.push(sceneVideoPath);
        }
        
        await updateJobStatus(jobId, { stage: 'merging', progress: 85 });
        await mergeScenes(sceneVideos, finalOutputFile, jobWorkDir);

        await updateJobStatus(jobId, { stage: 'uploading', progress: 95 });
        const fileBuffer = await fs.readFile(finalOutputFile);
        const fileName = `video_${jobId}.mp4`;
        await supabase.storage.from('videos').upload(fileName, fileBuffer, { contentType: 'video/mp4', upsert: true });
        const { data: { publicUrl } } = supabase.storage.from('videos').getPublicUrl(fileName);

        await updateJobStatus(jobId, { status: 'completed', stage: 'done', progress: 100, videoUrl: publicUrl });

    } catch (error) {
        await updateJobStatus(jobId, { status: "failed", error: error.message });
    } finally {
        await fs.remove(jobWorkDir).catch(() => {});
    }
}

async function renderCinematicScene(asset, outputPath) {
    const duration = asset.duration || 5;
    const totalFrames = duration * 30;
    const zoompan = `zoompan=z='min(zoom+0.0015,1.5)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=${totalFrames}:s=1080x1920`;
    return new Promise((resolve, reject) => {
        const args = ["-loop", "1", "-i", asset.imgPath, "-i", asset.audPath, "-filter_complex", `[0:v]scale=iw*2:ih*2,${zoompan},format=yuv420p[v];[1:a]loudnorm[a]`, "-map", "[v]", "-map", "[a]", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "30", "-t", duration.toString(), "-c:a", "aac", "-shortest", "-y", outputPath];
        spawn(ffmpegPath, args).on('close', (code) => code === 0 ? resolve() : reject(new Error("FFMPEG failed")));
    });
}

async function mergeScenes(videos, outputPath, workDir) {
    const listFile = path.resolve(workDir, 'list.txt');
    fs.writeFileSync(listFile, videos.map(v => `file '${path.resolve(v).replace(/\\/g, '/')}'`).join('\n'));
    return new Promise((resolve, reject) => {
        spawn(ffmpegPath, ["-f", "concat", "-safe", "0", "-i", listFile, "-c", "copy", "-y", outputPath]).on('close', (code) => code === 0 ? resolve() : reject(new Error("Merge failed")));
    });
}

async function retryOperation(fn, retries = 3) {
    let lastError;
    for (let i = 0; i < retries; i++) {
        try { return await fn(); }
        catch (e) { lastError = e; if (i < retries - 1) await new Promise(r => setTimeout(r, Math.pow(2, i) * 1000)); }
    }
    throw lastError;
}

async function generateLaunchpadStep(context, systemPrompt) {
    const response = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [{ role: "user", content: systemPrompt + context }]
    });
    return response.choices[0].message.content;
}

async function generateVideoScript(prompt) {
    const res = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [{ role: "system", content: "Return JSON: { \"scenes\": [{ \"text\": \"...\", \"visual\": \"...\", \"duration\": 5 }] }" }, { role: "user", content: prompt }],
        response_format: { type: "json_object" }
    });
    return JSON.parse(res.choices[0].message.content);
}

async function generateImage(visual, outputPath) {
    const response = await openai.images.generate({ model: "dall-e-3", prompt: `Cinematic 9:16 portrait: ${visual}`, size: "1024x1792" });
    const res = await axios.get(response.data[0].url, { responseType: 'arraybuffer' });
    await fs.writeFile(outputPath, Buffer.from(res.data));
}

async function simplifyVisualPrompt(complexPrompt) {
    const res = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [{ role: "user", content: `Simplify this for DALL-E: ${complexPrompt}` }]
    });
    return res.choices[0].message.content;
}

async function generateAudio(text, outputPath) {
    const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: text });
    await fs.writeFile(outputPath, Buffer.from(await mp3.arrayBuffer()));
}

console.log('🚀 WORKER ACTIVE: Ready for Video and Launchpad jobs...');
