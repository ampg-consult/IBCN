package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class GeneratedAsset(
    val id: String = "",
    val idea: String = "",
    val code: String = "",
    val readme: String = "",
    val seoTitle: String = "",
    val seoDescription: String = "",
    val suggestedPrice: Double = 0.0,
    val suggestedTags: List<String> = emptyList(),
    val category: String = "",
    val socialCaptions: Map<String, String> = emptyMap(), // Platform -> Caption
    val createdAt: Timestamp = Timestamp.now()
)

data class ReferralRecord(
    val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val assetId: String? = null,
    val status: String = "pending", // "pending", "converted"
    val rewardAmount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
)

data class UserEarnings(
    val uid: String = "",
    val totalRevenue: Double = 0.0,
    val referralEarnings: Double = 0.0,
    val availableBalance: Double = 0.0,
    val currency: String = "USD"
)

data class TrendingInsight(
    val id: String = "",
    val title: String = "",
    val demandScore: Int = 0,
    val reason: String = "",
    val suggestedIdea: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val value: Double = 0.0, // Can be revenue or download count
    val rank: Int = 0,
    val badges: List<String> = emptyList()
)
