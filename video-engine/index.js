require('dotenv').config();
const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8081;

// 1. MIDDLEWARE
app.use(cors({ origin: "*" }));
app.use(express.json());

// 2. INSTANT HEALTHCHECK (Railway needs this to be 'Active')
app.get('/health', (req, res) => {
    res.status(200).send('OK');
});

app.get('/', (req, res) => {
    res.status(200).json({ status: "IBCN Engine Online", port: PORT });
});

// 3. START SERVER IMMEDIATELY
app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 SERVER LISTENING ON PORT ${PORT}`);
    
    // 4. DELAYED SAFE INITIALIZATION
    setTimeout(initializeBackgroundServices, 500);
});

async function initializeBackgroundServices() {
    console.log("📦 Initializing AI background services...");
    try {
        const { createClient } = require('@supabase/supabase-js');
        const { v4: uuidv4 } = require('uuid');
        const Redis = require('ioredis');

        // Initialize Supabase Safely
        let supabase = null;
        if (process.env.SUPABASE_URL && process.env.SUPABASE_KEY && process.env.SUPABASE_URL.startsWith('http')) {
            try {
                supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
                console.log("✅ Supabase Initialized");
            } catch (e) { console.error("❌ Supabase Error:", e.message); }
        }

        // Initialize Redis Safely
        const redis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL) : null;
        if (redis) console.log("✅ Redis Initialized");

        // Load Queue and Worker
        const { videoQueue } = require('./queue');
        require('./worker');
        console.log("✅ AI Worker/Queue Loaded");

        // Register Functional Endpoints
        registerEndpoints(app, supabase, redis, videoQueue, uuidv4);

    } catch (err) {
        console.error("💥 Critical Background Error:", err.message);
    }
}

function registerEndpoints(app, supabase, redis, videoQueue, uuidv4) {
    app.post('/generate-video', async (req, res) => {
        const { prompt, userId } = req.body;
        const jobId = uuidv4();
        try {
            if (supabase) await supabase.from('Video_jobs').upsert({ id: jobId, user_id: userId || 'anon', prompt, status: 'queued' });
            await videoQueue.add('generate-video', { jobId, prompt, userId, type: 'video' });
            res.status(202).json({ jobId, status: 'queued' });
        } catch (e) { res.status(500).json({ error: e.message }); }
    });

    app.get('/status/:jobId', async (req, res) => {
        const { jobId } = req.params;
        try {
            let job = redis ? JSON.parse(await redis.get(`job:${jobId}`) || 'null') : null;
            if (!job && supabase) {
                const { data } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
                job = data;
            }
            res.json(job || { error: "Job not found" });
        } catch (e) { res.status(500).json({ error: e.message }); }
    });
}
