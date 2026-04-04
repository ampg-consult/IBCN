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

// Global job store for real-time status (Requirement)
global.jobs = {};

// Initialize Supabase
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_KEY);

app.use(cors());
app.use(express.json());

// Helper to update job state (Requirement)
global.updateJob = function(jobId, updates) {
    if (!global.jobs[jobId]) {
        global.jobs[jobId] = {
            status: "queued",
            stage: "starting",
            progress: 0,
            videoUrl: null,
            error: null
        };
    }
    global.jobs[jobId] = { ...global.jobs[jobId], ...updates };
    
    // Standardize: Replace any "READY" with "completed" (Requirement)
    if (global.jobs[jobId].status === 'READY') {
        global.jobs[jobId].status = 'completed';
    }

    console.log("JOB UPDATE:", jobId, global.jobs[jobId]);
    
    // Also push to SSE clients if connected
    if (global.pushUpdate) {
        global.pushUpdate(jobId, global.jobs[jobId]);
    }
};

app.get("/", (req, res) => res.send("🚀 IBCN AI Video Engine Running"));
app.get("/health", (req, res) => res.status(200).send("OK"));

// 📡 STATUS API (Requirement)
app.get("/status/:jobId", (req, res) => {
    const job = global.jobs[req.params.jobId];
    if (!job) {
        return res.status(404).json({ error: "Job not found" });
    }
    res.json(job);
});

app.post('/generate-video', async (req, res) => {
    const { videoQueue } = require('./queue');
    const jobId = req.body.jobId || uuidv4();
    const prompt = req.body.prompt;
    const userId = req.body.userId || 'anon';

    if (!prompt) return res.status(400).json({ error: "Prompt required" });

    try {
        // Initialize job in memory
        global.updateJob(jobId, { status: 'processing', stage: 'script', progress: 10 });

        // Save to Supabase for persistence
        await supabase.from('Video_jobs').upsert({ 
            id: jobId, user_id: userId, prompt, status: 'processing', progress: 10, stage: 'script' 
        });

        await videoQueue.add('generate-video', { jobId, prompt, userId });
        res.status(202).json({ jobId, status: 'queued' });
    } catch (error) {
        console.error("POST /generate-video Error:", error.message);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.listen(PORT, "0.0.0.0", () => {
    console.log(`🚀 API LIVE ON PORT ${PORT}`);
    require('./worker');
});

process.on('uncaughtException', (err) => console.error('💥 UNCAUGHT:', err));
process.on('unhandledRejection', (reason) => console.error('💥 REJECTION:', reason));
