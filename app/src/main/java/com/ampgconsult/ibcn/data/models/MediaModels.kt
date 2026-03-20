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
    val createdAt: Timestamp = Timestamp.now()
)

data class VideoScript(
    val hook: String = "",
    val highlights: List<String> = emptyList(),
    val cta: String = ""
)

enum class MediaStatus {
    PENDING, GENERATING, READY, FAILED
}

data class ShareMetrics(
    val shares: Int = 0,
    val views: Int = 0,
    val conversions: Int = 0
)
