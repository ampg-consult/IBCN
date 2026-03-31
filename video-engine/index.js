require('dotenv').config();
const express = require('express');
const cors = require('cors');

/**
 * SENIOR DEVOPS NOTE: 
 * We use an 'Instant-On' strategy to pass cloud health checks.
 * The HTTP server starts immediately, while heavy dependencies 
 * load in the background.
 */

const app = express();
const PORT = process.env.PORT || 8081;

// Global error handlers to prevent silent crashes
process.on('uncaughtException', (err) => console.error('💥 Uncaught Exception:', err));
process.on('unhandledRejection', (reason) => console.error('💥 Unhandled Rejection:', reason));

app.use(cors({ origin: "*" }));
app.use(express.json());

// 1. Mandatory Health Endpoint
app.get('/health', (req, res) => {
    res.status(200).json({ 
        status: "ok", 
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

// 2. Root Endpoint
app.get('/', (req, res) => {
    res.send("🚀 IBCN AI Video Engine Running");
});

// 3. Dynamic Port Binding (Dynamic 0.0.0.0 for Cloud)
const server = app.listen(PORT, "0.0.0.0", () => {
    console.log(`✅ Server started on port ${PORT}`);
    console.log(`📡 Health check: http://0.0.0.0:${PORT}/health`);
    
    // Defer heavy initialization to ensure health check passes first
    setImmediate(initializeServices);
});

async function initializeServices() {
    console.log("📦 Initializing background services...");
    try {
        const { createClient } = require('@supabase/supabase-js');
        const Redis = require('ioredis');
        const { v4: uuidv4 } = require('uuid');

        // Defensive ENV validation
        const supabase = (process.env.SUPABASE_URL && process.env.SUPABASE_KEY)
            ? createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY)
            : null;
        if (!supabase) console.warn("⚠️ Supabase credentials missing. Persistence disabled.");

        const redis = process.env.REDIS_URL ? new Redis(process.env.REDIS_URL) : null;
        if (!redis) console.warn("⚠️ REDIS_URL missing. Real-time polling disabled.");

        // Load Worker/Queue only after server is live
        const { videoQueue } = require('./queue');
        require('./worker');
        
        console.log("🚀 Background services initialized.");

        // Register feature endpoints
        registerFeatureRoutes(app, supabase, redis, videoQueue, uuidv4);

    } catch (error) {
        console.error("💥 Startup Service Error:", error.message);
    }
}

function registerFeatureRoutes(app, supabase, redis, videoQueue, uuidv4) {
    app.post('/generate-video', async (req, res) => {
        const { prompt, userId } = req.body;
        if (!prompt) return res.status(400).json({ error: "Prompt required" });
        
        const jobId = uuidv4();
        try {
            if (supabase) {
                await supabase.from('Video_jobs').upsert({ id: jobId, user_id: userId || 'anon', prompt, status: 'queued' });
            }
            await videoQueue.add('generate-video', { jobId, prompt, userId, type: 'video' });
            res.status(202).json({ jobId, status: 'queued' });
        } catch (e) {
            res.status(500).json({ error: e.message });
        }
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
        } catch (e) {
            res.status(500).json({ error: e.message });
        }
    });
}
