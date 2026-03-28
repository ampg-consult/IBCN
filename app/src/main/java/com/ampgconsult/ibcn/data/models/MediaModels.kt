package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class ViralVideoMetadata(
    val id: String = "",
    val assetId: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val script: VideoScript = VideoScript(),
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val status: MediaStatus = MediaStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val optimization: ViralOptimization? = null
)

data class VideoScript(
    val hook: String = "",
    val highlights: List<String> = emptyList(),
    val cta: String = ""
)

enum class MediaStatus {
    PENDING, GENERATING, READY, COMPLETED, FAILED, OPTIMIZING
}

data class ViralOptimization(
    val optimizedScript: String = "",
    val hook: String = "",
    val captions: String = "",
    val hashtags: List<String> = emptyList(),
    val watermarkText: String = "Build yours on ibcn.site"
)

data class ScheduledPost(
    val id: String = "",
    val videoId: String = "",
    val userId: String = "",
    val platform: SocialPlatform = SocialPlatform.TIKTOK,
    val scheduledAt: Timestamp = Timestamp.now(),
    val status: PostStatus = PostStatus.PENDING,
    val caption: String = "",
    val metadata: Map<String, String> = emptyMap()
)

enum class SocialPlatform {
    TIKTOK, YOUTUBE_SHORTS, INSTAGRAM_REELS
}

enum class PostStatus {
    PENDING, POSTED, FAILED, CANCELLED
}

data class VideoAnalytics(
    val videoId: String = "",
    val views: Int = 0,
    val clicks: Int = 0,
    val salesGenerated: Double = 0.0,
    val platforms: Map<String, Int> = emptyMap()
)

data class ShareMetrics(
    val shares: Int = 0,
    val views: Int = 0,
    val conversions: Int = 0
)

data class JobStatusUpdate(
    val status: String = "",
    val stage: String = "",
    val progress: Int = 0,
    val videoUrl: String? = null,
    val result: Map<String, Any>? = null,
    val error: String? = null
)
