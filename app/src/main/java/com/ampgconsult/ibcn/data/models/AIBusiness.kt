package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class AIBusiness(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val idea: String = "",
    val description: String = "",
    val budget: Double = 0.0,
    val revenue: Double = 0.0,
    val mrr: Double = 0.0,
    val userCount: Int = 0,
    val status: BusinessStatus = BusinessStatus.IDLE,
    val currentPhase: BusinessPhase = BusinessPhase.PHASE_IDEA,
    val lastOptimized: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val projectId: String = "",
    val saasProductId: String = "",
    val landingPageUrl: String = "",
    val optimizationLogs: List<BusinessActionLog> = emptyList(),
    val config: BusinessConfig = BusinessConfig()
)

data class BusinessConfig(
    val autoOptimize: Boolean = true,
    val approveMajorChanges: Boolean = true,
    val monthlySpendingLimit: Double = 100.0
)

data class BusinessActionLog(
    val action: String = "",
    val agent: String = "",
    val result: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

enum class BusinessStatus {
    IDLE, RUNNING, PAUSED, OPTIMIZING, FAILED
}

enum class BusinessPhase {
    PHASE_IDEA, PHASE_BUILD, PHASE_SAAS, PHASE_MARKETING, PHASE_LAUNCHED, PHASE_OPTIMIZING
}
