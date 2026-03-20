package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class MarketplaceAsset(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Templates",
    val authorUsername: String = "",
    val authorUid: String = "",
    val price: Double = 0.0,
    val rating: Double = 0.0,
    val downloads: Int = 0,
    val previewImages: List<String> = emptyList(),
    val assetUrl: String = "",
    val version: String = "1.0.0",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val tags: List<String> = emptyList(),
    val documentation: String = "",
    val type: String = "Templates"
)

enum class AssetCategory(val displayName: String) {
    FLUTTER_UI("Flutter UI Kits"),
    BACKEND_API("Backend APIs"),
    AI_PROMPTS("AI Prompts"),
    DEVOPS("DevOps Templates"),
    DATABASE("Database Schemas"),
    AUTOMATION("Automation Tools")
}
