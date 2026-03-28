package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.BusinessOrchestrator
import com.ampgconsult.ibcn.data.models.AIBusiness
import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.BusinessPhase
import com.ampgconsult.ibcn.data.models.BusinessStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessLaunchViewModel @Inject constructor(
    private val orchestrator: BusinessOrchestrator,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    data class LaunchUiState(
        val name: String = "",
        val idea: String = "",
        val isLaunching: Boolean = false,
        val isComplete: Boolean = false,
        val logs: List<AgentResponse> = emptyList(),
        val businesses: List<AIBusiness> = emptyList(),
        val createdProjectId: String? = null,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(LaunchUiState())
    val uiState: StateFlow<LaunchUiState> = _uiState

    init {
        loadBusinesses()
        observeLaunchFlow()
    }

    private fun loadBusinesses() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("businesses")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(AIBusiness::class.java) ?: emptyList()
                _uiState.value = _uiState.value.copy(businesses = list)
            }
    }

    private fun observeLaunchFlow() {
        viewModelScope.launch {
            orchestrator.launchFlow.collect { response ->
                _uiState.value = _uiState.value.copy(
                    logs = _uiState.value.logs + response
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onIdeaChange(idea: String) {
        _uiState.value = _uiState.value.copy(idea = idea)
    }

    fun launchStartup() {
        val name = _uiState.value.name
        val idea = _uiState.value.idea
        if (name.isBlank() || idea.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLaunching = true, logs = emptyList(), errorMessage = null)
            val result = orchestrator.startFullLaunch(name, idea)
            result.onSuccess { projectId ->
                _uiState.value = _uiState.value.copy(isLaunching = false, isComplete = true, createdProjectId = projectId)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLaunching = false, errorMessage = error.message)
            }
        }
    }
}
