const { Queue } = require('bullmq');
const Redis = require('ioredis');

let connection;
try {
    if (!process.env.REDIS_URL) {
        console.warn("⚠️ WARNING: REDIS_URL is missing. Queue will not function.");
        // Fallback to local for dev, but this will fail in production
        connection = new Redis({ host: '127.0.0.1', port: 6379, maxRetriesPerRequest: null });
    } else {
        connection = new Redis(process.env.REDIS_URL, {
            maxRetriesPerRequest: null,
            connectTimeout: 10000
        });
        console.log("✅ Redis Connection Initialized");
    }
} catch (e) {
    console.error("❌ Redis Initialization Error:", e.message);
}

const videoQueue = new Queue('video-generation', {
    connection
});

module.exports = { videoQueue, connection };
