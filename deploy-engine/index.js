const express = require('express');
const fs = require('fs-extra');
const { execSync } = require('child_process');
const path = require('path');
const admin = require('firebase-admin');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json({ limit: '100mb' }));

// --- JOB STORAGE & SSE CLIENTS ---
const jobs = {};
const clients = {};

// Initialize Firebase
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    try {
        const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            storageBucket: `${serviceAccount.project_id}.appspot.com`
        });
        console.log("Firebase Admin initialized successfully");
    } catch (e) {
        console.error("Failed to parse FIREBASE_SERVICE_ACCOUNT:", e.message);
    }
}

// --- SSE ENDPOINT ---
app.get('/stream/:jobId', (req, res) => {
    const { jobId } = req.params;

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders();

    clients[jobId] = res;

    // Send initial state if job exists
    if (jobs[jobId]) {
        res.write(`data: ${JSON.stringify(jobs[jobId])}\n\n`);
    }

    req.on('close', () => {
        delete clients[jobId];
    });
});

function pushUpdate(jobId, data) {
    const client = clients[jobId];
    if (client) {
        client.write(`data: ${JSON.stringify(data)}\n\n`);
    }
}

// --- GENERATION ENDPOINTS ---
app.post('/generate-video', async (req, res) => {
    const { jobId = uuidv4(), userId, prompt } = req.body;
    
    jobs[jobId] = {
        id: jobId,
        status: 'processing',
        stage: 'script',
        progress: 10,
        userId: userId
    };

    res.status(202).json({ jobId });

    // Start background simulation
    simulateVideoGeneration(jobId);
});

async function simulateVideoGeneration(jobId) {
    const stages = [
        { stage: 'script', progress: 20, delay: 2000 },
        { stage: 'image', progress: 40, delay: 3000 },
        { stage: 'audio', progress: 60, delay: 2000 },
        { stage: 'rendering', progress: 85, delay: 4000 },
        { stage: 'uploading', progress: 95, delay: 2000 }
    ];

    for (const step of stages) {
        await new Promise(resolve => setTimeout(resolve, step.delay));
        if (!jobs[jobId]) return;
        
        jobs[jobId].stage = step.stage;
        jobs[jobId].progress = step.progress;
        pushUpdate(jobId, jobs[jobId]);
    }

    // Final result
    jobs[jobId].status = 'completed';
    jobs[jobId].stage = 'done';
    jobs[jobId].progress = 100;
    jobs[jobId].videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"; // Sample for testing
    
    pushUpdate(jobId, jobs[jobId]);
}

app.get('/status/:jobId', (req, res) => {
    const job = jobs[req.params.jobId];
    if (!job) return res.status(404).json({ error: 'Job not found' });
    res.json(job);
});

app.get('/health', (req, res) => {
    res.status(200).json(getHealthData());
});

app.get('/', (req, res) => {
    res.status(200).send('<h1>IBCN Real-time Engine</h1><p>SSE & WebSocket Active.</p>');
});

app.post('/build', async (req, res) => {
    const { projectId, files } = req.body;
    if (!projectId || !files) {
        return res.status(400).json({ error: 'Missing projectId or files' });
    }

    const buildDir = path.join(__dirname, 'temp', projectId);
    
    try {
        await fs.ensureDir(buildDir);
        for (const [fileName, content] of Object.entries(files)) {
            await fs.outputFile(path.join(buildDir, fileName), content);
        }
        execSync('flutter build web --release', { cwd: buildDir, stdio: 'inherit' });
        // ... rest of build logic
        res.status(200).json({ success: true, url: `https://ibcn.site/apps/${projectId}` });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    } finally {
        try { await fs.remove(buildDir); } catch (e) {}
    }
});

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

function getHealthData() {
    return {
        status: "ok",
        uptime: Math.floor(process.uptime()),
        aiHealth: "active",
        timestamp: Date.now()
    };
}

wss.on('connection', (ws) => {
    ws.send(JSON.stringify(getHealthData()));
    const interval = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(getHealthData()));
    }, 5000);
    ws.on('close', () => clearInterval(interval));
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => console.log(`IBCN Real-time Engine running on port ${PORT}`));
