require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { createClient } = require('@supabase/supabase-js');
const Redis = require('ioredis');
const { v4: uuidv4 } = require('uuid');

const app = express();
const PORT = process.env.PORT || 8081;

// Global error handlers
process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT EXCEPTION:', err.stack || err));
process.on('unhandledRejection', (reason) => console.error('💥 UNHANDLED REJECTION:', reason));

app.use(cors({ origin: "*" }));
app.use(express.json());

// Service Instances
let supabase;
let redis;

// Initialize clients (Non-blocking)
try {
    if (process.env.SUPABASE_URL && process.env.SUPABASE_KEY) {
        supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
        console.log("✅ Supabase Initialized");
    }
    if (process.env.REDIS_URL) {
        redis = new Redis(process.env.REDIS_URL, { maxRetriesPerRequest: null });
        console.log("✅ Redis Initialized");
    }
} catch (e) {
    console.error("Client Init Error:", e.message);
}

// Routes
app.get('/health', (req, res) => res.status(200).json({ status: "ok", redis: !!redis, supabase: !!supabase }));
app.get('/', (req, res) => res.send("🚀 IBCN AI Video Engine Running"));

app.post('/generate-video', async (req, res) => {
    const { videoQueue } = require('./queue');
    const { prompt, userId, jobId: requestedJobId } = req.body;
    
    if (!prompt) return res.status(400).json({ error: 'Prompt is required' });
    
    const jobId = requestedJobId || uuidv4();
    const uid = userId || 'anonymous';
    
    try {
        const initialData = {
            id: jobId,
            user_id: uid,
            prompt,
            status: 'queued',
            progress: 0,
            stage: 'script',
            created_at: new Date().toISOString()
        };

        if (supabase) {
            await supabase.from('Video_jobs').upsert(initialData);
        }

        if (redis) {
            await redis.set(`job:${jobId}`, JSON.stringify({
                status: 'queued',
                progress: 0,
                stage: 'script',
                videoUrl: null
            }), 'EX', 3600);
        }

        await videoQueue.add('generate-video', { jobId, prompt, userId: uid, type: 'video' });
        console.log(`[QUEUE] Job ${jobId} added`);
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) {
        console.error(`[${jobId}] Error:`, error.message);
        res.status(500).json({ error: error.message });
    }
});

app.get('/status/:jobId', async (req, res) => {
    const { jobId } = req.params;
    try {
        let job;
        // 1. Try Redis for real-time speed
        if (redis) {
            const cached = await redis.get(`job:${jobId}`);
            if (cached) {
                job = JSON.parse(cached);
                console.log(`[STATUS] ${jobId} from Redis: ${job.status} (${job.progress}%)`);
            }
        }
        
        // 2. Fallback to Supabase
        if (!job && supabase) {
            const { data, error } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
            if (data) {
                job = {
                    status: data.status,
                    stage: data.stage,
                    progress: data.progress,
                    videoUrl: data.video_url || data.videoUrl, // Standardize
                    error: data.error
                };
                console.log(`[STATUS] ${jobId} from Supabase: ${job.status}`);
            }
        }
        
        if (!job) return res.status(404).json({ error: 'Job not found' });
        
        // Final normalization for Frontend
        const response = {
            status: job.status === 'READY' ? 'completed' : job.status, // Support both
            progress: job.progress || 0,
            stage: job.stage || 'working',
            videoUrl: job.videoUrl || null,
            error: job.error || null
        };
        
        res.json(response);
    } catch (error) {
        console.error(`[STATUS ${jobId}] Error:`, error.message);
        res.status(500).json({ error: error.message });
    }
});

app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 IBCN AI ENGINE ACTIVE ON PORT ${PORT}`);
    // Load Worker/Queue
    try {
        require('./worker');
        console.log("✅ Worker Started");
    } catch (e) {
        console.error("Worker Start Error:", e.message);
    }
});
