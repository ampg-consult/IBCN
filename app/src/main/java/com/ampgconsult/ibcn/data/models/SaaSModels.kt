package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

enum class SubscriptionTier {
    FREE, PRO, PREMIUM
}

data class SaaSProduct(
    val id: String = "",
    val projectId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val pricing: Map<SubscriptionTier, Double> = mapOf(
        SubscriptionTier.FREE to 0.0,
        SubscriptionTier.PRO to 19.99,
        SubscriptionTier.PREMIUM to 49.99
    ),
    val features: List<String> = emptyList(),
    val landingPageUrl: String = "",
    val status: String = "active", // "active", "archived"
    val createdAt: Timestamp = Timestamp.now(),
    val revenueTotal: Double = 0.0,
    val activeSubscribers: Int = 0
)

data class UserSubscription(
    val userId: String = "",
    val productId: String = "",
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val status: String = "active", // "active", "canceled", "expired"
    val autoRenew: Boolean = true
)

data class SaaSAnalytics(
    val productId: String = "",
    val dailyRevenue: Map<String, Double> = emptyMap(), // Date string to amount
    val totalVisitors: Int = 0,
    val conversionRate: Double = 0.0,
    val churnRate: Double = 0.0
)
