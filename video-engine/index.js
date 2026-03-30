// GLOBAL ERROR CATCHING (MUST BE AT THE TOP)
process.on('uncaughtException', (err) => {
    console.error('💥 UNCAUGHT EXCEPTION:', err.stack || err);
    process.exit(1);
});
process.on('unhandledRejection', (reason, promise) => {
    console.error('💥 UNHANDLED REJECTION:', reason);
});

console.log("🚀 STARTING IBCN AI ENGINE...");

require('dotenv').config();
const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const Redis = require('ioredis');
const dns = require('node:dns');

// Fix for fetch errors in some container environments
try {
    dns.setDefaultResultOrder('ipv4first');
} catch (e) {
    console.warn("DNS setting not supported");
}

console.log("📦 Initializing Worker and Queue...");
const { videoQueue, connection: queueConnection } = require('./queue');
require('./worker');

const app = express();
app.use(cors({ origin: "*", methods: ["GET", "POST"] }));
app.use(express.json());

// Initialize Cloud Services
let supabase;
try {
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_KEY;
    if (!supabaseUrl || !supabaseKey) {
        console.error("❌ SUPABASE_URL or SUPABASE_KEY is missing.");
    } else {
        supabase = createClient(supabaseUrl, supabaseKey);
        console.log("✅ Supabase Client Initialized");
    }
} catch (e) {
    console.error("❌ Failed to initialize Supabase:", e.message);
}

let redis;
try {
    if (process.env.REDIS_URL) {
        redis = new Redis(process.env.REDIS_URL, { 
            maxRetriesPerRequest: null,
            enableReadyCheck: false
        });
        console.log("✅ Redis Client (API) Initialized");
    } else {
        console.error("❌ REDIS_URL is missing.");
    }
} catch (e) {
    console.error("❌ Failed to initialize Redis (API):", e.message);
}

const JOBS_TABLE = 'Video_jobs';

app.get('/health', (req, res) => res.status(200).json({ 
    status: "ok", 
    service: "IBCN AI Engine",
    checks: {
        redis: redis ? "connected" : "missing",
        supabase: supabase ? "connected" : "missing",
        openai: process.env.OPENAI_API_KEY ? "present" : "missing"
    } 
}));

app.post('/generate-video', async (req, res) => {
    const { prompt, userId, jobId: requestedJobId } = req.body;
    if (!prompt) return res.status(400).json({ error: 'Prompt is required' });
    if (!supabase) return res.status(503).json({ error: 'Database service unavailable' });
    
    const jobId = requestedJobId || uuidv4();
    try {
        const initialData = { id: jobId, user_id: userId || 'anonymous', prompt, status: 'queued', progress: 0, stage: 'script', created_at: new Date().toISOString() };
        await supabase.from(JOBS_TABLE).upsert(initialData);
        if (redis) await redis.set(`job:${jobId}`, JSON.stringify(initialData), 'EX', 3600);
        await videoQueue.add('generate-video', { jobId, prompt, userId: userId || 'anonymous', type: 'video' });
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/status/:jobId', async (req, res) => {
    const { jobId } = req.params;
    try {
        let job;
        if (redis) {
            const data = await redis.get(`job:${jobId}`);
            job = data ? JSON.parse(data) : null;
        }
        if (!job && supabase) {
            const { data } = await supabase.from(JOBS_TABLE).select('*').eq('id', jobId).maybeSingle();
            if (data) job = { jobId: data.id, status: data.status, stage: data.stage, progress: data.progress, videoUrl: data.video_url, error: data.error };
        }
        if (!job) return res.status(404).json({ error: 'Job not found' });
        res.json(job);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

const PORT = process.env.PORT || 8081;
app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 IBCN AI ENGINE ACTIVE ON PORT ${PORT}`);
});
