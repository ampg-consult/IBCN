package com.ampgconsult.ibcn.modules.content_studio.models

import com.google.firebase.Timestamp

data class GeneratedContent(
    val id: String = "",
    val type: ContentType = ContentType.ARTICLE,
    val prompt: String = "",
    val output: String = "",
    val mediaUrl: String? = null,
    val authorUid: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

enum class ContentType {
    ARTICLE, SOCIAL_POST, IMAGE, VIDEO_WORKFLOW
}

data class GenerationTask(
    val taskId: String = "",
    val type: ContentType = ContentType.ARTICLE,
    val status: GenerationStatus = GenerationStatus.PENDING,
    val progress: Float = 0f,
    val error: String? = null
)

enum class GenerationStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}
