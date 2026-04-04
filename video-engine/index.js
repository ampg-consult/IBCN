require('dotenv').config();
const express = require('express');
const cors = require('cors');
const dns = require('node:dns');
const { v4: uuidv4 } = require('uuid');

try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

const app = express();
const PORT = process.env.PORT || 8081;

process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT EXCEPTION:', err));
process.on('unhandledRejection', (reason) => console.error('💥 UNHANDLED REJECTION:', reason));

app.use(cors());
app.use(express.json());

const clients = {};

app.get("/", (req, res) => { 
    res.json({ status: "IBCN Video Engine v2.0 (SSE Enabled)", endpoints: ["/generate-video", "/status/:jobId", "/stream/:jobId"] }); 
});

// 🧩 SSE Stream Endpoint
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

global.pushUpdate = function(jobId, data) {
    const client = clients[jobId];
    if (client) {
        client.write(`data: ${JSON.stringify(data)}\n\n`);
    }
};

app.post('/generate-video', async (req, res) => {
    const { videoQueue } = require('./queue');
    const jobId = req.body.jobId || uuidv4();
    try {
        const { createClient } = require('@supabase/supabase-js');
        const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
        await supabase.from('Video_jobs').upsert({ id: jobId, user_id: req.body.userId || 'anon', prompt: req.body.prompt, status: 'queued', progress: 0 });
        await videoQueue.add('generate-video', { jobId, prompt: req.body.prompt, userId: req.body.userId });
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server listening on ${PORT}`);
    require('./worker');
});
