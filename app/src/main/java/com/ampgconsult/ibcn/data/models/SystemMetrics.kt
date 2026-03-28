package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

/**
 * Unified Analytics model for IBCN.
 * Supports both platform health metrics and project-specific SaaS performance.
 */
data class SystemMetrics(
    // Platform Health
    val uptime: String = "0%",
    val aiHealth: String = "Unknown",
    val statusMessage: String = "Connecting to IBCN Core...",
    val cpuUsage: Float = 0f,
    val ramUsage: Float = 0f,
    val gpuUsage: Float = 0f,
    val taskSuccessRate: String = "0%",
    val responseLatency: String = "0ms",
    val agentExecutionCount: Int = 0,
    val agentEfficiencies: Map<String, Float> = emptyMap(),
    
    // Project-specific metrics (Phase 5: Analytics Lab)
    val projectId: String = "",
    val activeUsers: Int = 0,
    val revenue: Double = 0.0,
    val downloads: Int = 0,
    val timestamp: Timestamp = Timestamp.now()
)

data class ActivityEvent(
    val id: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
