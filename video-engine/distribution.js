require('dotenv').config();
const admin = require('firebase-admin');
const axios = require('axios');

if (admin.apps.length === 0 && process.env.FIREBASE_SERVICE_ACCOUNT) {
    try {
        const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            storageBucket: `${serviceAccount.project_id}.appspot.com`
        });
    } catch (e) {
        console.error("Firebase init failed in distribution:", e.message);
    }
}

const db = admin.firestore();

/**
 * DISTRIBUTION ENGINE
 * Listens for scheduled posts and triggers the actual social media upload.
 */
async function runDistribution() {
    console.log("Distribution engine starting...");

    // Watch for pending posts
    db.collection('scheduled_posts')
        .where('status', '==', 'PENDING')
        .where('scheduledAt', '<=', admin.firestore.Timestamp.now())
        .onSnapshot(async (snapshot) => {
            for (const doc of snapshot.docs) {
                const post = doc.data();
                await processDistribution(doc.id, post);
            }
        });
}

async function processDistribution(id, post) {
    try {
        console.log(`Distributing post ${id} to ${post.platform}...`);
        
        // 1. Get OAuth Token for the user (stored securely in Firestore)
        const userTokenDoc = await db.collection('user_tokens').doc(post.userId).get();
        if (!userTokenDoc.exists) throw new Error("OAuth tokens not found for user");
        const tokens = userTokenDoc.data();
        
        // 2. Perform Platform-Specific Upload
        let result;
        switch (post.platform) {
            case 'TIKTOK':
                result = await uploadToTikTok(post.videoId, post.caption, tokens.tiktok);
                break;
            case 'YOUTUBE_SHORTS':
                result = await uploadToYouTube(post.videoId, post.caption, tokens.youtube);
                break;
            case 'INSTAGRAM_REELS':
                result = await uploadToInstagram(post.videoId, post.caption, tokens.instagram);
                break;
            default:
                throw new Error(`Platform ${post.platform} not supported`);
        }

        // 3. Update Status
        await db.collection('scheduled_posts').doc(id).update({
            status: 'POSTED',
            postedAt: admin.firestore.Timestamp.now(),
            platformResponse: result
        });
        
        console.log(`Successfully posted ${id} to ${post.platform}`);
    } catch (error) {
        console.error(`Distribution failed for ${id}:`, error.message);
        await db.collection('scheduled_posts').doc(id).update({
            status: 'FAILED',
            error: error.message
        });
    }
}

// MOCKED PLATFORM UPLOADS (In Production: Call actual Platform APIs)

async function uploadToTikTok(videoId, caption, token) {
    console.log("Simulating TikTok Upload...");
    return { status: "success", platformId: "tiktok_" + Date.now() };
}

async function uploadToYouTube(videoId, caption, token) {
    console.log("Simulating YouTube Shorts Upload...");
    return { status: "success", platformId: "yt_" + Date.now() };
}

async function uploadToInstagram(videoId, caption, token) {
    console.log("Simulating Instagram Reels Upload...");
    return { status: "success", platformId: "ig_" + Date.now() };
}

runDistribution();
