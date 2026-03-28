package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.SystemMetrics
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * Aggregates metrics across SaaS, Marketplace, and Growth engines.
     */
    suspend fun getGlobalMetrics(projectId: String): Result<SystemMetrics> {
        return try {
            val saasDoc = firestore.collection("saas_analytics").document(projectId).get().await()
            val marketQuery = firestore.collection("marketplace_assets")
                .whereEqualTo("projectId", projectId).get().await()
            
            val totalDownloads = marketQuery.documents.sumOf { it.getLong("downloadCount") ?: 0L }
            val totalRevenue = saasDoc.getDouble("totalRevenue") ?: 0.0
            
            val metrics = SystemMetrics(
                projectId = projectId,
                activeUsers = saasDoc.getLong("activeSubscribers")?.toInt() ?: 0,
                revenue = totalRevenue,
                downloads = totalDownloads.toInt(),
                uptime = "99.9%",
                timestamp = Timestamp.now()
            )
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tracks a custom event (e.g., "build_deployed", "investment_received").
     */
    suspend fun trackEvent(eventName: String, params: Map<String, Any>) {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val log = params.toMutableMap().apply {
            put("event", eventName)
            put("uid", uid)
            put("timestamp", FieldValue.serverTimestamp())
        }
        firestore.collection("activity_logs").add(log)
    }
}
