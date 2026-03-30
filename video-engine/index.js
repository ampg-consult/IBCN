require('dotenv').config();
const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const Redis = require('ioredis');
const dns = require('node:dns');
const { videoQueue, connection: queueConnection } = require('./queue');

// Start the worker process
require('./worker');

// FIX: Force IPv4 first to prevent 'fetch failed' errors in container environments
dns.setDefaultResultOrder('ipv4first');

const app = express();

// Production CORS
app.use(cors({
    origin: "*", 
    methods: ["GET", "POST"]
}));

app.use(express.json());

// Initialize Cloud Services
let supabase;
try {
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_KEY;
    if (!supabaseUrl || !supabaseKey) {
        console.error("❌ SUPABASE_URL or SUPABASE_KEY is missing. Database sync will fail.");
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
    }
} catch (e) {
    console.error("❌ Failed to initialize Redis (API):", e.message);
}

const JOBS_TABLE = 'Video_jobs';

// Health check
app.get('/health', (req, res) => res.status(200).json({ 
    status: "ok", 
    service: "IBCN AI Engine",
    services: {
        redis: redis ? "connected" : "missing",
        supabase: supabase ? "connected" : "missing",
        openai: process.env.OPENAI_API_KEY ? "key present" : "missing"
    } 
}));

// Helper to get job from Redis
async function getJobFromRedis(jobId) {
    try {
        if (!redis) return null;
        const data = await redis.get(`job:${jobId}`);
        return data ? JSON.parse(data) : null;
    } catch (e) {
        console.error("Redis Get Error:", e);
        return null;
    }
}

// PRODUCTION ENDPOINT: /generate-video
app.post('/generate-video', async (req, res) => {
    const { prompt, userId, jobId: requestedJobId } = req.body;
    
    if (!prompt) return res.status(400).json({ error: 'Prompt is required' });
    if (!supabase) return res.status(503).json({ error: 'Database service unavailable' });
    
    const jobId = requestedJobId || uuidv4();
    const uid = userId || 'anonymous';
    
    try {
        // Initial state in Supabase
        const initialData = {
            id: jobId,
            user_id: uid,
            prompt,
            status: 'queued',
            progress: 0,
            stage: 'script',
            created_at: new Date().toISOString()
        };

        const { error } = await supabase.from(JOBS_TABLE).upsert(initialData);
        if (error) throw error;

        // Store in Redis for fast polling (expire in 1 hour)
        if (redis) await redis.set(`job:${jobId}`, JSON.stringify(initialData), 'EX', 3600);

        // Add to BullMQ Queue
        await videoQueue.add('generate-video', { jobId, prompt, userId: uid, type: 'video' });

        console.log(`[QUEUE] Job ${jobId} added to video-generation queue`);
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) {
        console.error(`[${jobId}] Error:`, error.message);
        res.status(500).json({ error: error.message });
    }
});

// PRODUCTION STATUS API
app.get('/status/:jobId', async (req, res) => {
    const { jobId } = req.params;
    try {
        // Try Redis first for real-time speed
        let job = await getJobFromRedis(jobId);
        
        if (!job && supabase) {
            // Fallback to Supabase
            const { data, error } = await supabase.from(JOBS_TABLE).select('*').eq('id', jobId).maybeSingle();
            if (error || !data) return res.status(404).json({ error: 'Job not found' });
            job = {
                jobId: data.id,
                status: data.status,
                stage: data.stage,
                progress: data.progress,
                videoUrl: data.video_url,
                error: data.error
            };
        }
        
        if (!job) return res.status(404).json({ error: 'Job not found' });
        res.json(job);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// PORT EXPOSURE FOR RAILWAY
const PORT = process.env.PORT || 8081;
app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 IBCN AI ENGINE ACTIVE ON PORT ${PORT}`);
});
