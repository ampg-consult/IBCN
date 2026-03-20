package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.repository.CollaborationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollaborationUiState(
    val members: List<ProjectMember> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val activity: List<ProjectActivity> = emptyList(),
    val invites: List<ProjectInvite> = emptyList(),
    val aiTaskSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CollaborationViewModel @Inject constructor(
    private val collaborationService: CollaborationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollaborationUiState())
    val uiState: StateFlow<CollaborationUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    fun loadProjectCollaboration(projectId: String) {
        currentProjectId = projectId
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            // Combine multiple streams for real-time updates
            combine(
                collaborationService.getProjectMembers(projectId),
                collaborationService.getProjectMessages(projectId),
                collaborationService.getActivityFeed(projectId)
            ) { members, messages, activity ->
                _uiState.update { it.copy(
                    members = members,
                    messages = messages,
                    activity = activity,
                    isLoading = false
                ) }
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect()
        }
    }

    fun loadMyInvites() {
        viewModelScope.launch {
            collaborationService.getMyInvites().collect { invites ->
                _uiState.update { it.copy(invites = invites) }
            }
        }
    }

    fun sendInvite(projectId: String, invitedUserId: String) {
        viewModelScope.launch {
            val result = collaborationService.sendInvite(projectId, invitedUserId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun respondToInvite(inviteId: String, accept: Boolean) {
        viewModelScope.launch {
            val result = collaborationService.respondToInvite(inviteId, accept)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun sendMessage(projectId: String, text: String) {
        viewModelScope.launch {
            collaborationService.sendMessage(projectId, text)
        }
    }

    fun getAiSuggestions(projectId: String) {
        viewModelScope.launch {
            val result = collaborationService.getAiTaskSuggestions(projectId)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiTaskSuggestions = result.getOrNull() ?: emptyList()) }
            }
        }
    }
}
