require('dotenv').config();
const dns = require('node:dns');

try {
    dns.setDefaultResultOrder('ipv4first');
} catch (e) {}

const { Worker } = require('bullmq');
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs-extra');
const path = require('path');
const { spawn } = require('child_process');
const OpenAI = require('openai');
const axios = require('axios');
const Redis = require('ioredis');
const { connection } = require('./queue');

console.log("🛠️ WORKER MODULE LOADING...");

let openai;
if (process.env.OPENAI_API_KEY) {
    openai = new OpenAI({ 
        apiKey: process.env.OPENAI_API_KEY,
        timeout: 45000
    });
}

const statusRedis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL) : null;
const supabase = (process.env.SUPABASE_URL && process.env.SUPABASE_KEY) ? createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY) : null;

const JOBS_TABLE = 'Video_jobs';
const TEMP_DIR = path.resolve(__dirname, 'storage', 'temp');
const OUTPUT_DIR = path.resolve(__dirname, 'storage', 'output');

async function updateJobStatus(jobId, updateData) {
    const status = updateData.status || 'processing';
    const stage = updateData.stage || 'working';
    const progress = updateData.progress || 0;
    const videoUrl = updateData.videoUrl || null;
    const error = updateData.error || null;

    const payload = { status, stage, progress, videoUrl, error, updated_at: new Date().toISOString() };

    // 1. Update Persistent Stores
    if (statusRedis) await statusRedis.set(`job:${jobId}`, JSON.stringify(payload), 'EX', 3600).catch(console.error);
    if (supabase) await supabase.from(JOBS_TABLE).update({
        status, stage, progress, video_url: videoUrl, error, updated_at: payload.updated_at
    }).eq('id', jobId).catch(console.error);

    // 2. PUSH REAL-TIME SSE UPDATE (SENIOR DEVOPS UPGRADE)
    if (global.pushUpdate) {
        global.pushUpdate(jobId, payload);
    }
}

const worker = new Worker('video-generation', async job => {
    const { jobId, prompt } = job.data;
    console.log(`🚀 PROCESSING: ${jobId}`);

    try {
        const workDir = path.resolve(TEMP_DIR, jobId);
        await fs.ensureDir(workDir);
        await fs.ensureDir(OUTPUT_DIR);

        // --- STAGE 1: SCRIPT (15%) ---
        await updateJobStatus(jobId, { stage: 'script', progress: 15 });
        const scriptRes = await openai.chat.completions.create({
            model: "gpt-4o-mini",
            messages: [{ role: "user", content: `Video prompt: ${prompt}. Return JSON: { "scenes": [{"text": "narration", "visual": "dalle_prompt"}] }` }],
            response_format: { type: "json_object" }
        });
        const scriptData = JSON.parse(scriptRes.choices[0].message.content);

        // --- STAGE 2: ASSETS (50%) ---
        const sceneAssets = [];
        for (let i = 0; i < Math.min(scriptData.scenes.length, 3); i++) {
            await updateJobStatus(jobId, { stage: 'image', progress: 20 + (i * 10) });
            const imgRes = await openai.images.generate({ model: "dall-e-3", prompt: scriptData.scenes[i].visual });
            const imgPath = path.resolve(workDir, `img_${i}.png`);
            const imgBuffer = await axios.get(imgRes.data[0].url, { responseType: 'arraybuffer' });
            await fs.writeFile(imgPath, Buffer.from(imgBuffer.data));

            await updateJobStatus(jobId, { stage: 'audio', progress: 25 + (i * 10) });
            const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: scriptData.scenes[i].text });
            const audPath = path.resolve(workDir, `aud_${i}.mp3`);
            await fs.writeFile(audPath, Buffer.from(await mp3.arrayBuffer()));
            
            sceneAssets.push({ imgPath, audPath });
        }

        // --- STAGE 3: RENDERING (80%) ---
        await updateJobStatus(jobId, { stage: 'rendering', progress: 80 });
        const finalFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);
        await new Promise((resolve, reject) => {
            const args = ["-loop", "1", "-i", sceneAssets[0].imgPath, "-i", sceneAssets[0].audPath, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-t", "5", "-y", finalFile];
            spawn('ffmpeg', args).on('close', code => code === 0 ? resolve() : reject(new Error("FFMPEG error")));
        });

        // --- STAGE 4: UPLOAD (95%) ---
        await updateJobStatus(jobId, { stage: 'uploading', progress: 95 });
        const fileBuffer = await fs.readFile(finalFile);
        const fileName = `video_${jobId}.mp4`;
        await supabase.storage.from('videos').upload(fileName, fileBuffer, { contentType: 'video/mp4', upsert: true });
        const { data } = supabase.storage.from('videos').getPublicUrl(fileName);

        // --- FINAL: COMPLETE (100%) ---
        await updateJobStatus(jobId, { 
            status: 'completed', stage: 'done', progress: 100, videoUrl: data.publicUrl 
        });

    } catch (err) {
        console.error(`💥 ERROR:`, err.message);
        await updateJobStatus(jobId, { status: "failed", error: err.message });
    } finally {
        await fs.remove(path.resolve(TEMP_DIR, jobId)).catch(() => {});
    }
}, { connection, concurrency: 1 });

console.log("🚀 WORKER READY");
