package com.ampgconsult.ibcn.modules.marketplace.models

import com.google.firebase.Timestamp

data class PluginMetadata(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val developerUid: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: PluginCategory = PluginCategory.UTILITY,
    val permissions: List<String> = emptyList(),
    val rating: Float = 0f,
    val downloads: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

enum class PluginCategory {
    AI_AGENT, UI_THEME, DATA_CONNECTOR, UTILITY, BUSINESS
}

interface IBCNPlugin {
    val metadata: PluginMetadata
    fun onInstall()
    fun onActivate()
    fun onDeactivate()
}
