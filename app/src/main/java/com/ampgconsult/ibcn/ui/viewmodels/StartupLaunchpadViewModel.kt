package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.InvestorInsight
import com.ampgconsult.ibcn.data.models.LaunchpadConfig
import com.ampgconsult.ibcn.data.models.MediaStatus
import com.ampgconsult.ibcn.data.models.StartupProject
import com.ampgconsult.ibcn.data.repository.MediaGenerationService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LaunchpadUiState(
    val projects: List<StartupProject> = emptyList(),
    val insights: List<InvestorInsight> = emptyList(),
    val config: LaunchpadConfig = LaunchpadConfig(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val progress: Int = 0,
    val stage: String = "",
    val jobResult: Map<String, Any>? = null,
    val error: String? = null
)

@HiltViewModel
class StartupLaunchpadViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaService: MediaGenerationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchpadUiState())
    val uiState: StateFlow<LaunchpadUiState> = _uiState.asStateFlow()

    init {
        fetchLaunchpadData()
    }

    fun fetchLaunchpadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            firestore.collection("app_configs").document("launchpad")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(LaunchpadConfig::class.java)?.let { config ->
                        _uiState.update { it.copy(config = config) }
                    }
                }

            firestore.collection("startup_projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error == null) {
                        val projects = snapshot?.toObjects(StartupProject::class.java) ?: emptyList()
                        _uiState.update { it.copy(projects = projects) }
                    }
                }

            firestore.collection("investor_insights")
                .addSnapshotListener { snapshot, error ->
                    if (error == null) {
                        val insights = snapshot?.toObjects(InvestorInsight::class.java) ?: emptyList()
                        _uiState.update { it.copy(insights = insights, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun startLaunchpadJob(prompt: String) {
        _uiState.update { it.copy(isGenerating = true, error = null, progress = 0, stage = "Initializing Launchpad...") }
        viewModelScope.launch {
            val jobId = UUID.randomUUID().toString()
            val result = mediaService.generateAIJob("launchpad", prompt, jobId)
            
            if (result.isSuccess) {
                pollForJobStatus(jobId)
            } else {
                _uiState.update { it.copy(isGenerating = false, error = "Failed to start AI Launchpad: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    private fun pollForJobStatus(jobId: String) {
        viewModelScope.launch {
            var isPolling = true
            while (isPolling) {
                delay(3000)
                val statusUpdate = mediaService.getJobStatus(jobId)
                
                if (statusUpdate == null) continue

                _uiState.update { it.copy(
                    progress = statusUpdate.progress,
                    stage = statusUpdate.stage,
                    isGenerating = statusUpdate.status != "completed" && statusUpdate.status != "failed"
                ) }

                if (statusUpdate.status == "completed") {
                    _uiState.update { it.copy(
                        isGenerating = false,
                        stage = "Launchpad Ready!"
                    ) }
                    isPolling = false
                } else if (statusUpdate.status == "failed") {
                    _uiState.update { it.copy(isGenerating = false, error = statusUpdate.error ?: "Generation failed") }
                    isPolling = false
                }
            }
        }
    }
}
