package com.ampgconsult.ibcn.data.models

data class UserPublic(
    val uid: String = "",
    val username: String = "", // Styled: @DarLingTon_AI
    val username_lower: String = "", // Normalized: darlington_ai
    val displayName: String = "",
    val avatarUrl: String = "",
    val isVerified: Boolean = false,
    val reputationScore: Int = 0,
    val followersCount: Int = 0,
    val projectsCount: Int = 0,
    val badge: String = ""
)
