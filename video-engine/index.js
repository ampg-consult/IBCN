require('dotenv').config();
const express = require('express');
const cors = require('cors');
const dns = require('node:dns');
const { v4: uuidv4 } = require('uuid');

// 📡 DEVOPS: Force IPv4 for stable cloud connections
try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

const app = express();
const PORT = process.env.PORT || 8081;

// Global Error Handling
process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT:', err));
process.on('unhandledRejection', (reason) => console.error('💥 REJECTION:', reason));

app.use(cors());
app.use(express.json());

// 🧩 SSE Clients Storage
const clients = {};

// 1. Root & Health (CRITICAL for Railway)
app.get("/", (req, res) => res.json({ status: "IBCN Video Engine v2.0", sse: "enabled" }));
app.get("/health", (req, res) => res.status(200).json({ status: "ok", uptime: process.uptime() }));

// 2. 🧩 SSE Stream Endpoint
app.get("/stream/:jobId", (req, res) => {
    const { jobId } = req.params;
    res.setHeader("Content-Type", "text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");
    res.setHeader("Access-Control-Allow-Origin", "*");

    clients[jobId] = res;
    console.log(`📡 SSE Connected: ${jobId}`);

    const heartbeat = setInterval(() => res.write(': keep-alive\n\n'), 30000);

    req.on("close", () => {
        console.log(`🔌 SSE Disconnected: ${jobId}`);
        clearInterval(heartbeat);
        delete clients[jobId];
    });
});

// 3. 📡 Push Update Function (Global for Worker)
global.pushUpdate = function(jobId, data) {
    const client = clients[jobId];
    if (client) {
        client.write(`data: ${JSON.stringify(data)}\n\n`);
    }
};

// 4. Job Creation
app.post('/generate-video', async (req, res) => {
    const { videoQueue } = require('./queue');
    const jobId = req.body.jobId || uuidv4();
    try {
        const { createClient } = require('@supabase/supabase-js');
        const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
        
        const initialData = { 
            id: jobId, 
            user_id: req.body.userId || 'anon', 
            prompt: req.body.prompt, 
            status: 'processing', 
            progress: 10, 
            stage: 'script' 
        };
        
        await supabase.from('Video_jobs').upsert(initialData);
        await videoQueue.add('generate-video', { jobId, prompt: req.body.prompt, userId: req.body.userId });
        
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) { 
        res.status(500).json({ error: error.message }); 
    }
});

// 5. Polling Fallback / Status API
app.get('/status/:jobId', async (req, res) => {
    const { jobId } = req.params;
    try {
        const { createClient } = require('@supabase/supabase-js');
        const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
        const { data } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
        
        if (!data) return res.status(404).json({ error: 'Job not found' });
        
        res.json({
            jobId: jobId,
            status: data.status === 'READY' ? 'completed' : data.status,
            progress: data.progress,
            stage: data.stage,
            videoUrl: data.video_url,
            error: data.error
        });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server listening on ${PORT}`);
    require('./worker');
});
