const express = require('express');
const fs = require('fs-extra');
const { execSync } = require('child_process');
const path = require('path');
const admin = require('firebase-admin');

const app = express();
app.use(express.json({ limit: '100mb' }));

// Initialize Firebase (Requires FIREBASE_SERVICE_ACCOUNT variable in Railway)
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

app.get('/', (req, res) => {
    res.status(200).send('<h1>IBCN Deploy Engine is Online</h1><p>Ready to process builds.</p>');
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
        
        for (const [fileName, content] of Object.entries(files)) {
            await fs.outputFile(path.join(buildDir, fileName), content);
        }

        console.log(`Running flutter build web...`);
        execSync('flutter build web --release', { cwd: buildDir, stdio: 'inherit' });

        const webBuildDir = path.join(buildDir, 'build', 'web');
        
        // --- Firebase Upload Logic ---
        if (admin.apps.length > 0) {
            console.log(`Uploading build to Firebase Hosting path: apps/${projectId}...`);
            const bucket = admin.storage().bucket();
            const buildFiles = await fs.readdir(webBuildDir, { recursive: true });

            for (const file of buildFiles) {
                const fullPath = path.join(webBuildDir, file);
                if ((await fs.stat(fullPath)).isFile()) {
                    await bucket.upload(fullPath, {
                        destination: `deployed_apps/${projectId}/${file}`,
                        public: true,
                        metadata: { cacheControl: 'public, max-age=3600' }
                    });
                }
            }
            console.log("Upload complete.");
        } else {
            console.warn("Skipping upload: Firebase Admin not initialized (Missing variable)");
        }

        res.status(200).json({
            success: true,
            buildId: Date.now().toString(),
            message: "Build successful and uploaded",
            url: `https://ibcn.site/apps/${projectId}`
        });

    } catch (error) {
        console.error('Build failed:', error);
        res.status(500).json({ success: false, error: error.message });
    } finally {
        // Cleanup to save space on Railway
        try { await fs.remove(buildDir); } catch (e) {}
    }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => console.log(`Deploy Engine running on port ${PORT}`));
