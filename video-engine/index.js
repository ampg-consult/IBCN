require('dotenv').config();
const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const Redis = require('ioredis');
const dns = require('node:dns');
const { videoQueue } = require('./queue');

// Start the worker process in the same instance
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
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
const redis = new Redis(process.env.REDIS_URL, { 
    maxRetriesPerRequest: null,
    enableReadyCheck: false
});

const JOBS_TABLE = 'Video_jobs';

// Health check
app.get('/health', (req, res) => res.status(200).json({ status: "ok", service: "IBCN AI Engine" }));

// Helper to get job from Redis
async function getJobFromRedis(jobId) {
    try {
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
        await redis.set(`job:${jobId}`, JSON.stringify(initialData), 'EX', 3600);

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
        
        if (!job) {
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
