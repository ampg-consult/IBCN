require('dotenv').config();
const express = require('express');
const cors = require('cors');
const dns = require('node:dns');
const { v4: uuidv4 } = require('uuid');
const { createClient } = require('@supabase/supabase-js');

// 📡 DEVOPS: Force IPv4 for stable cloud connections
try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

const app = express();
const PORT = process.env.PORT || 8081;

// 🛡️ Pre-initialize Supabase with safe defaults
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY, {
    auth: { persistSession: false }
});

app.use(cors());
app.use(express.json());

const clients = {};

app.get("/", (req, res) => res.json({ status: "IBCN Video Engine v4.2 (Production)", sse: "enabled" }));
app.get("/health", (req, res) => res.status(200).send("OK"));

app.get("/stream/:jobId", (req, res) => {
    const { jobId } = req.params;
    res.setHeader("Content-Type", "text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");
    res.setHeader("Access-Control-Allow-Origin", "*");

    clients[jobId] = res;
    console.log(`📡 SSE: Client connected [${jobId}]`);

    const heartbeat = setInterval(() => {
        if (!res.writableEnded) res.write(': heartbeat\n\n');
    }, 25000);

    req.on("close", () => {
        console.log(`🔌 SSE: Client disconnected [${jobId}]`);
        clearInterval(heartbeat);
        delete clients[jobId];
    });
});

global.pushUpdate = function(jobId, data) {
    const client = clients[jobId];
    if (client && !client.writableEnded) {
        if (data.status === 'READY' || data.status === 'done') data.status = 'completed';
        client.write(`data: ${JSON.stringify(data)}\n\n`);
    }
};

app.post('/generate-video', async (req, res) => {
    const jobId = req.body.jobId || uuidv4();
    const { prompt, userId = 'anon' } = req.body;

    if (!prompt) return res.status(400).json({ error: "Prompt required" });

    try {
        console.log(`🎬 Requesting Generation [${jobId}]: ${prompt.substring(0, 30)}...`);
        
        // 1. Quick initial save to DB (Timeout protected)
        const dbPromise = supabase.from('Video_jobs').upsert({ 
            id: jobId, user_id: userId, prompt, status: 'processing', progress: 10, stage: 'script' 
        });
        
        const timeoutPromise = new Promise((_, reject) => 
            setTimeout(() => reject(new Error('DB Timeout')), 5000)
        );

        await Promise.race([dbPromise, timeoutPromise]).catch(err => {
            console.warn("⚠️ Initial DB upsert was slow or failed, continuing to queue anyway...");
        });

        // 2. Add to Queue
        const { videoQueue } = require('./queue');
        await videoQueue.add('generate-video', { jobId, prompt, userId });
        
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) {
        console.error("💥 POST /generate-video Error:", error.message);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.get('/status/:jobId', async (req, res) => {
    try {
        const { data } = await supabase.from('Video_jobs').select('*').eq('id', req.params.jobId).maybeSingle();
        if (!data) return res.status(404).json({ error: 'Not found' });
        res.json({
            jobId: data.id,
            status: (data.status === 'READY' || data.status === 'done') ? 'completed' : data.status,
            progress: data.progress,
            stage: data.stage,
            videoUrl: data.video_url,
            error: data.error
        });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

const server = app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 API LIVE ON PORT ${PORT}`);
    require('./worker');
});

// 🛡️ Global protection against 502 causing crashes
process.on('uncaughtException', (err) => console.error('💥 SYSTEM UNCAUGHT:', err));
process.on('unhandledRejection', (reason) => console.error('💥 SYSTEM REJECTION:', reason));
