require('dotenv').config();
const express = require('express');
const cors = require('cors');
const dns = require('node:dns');

// 1. IMMEDIATE STARTUP
const app = express();
const PORT = process.env.PORT || 8081;

// Basic middleware
app.use(cors({ origin: "*", methods: ["GET", "POST"] }));
app.use(express.json());

// Healthcheck (Must be reachable ASAP)
app.get('/health', (req, res) => {
    res.status(200).json({ 
        status: "ok", 
        timestamp: new Date().toISOString(),
        env: {
            node: process.version,
            port: PORT
        }
    });
});

// Start listening immediately
const server = app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 API SERVER LIVE ON PORT ${PORT}`);
    console.log(`🔗 Healthcheck available at: http://0.0.0.0:${PORT}/health`);
    
    // 2. LAZY LOAD HEAVY SERVICES
    initializeBackgroundServices();
});

// 3. BACKGROUND INITIALIZATION
async function initializeBackgroundServices() {
    console.log("📦 Initializing background services...");
    
    try {
        const { createClient } = require('@supabase/supabase-js');
        const Redis = require('ioredis');
        const { v4: uuidv4 } = require('uuid');

        // Fix for container networking
        try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

        // Load Queue & Worker
        const { videoQueue } = require('./queue');
        require('./worker');

        const supabase = (process.env.SUPABASE_URL && process.env.SUPABASE_KEY) 
            ? createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY) 
            : null;

        const redis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL) : null;

        console.log("✅ Services Ready (Supabase:", !!supabase, "Redis:", !!redis, ")");

        // Register Endpoints
        setupEndpoints(app, supabase, redis, videoQueue, uuidv4);
        
    } catch (error) {
        console.error("💥 Background Init Error:", error);
    }
}

function setupEndpoints(app, supabase, redis, videoQueue, uuidv4) {
    const JOBS_TABLE = 'Video_jobs';

    app.post('/generate-video', async (req, res) => {
        const { prompt, userId, jobId: requestedJobId } = req.body;
        if (!prompt) return res.status(400).json({ error: 'Prompt is required' });
        if (!supabase) return res.status(503).json({ error: 'Database unavailable' });

        const jobId = requestedJobId || uuidv4();
        try {
            const data = { id: jobId, user_id: userId || 'anonymous', prompt, status: 'queued', progress: 0 };
            await supabase.from(JOBS_TABLE).upsert(data);
            if (redis) await redis.set(`job:${jobId}`, JSON.stringify(data), 'EX', 3600);
            await videoQueue.add('generate-video', { jobId, prompt, userId: userId || 'anonymous', type: 'video' });
            res.status(202).json({ jobId, status: 'queued' });
        } catch (e) { res.status(500).json({ error: e.message }); }
    });

    app.get('/status/:jobId', async (req, res) => {
        const { jobId } = req.params;
        try {
            let job = redis ? JSON.parse(await redis.get(`job:${jobId}`) || 'null') : null;
            if (!job && supabase) {
                const { data } = await supabase.from(JOBS_TABLE).select('*').eq('id', jobId).maybeSingle();
                if (data) job = data;
            }
            if (!job) return res.status(404).json({ error: 'Not found' });
            res.json(job);
        } catch (e) { res.status(500).json({ error: e.message }); }
    });
}

// Global error handling
process.on('uncaughtException', (err) => console.error('💥 Uncaught:', err));
process.on('unhandledRejection', (err) => console.error('💥 Unhandled:', err));
