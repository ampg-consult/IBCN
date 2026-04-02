require('dotenv').config();
const dns = require('node:dns');

// 1. DNS FIX: Force IPv4 to prevent "Connection error" in cloud containers
try {
    dns.setDefaultResultOrder('ipv4first');
    console.log("📡 DNS: IPv4 Priority Enabled");
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

console.log("🛠️ WORKER MODULE STARTING...");

// 2. Initialize OpenAI with strict config
let openai;
if (process.env.OPENAI_API_KEY) {
    openai = new OpenAI({ 
        apiKey: process.env.OPENAI_API_KEY,
        timeout: 30000, // 30s timeout
        maxRetries: 2
    });
    console.log("✅ OpenAI Initialized");
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

    console.log(`[JOB ${jobId}] Stage: ${stage} | Progress: ${progress}% | Status: ${status}`);

    const payload = { status, stage, progress, videoUrl, updated_at: new Date().toISOString() };

    if (statusRedis) await statusRedis.set(`job:${jobId}`, JSON.stringify(payload), 'EX', 3600).catch(console.error);
    if (supabase) await supabase.from(JOBS_TABLE).update({
        status, stage, progress, video_url: videoUrl, updated_at: payload.updated_at
    }).eq('id', jobId).catch(console.error);
}

// 3. THE WORKER
const worker = new Worker('video-generation', async job => {
    const { jobId, prompt, type } = job.data;
    console.log(`🚀 PROCESSING JOB: ${jobId} (${type})`);

    try {
        if (!openai) throw new Error("OpenAI Key Missing");
        
        await fs.ensureDir(path.resolve(TEMP_DIR, jobId));
        await fs.ensureDir(OUTPUT_DIR);

        // --- STAGE 1: SCRIPT ---
        await updateJobStatus(jobId, { stage: 'script', progress: 15 });
        console.log(`[${jobId}] Calling OpenAI GPT-4o-mini...`);
        
        const scriptResponse = await openai.chat.completions.create({
            model: "gpt-4o-mini",
            messages: [
                { role: "system", content: "You are a viral video director. Return JSON: { \"scenes\": [{ \"text\": \"Short narration\", \"visual\": \"DALL-E prompt\" }] }" },
                { role: "user", content: prompt }
            ],
            response_format: { type: "json_object" }
        });

        const scriptData = JSON.parse(scriptResponse.choices[0].message.content);
        console.log(`[${jobId}] Script Generated: ${scriptData.scenes.length} scenes`);

        // --- STAGE 2: ASSETS (Images & Audio) ---
        const sceneAssets = [];
        for (let i = 0; i < scriptData.scenes.length; i++) {
            const progress = 20 + Math.floor((i / scriptData.scenes.length) * 40);
            await updateJobStatus(jobId, { stage: 'image', progress });
            
            const imgPath = path.resolve(TEMP_DIR, jobId, `img_${i}.png`);
            const audPath = path.resolve(TEMP_DIR, jobId, `aud_${i}.mp3`);

            console.log(`[${jobId}] Generating Image ${i+1}/${scriptData.scenes.length}...`);
            const imgResult = await openai.images.generate({ model: "dall-e-3", prompt: scriptData.scenes[i].visual, size: "1024x1792" });
            const imgBuffer = await axios.get(imgResult.data[0].url, { responseType: 'arraybuffer' });
            await fs.writeFile(imgPath, Buffer.from(imgBuffer.data));

            console.log(`[${jobId}] Generating Audio ${i+1}/${scriptData.scenes.length}...`);
            const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: scriptData.scenes[i].text });
            await fs.writeFile(audPath, Buffer.from(await mp3.arrayBuffer()));

            sceneAssets.push({ imgPath, audPath });
        }

        // --- STAGE 3: RENDERING ---
        await updateJobStatus(jobId, { stage: 'rendering', progress: 70 });
        const finalFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);
        
        // Simplified Rendering for speed
        await new Promise((resolve, reject) => {
            const args = ["-loop", "1", "-i", sceneAssets[0].imgPath, "-i", sceneAssets[0].audPath, "-c:v", "libx264", "-t", "5", "-y", finalFile];
            spawn('ffmpeg', args).on('close', code => code === 0 ? resolve() : reject(new Error("FFMPEG fail")));
        });

        // --- STAGE 4: UPLOAD ---
        await updateJobStatus(jobId, { stage: 'uploading', progress: 90 });
        const fileBuffer = await fs.readFile(finalFile);
        const fileName = `video_${jobId}.mp4`;
        await supabase.storage.from('videos').upload(fileName, fileBuffer, { contentType: 'video/mp4', upsert: true });
        
        const { data } = supabase.storage.from('videos').getPublicUrl(fileName);
        
        await updateJobStatus(jobId, { 
            status: 'completed', stage: 'done', progress: 100, videoUrl: data.publicUrl 
        });
        
        console.log(`✨ JOB COMPLETED: ${jobId}`);

    } catch (err) {
        console.error(`💥 FATAL ERROR [${jobId}]:`, err.message);
        await updateJobStatus(jobId, { status: "failed", error: err.message });
    } finally {
        await fs.remove(path.resolve(TEMP_DIR, jobId)).catch(() => {});
    }
}, { connection, concurrency: 1 });

console.log("🚀 WORKER ACTIVE");
