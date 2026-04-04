require('dotenv').config();
const dns = require('node:dns');

// DEVOPS FIX: Force IPv4 first to prevent "Connection error" in cloud environments
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
const { connection } = require('./queue');

console.log("🛠️ WORKER MODULE LOADING...");

// 1. Initialize OpenAI
let openai;
if (process.env.OPENAI_API_KEY) {
    openai = new OpenAI({ 
        apiKey: process.env.OPENAI_API_KEY,
        timeout: 60000,
        maxRetries: 3
    });
    console.log("✅ OpenAI Initialized");
}

// 2. Initialize Supabase
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

const TEMP_DIR = path.resolve(__dirname, 'storage', 'temp');
const OUTPUT_DIR = path.resolve(__dirname, 'storage', 'output');

// 3. THE WORKER
const worker = new Worker('video-generation', async job => {
    const { jobId, prompt } = job.data;
    console.log(`🚀 PROCESSING VIDEO: ${jobId}`);

    try {
        const workDir = path.resolve(TEMP_DIR, jobId);
        await fs.ensureDir(workDir);
        await fs.ensureDir(OUTPUT_DIR);

        // --- STAGE 1: SCRIPT (10%) ---
        global.updateJob(jobId, { stage: 'script', progress: 10 });
        const scriptRes = await openai.chat.completions.create({
            model: "gpt-4o-mini",
            messages: [{ role: "system", content: "You are a viral video director. Return JSON: { \"scenes\": [{\"text\": \"narration\", \"visual\": \"visual_prompt\"}] }" }, { role: "user", content: prompt }],
            response_format: { type: "json_object" }
        });
        const scriptData = JSON.parse(scriptRes.choices[0].message.content);

        // --- STAGE 2: ASSETS (Images 30%, Audio 50%) ---
        const sceneAssets = [];
        for (let i = 0; i < Math.min(scriptData.scenes.length, 3); i++) {
            global.updateJob(jobId, { stage: 'image', progress: 15 + (i * 10) });
            const imgRes = await openai.images.generate({ model: "dall-e-3", prompt: scriptData.scenes[i].visual });
            const imgPath = path.resolve(workDir, `img_${i}.png`);
            const imgBuffer = await axios.get(imgRes.data[0].url, { responseType: 'arraybuffer' });
            await fs.writeFile(imgPath, Buffer.from(imgBuffer.data));

            global.updateJob(jobId, { stage: 'audio', progress: 35 + (i * 10) });
            const mp3 = await openai.audio.speech.create({ model: "tts-1", voice: "onyx", input: scriptData.scenes[i].text });
            const audPath = path.resolve(workDir, `aud_${i}.mp3`);
            await fs.writeFile(audPath, Buffer.from(await mp3.arrayBuffer()));
            
            sceneAssets.push({ imgPath, audPath });
        }

        // --- STAGE 3: RENDERING (70%) ---
        global.updateJob(jobId, { stage: 'rendering', progress: 70 });
        const finalFile = path.resolve(OUTPUT_DIR, `${jobId}.mp4`);
        await new Promise((resolve, reject) => {
            const args = ["-loop", "1", "-i", sceneAssets[0].imgPath, "-i", sceneAssets[0].audPath, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-t", "5", "-y", finalFile];
            spawn('ffmpeg', args).on('close', code => code === 0 ? resolve() : reject(new Error("FFMPEG failed")));
        });

        // --- STAGE 4: MERGING (85%) ---
        global.updateJob(jobId, { stage: 'merging', progress: 85 });

        // --- STAGE 5: UPLOADING (95%) ---
        global.updateJob(jobId, { stage: 'uploading', progress: 95 });
        const fileBuffer = await fs.readFile(finalFile);
        const fileName = `video_${jobId}.mp4`;
        
        await supabase.storage.from('videos').upload(fileName, fileBuffer, { 
            contentType: 'video/mp4', 
            upsert: true 
        });
        
        const { data } = supabase.storage.from('videos').getPublicUrl(fileName);
        const publicUrl = data.publicUrl;

        // --- STAGE 6: DONE (100%) ---
        global.updateJob(jobId, { 
            status: 'completed', 
            stage: 'done', 
            progress: 100, 
            videoUrl: publicUrl 
        });

    } catch (err) {
        console.error(`💥 FATAL ERROR:`, err.message);
        global.updateJob(jobId, { status: "failed", error: err.message });
    } finally {
        await fs.remove(path.resolve(TEMP_DIR, jobId)).catch(() => {});
    }
}, { connection, concurrency: 1 });

console.log("🚀 WORKER ACTIVE");
