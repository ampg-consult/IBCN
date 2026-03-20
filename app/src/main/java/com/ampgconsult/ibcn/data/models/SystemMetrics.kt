package com.ampgconsult.ibcn.data.models

data class SystemMetrics(
    val uptime: String = "0%",
    val aiHealth: String = "Unknown",
    val statusMessage: String = "Connecting to IBCN Core...",
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f,
    val gpuUsage: Float = 0f,
    val taskSuccessRate: String = "0%",
    val responseLatency: String = "0ms",
    val agentExecutionCount: Int = 0,
    val agentEfficiencies: Map<String, Float> = emptyMap()
)

data class ActivityEvent(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
