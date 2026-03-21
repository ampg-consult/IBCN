const express = require('express');
const fs = require('fs-extra');
const { execSync } = require('child_process');
const path = require('path');

const app = express();
app.use(express.json({ limit: '100mb' }));

// Friendly Landing Page / Health Check
app.get('/', (req, res) => {
    res.status(200).send('<h1>IBCN Deploy Engine is Online</h1><p>Send a POST request to <code>/build</code> to trigger a deployment.</p>');
});

app.post('/build', async (req, res) => {
    const { projectId, files } = req.body;
    if (!projectId || !files) {
        return res.status(400).json({ error: 'Missing projectId or files' });
    }

    const buildDir = path.join(__dirname, 'temp', projectId);
    
    try {
        console.log(`Starting build for project: ${projectId}`);
        await fs.ensureDir(buildDir);
        
        // Write files received from Android App
        for (const [fileName, content] of Object.entries(files)) {
            await fs.outputFile(path.join(buildDir, fileName), content);
        }

        // Run Flutter Build
        console.log(`Running flutter build web...`);
        execSync('flutter build web --release', { 
            cwd: buildDir, 
            stdio: 'inherit'
        });

        console.log(`Build complete for ${projectId}`);

        res.status(200).json({
            success: true,
            buildId: Date.now().toString(),
            message: "Build successful",
            url: `https://ibcn.site/apps/${projectId}`
        });

    } catch (error) {
        console.error('Build failed:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => console.log(`Deploy Engine running on port ${PORT}`));
