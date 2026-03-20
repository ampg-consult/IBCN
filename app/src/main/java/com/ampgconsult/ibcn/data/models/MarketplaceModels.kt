package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class MarketplaceOrder(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val assetId: String = "",
    val assetTitle: String = "",
    val amount: Double = 0.0,
    val status: String = "completed", // "pending", "completed", "failed"
    val createdAt: Timestamp = Timestamp.now()
)

data class UserReputation(
    val uid: String = "",
    val totalSales: Int = 0,
    val totalDownloads: Int = 0,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val badge: String = "New Builder" // "New Builder", "Verified Seller", "Top Creator"
)

data class AssetReview(
    val id: String = "",
    val assetId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val assetContextId: String? = null // Optional: if chat started from an asset
)
