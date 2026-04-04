require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const { Server } = require("socket.io");
const dns = require('node:dns');
const { v4: uuidv4 } = require('uuid');

// DEVOPS FIX: Force IPv4 for stable cloud connections
try { dns.setDefaultResultOrder('ipv4first'); } catch (e) {}

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 8081;

const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Global error handlers
process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT:', err));
process.on('unhandledRejection', (reason) => console.error('💥 REJECTION:', reason));

app.use(cors());
app.use(express.json());

// 🧩 WEBSOCKET CONNECTION (Requirement)
io.on("connection", (socket) => {
    console.log("📡 Client connected:", socket.id);

    socket.on("joinJob", (jobId) => {
        console.log(`🔗 Socket ${socket.id} joining job: ${jobId}`);
        socket.join(jobId);
    });

    socket.on("disconnect", () => {
        console.log("🔌 Client disconnected:", socket.id);
    });
});

// 📡 Real-time Job Emitter (Requirement)
global.pushUpdate = function(jobId, data) {
    // Standardize Status: Replace READY with completed (Requirement)
    if (data.status === 'READY' || data.status === 'done') {
        data.status = 'completed';
    }
    
    console.log(`📤 Emitting jobUpdate to ${jobId}: ${data.stage} (${data.progress}%)`);
    io.to(jobId).emit("jobUpdate", data);
};

app.get("/", (req, res) => res.json({ status: "IBCN Video Engine v3.0", websockets: "enabled" }));
app.get("/health", (req, res) => res.status(200).json({ status: "ok" }));

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

// Keep status endpoint as fallback
app.get('/status/:jobId', async (req, res) => {
    const { jobId } = req.params;
    try {
        const { createClient } = require('@supabase/supabase-js');
        const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);
        const { data } = await supabase.from('Video_jobs').select('*').eq('id', jobId).maybeSingle();
        if (!data) return res.status(404).json({ error: 'Job not found' });
        res.json({
            jobId: jobId,
            status: (data.status === 'READY' || data.status === 'done') ? 'completed' : data.status,
            progress: data.progress,
            stage: data.stage,
            videoUrl: data.video_url,
            error: data.error
        });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

server.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 Server listening on ${PORT} (WebSocket Ready)`);
    require('./worker');
});
