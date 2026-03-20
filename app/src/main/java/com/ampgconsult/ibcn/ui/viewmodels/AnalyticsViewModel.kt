package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.ActivityEvent
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.SystemMetrics
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val aiService: AIService
) : ViewModel() {

    private val _metrics = MutableStateFlow<SystemMetrics?>(null)
    val metrics: StateFlow<SystemMetrics?> = _metrics

    private val _activities = MutableStateFlow<List<ActivityEvent>>(emptyList())
    val activities: StateFlow<List<ActivityEvent>> = _activities

    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchAnalyticsData()
    }

    fun fetchAnalyticsData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Stream System Metrics
            firestore.collection("system_metrics").document("current")
                .addSnapshotListener { snapshot, _ ->
                    val newMetrics = snapshot?.toObject(SystemMetrics::class.java)
                    _metrics.value = newMetrics
                    if (newMetrics != null) {
                        generateAIInsights(newMetrics)
                    }
                }

            // Stream Recent Activities
            firestore.collection("system_activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener { snapshot, _ ->
                    _activities.value = snapshot?.toObjects(ActivityEvent::class.java) ?: emptyList()
                    _isLoading.value = false
                }
        }
    }

    private fun generateAIInsights(metrics: SystemMetrics) {
        viewModelScope.launch {
            val prompt = "Analyze the following system metrics and provide a 1-sentence strategic insight: " +
                    "CPU: ${metrics.cpuUsage}, RAM: ${metrics.ramUsage}, AI Health: ${metrics.aiHealth}, " +
                    "Uptime: ${metrics.uptime}. Status: ${metrics.statusMessage}"
            
            val result = aiService.getResponse(prompt, AgentType.ANALYTICS_LAB)
            _aiInsights.value = result.getOrNull()
        }
    }
}
