require('dotenv').config();
const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8081;

app.use(cors({ origin: "*" }));
app.use(express.json());

// 1. INSTANT HEALTHCHECK (To pass Railway's network check)
app.get('/health', (req, res) => {
    res.status(200).send('OK');
});

// 2. START LISTENING IMMEDIATELY
app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 API BOOTED ON PORT ${PORT}`);
    
    // 3. INITIALIZE SERVICES LATER
    setTimeout(initServices, 1000);
});

async function initServices() {
    console.log("📦 Loading background modules...");
    try {
        const { createClient } = require('@supabase/supabase-js');
        const { v4: uuidv4 } = require('uuid');
        const Redis = require('ioredis');
        
        // Use try-require for queue/worker to prevent total crash
        let videoQueue;
        try {
            const queueMod = require('./queue');
            videoQueue = queueMod.videoQueue;
            require('./worker');
            console.log("✅ Worker and Queue Loaded");
        } catch (e) {
            console.error("❌ Failed to load Worker/Queue:", e.message);
        }

        const supabase = (process.env.SUPABASE_URL && process.env.SUPABASE_KEY)
            ? createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY)
            : null;

        const redis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL) : null;

        // Register the real endpoints
        app.post('/generate-video', async (req, res) => {
            if (!videoQueue) return res.status(503).json({ error: "Worker not ready" });
            const { prompt, userId } = req.body;
            const jobId = uuidv4();
            try {
                if (supabase) {
                    await supabase.from('Video_jobs').upsert({ id: jobId, user_id: userId || 'anon', prompt, status: 'queued' });
                }
                await videoQueue.add('generate-video', { jobId, prompt, userId, type: 'video' });
                res.status(202).json({ jobId, status: 'queued' });
            } catch (err) { res.status(500).json({ error: err.message }); }
        });

        app.get('/status/:jobId', async (req, res) => {
            if (!redis && !supabase) return res.status(503).json({ error: "DB not ready" });
            const { jobId } = req.params;
            try {
                let job = redis ? JSON.parse(await redis.get(`job:${jobId}`) || 'null') : null;
                if (!job && supabase) {
                    const { data } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
                    job = data;
                }
                res.json(job || { error: "Not found" });
            } catch (err) { res.status(500).json({ error: err.message }); }
        });

        console.log("🚀 ALL SERVICES INITIALIZED");
    } catch (err) {
        console.error("💥 CRITICAL INIT FAILURE:", err);
    }
}
