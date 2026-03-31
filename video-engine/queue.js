const { Queue } = require('bullmq');
const Redis = require('ioredis');

// Simple, robust connection configuration
const redisOptions = {
    maxRetriesPerRequest: null,
    connectTimeout: 15000,
    reconnectOnError: (err) => {
        const targetError = "READONLY";
        if (err.message.includes(targetError)) return true;
        return false;
    }
};

let connection;
if (process.env.REDIS_URL) {
    try {
        connection = new Redis(process.env.REDIS_URL, redisOptions);
        console.log("✅ Queue: Redis connection string used");
    } catch (e) {
        console.error("❌ Queue: Redis connection failed:", e.message);
    }
} else {
    console.warn("⚠️ Queue: REDIS_URL not found, using localhost");
    connection = new Redis(redisOptions);
}

const videoQueue = new Queue('video-generation', { connection });

module.exports = { videoQueue, connection };
