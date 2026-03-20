package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

/**
 * Unified User Model for IBCN Elite Identity Engine
 */
data class User(
    val id: String = "", 
    val uid: String = "", 
    val email: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val displayName: String = "",
    val username: String = "", // Styled: @DarLingTon_AI
    val username_lower: String = "", // Normalized: darlington_ai
    val isVerified: Boolean = false,
    val reputationScore: Int = 0,
    val badge: String = "New Builder", 
    val lastUsernameChange: Timestamp? = null,
    val photoUrl: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val created_at: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    
    // Restoration of fields for system compatibility
    val credits: Double = 0.0,
    val projects_count: Int = 0,
    val projectsCount: Int = 0,
    val reputation: Int = 0,
    val followers_count: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val rank: Int = 0,
    val role: String = "member",
    val accountType: String = "individual",
    val username_display: String = "",
    val username_canonical: String = "",
    val usernameChangeUsed: Boolean = false,
    val username_history: List<String> = emptyList()
)
