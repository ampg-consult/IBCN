package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIBuilderOrchestrator
import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.InvestorInsight
import com.ampgconsult.ibcn.data.models.LaunchpadConfig
import com.ampgconsult.ibcn.data.models.StartupProject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaunchpadUiState(
    val projects: List<StartupProject> = emptyList(),
    val insights: List<InvestorInsight> = emptyList(),
    val config: LaunchpadConfig = LaunchpadConfig(),
    val isLoading: Boolean = false,
    val isGeneratingPlan: Boolean = false,
    val aiPlan: AgentResponse? = null
)

@HiltViewModel
class StartupLaunchpadViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val aiOrchestrator: AIBuilderOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchpadUiState())
    val uiState: StateFlow<LaunchpadUiState> = _uiState.asStateFlow()

    init {
        fetchLaunchpadData()
    }

    fun fetchLaunchpadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Stream Launchpad Config
            firestore.collection("app_configs").document("launchpad")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(LaunchpadConfig::class.java)?.let { config ->
                        _uiState.update { it.copy(config = config) }
                    }
                }

            // Stream Launch-Ready Projects
            firestore.collection("startup_projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error == null) {
                        val projects = snapshot?.toObjects(StartupProject::class.java) ?: emptyList()
                        _uiState.update { it.copy(projects = projects) }
                    }
                }

            // Stream Investor Insights
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

    fun generateStartupPlan(project: StartupProject) {
        _uiState.update { it.copy(isGeneratingPlan = true) }
        viewModelScope.launch {
            aiOrchestrator.generateLaunchpadPlan(project.description)
                .onEach { response ->
                    _uiState.update { it.copy(aiPlan = response) }
                }
                .onCompletion {
                    _uiState.update { it.copy(isGeneratingPlan = false) }
                }
                .collect()
        }
    }
}
