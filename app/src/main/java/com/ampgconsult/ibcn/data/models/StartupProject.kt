package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class StartupProject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val aiScore: Int = 0,
    val marketPotential: String = "Medium", // Low, Medium, High, Very High
    val creatorId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    // FIX 5 & 7: Pipeline Status tracking
    val status: String = "IDLE", // IDLE, DEPLOYING, DEPLOYED, GENERATING_VIDEO, VIDEO_READY, LISTED, FAILED
    val liveUrl: String? = null,
    val videoUrl: String? = null,
    val marketplaceId: String? = null
)

data class InvestorInsight(
    val title: String = "",
    val content: String = ""
)
