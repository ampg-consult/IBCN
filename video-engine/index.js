require('dotenv').config();
const express = require('express');
const cors = require('cors');
const dns = require('node:dns');

// DEVOPS FIX: Force IPv4 to prevent cloud connection errors
try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

const app = express();
const PORT = process.env.PORT || 8081;

process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT EXCEPTION:', err));
process.on('unhandledRejection', (reason) => console.error('💥 UNHANDLED REJECTION:', reason));

app.use(cors({ origin: "*" }));
app.use(express.json());

let supabase;
let redis;

try {
    const { createClient } = require('@supabase/supabase-js');
    const Redis = require('ioredis');
    
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

// 1. Root Route
app.get("/", (req, res) => { 
    res.json({ 
        status: "IBCN Video Engine Live", 
        version: "1.0", 
        endpoints: [ "/generate-video", "/status/:jobId", "/video/:jobId" ] 
    }); 
});

app.get('/health', (req, res) => res.status(200).json({ status: "ok", redis: !!redis, supabase: !!supabase }));

app.post('/generate-video', async (req, res) => {
    const { videoQueue } = require('./queue');
    const { prompt, userId, jobId: requestedJobId } = req.body;
    
    if (!prompt) return res.status(400).json({ error: 'Prompt is required' });
    
    const jobId = requestedJobId || require('uuid').v4();
    const uid = userId || 'anonymous';
    
    try {
        const initialData = {
            id: jobId,
            user_id: uid,
            prompt,
            status: 'processing',
            progress: 10,
            stage: 'script',
            created_at: new Date().toISOString()
        };

        if (supabase) await supabase.from('Video_jobs').upsert(initialData);
        if (redis) {
            await redis.set(`job:${jobId}`, JSON.stringify({
                status: 'processing',
                progress: 10,
                stage: 'script',
                videoUrl: null
            }), 'EX', 3600);
        }

        await videoQueue.add('generate-video', { jobId, prompt, userId: uid, type: 'video' });
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
            const cached = await redis.get(`job:${jobId}`);
            if (cached) job = JSON.parse(cached);
        }
        
        if (!job && supabase) {
            const { data } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
            if (data) {
                job = {
                    status: data.status,
                    stage: data.stage,
                    progress: data.progress,
                    videoUrl: data.video_url || data.videoUrl,
                    error: data.error
                };
            }
        }
        
        if (!job) return res.status(404).json({ error: 'Job not found' });
        
        // Ensure standardized response
        const response = {
            status: (job.status === 'READY' || job.status === 'done') ? 'completed' : job.status,
            progress: job.progress || 0,
            stage: job.stage || 'working',
            videoUrl: job.videoUrl || null,
            error: job.error || null
        };
        
        res.json(response);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server running on ${PORT}`);
    require('./worker');
});
