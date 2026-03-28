const { Queue } = require('bullmq');
const Redis = require('ioredis');

const connection = new Redis(process.env.REDIS_URL, {
    maxRetriesPerRequest: null
});

const videoQueue = new Queue('video-generation', {
    connection
});

module.exports = { videoQueue, connection };
