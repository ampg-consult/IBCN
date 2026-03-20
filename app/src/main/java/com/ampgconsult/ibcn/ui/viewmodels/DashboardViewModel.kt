package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.Project
import com.ampgconsult.ibcn.data.models.User
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _quickAiResponse = MutableStateFlow("")
    val quickAiResponse: StateFlow<String> = _quickAiResponse

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking

    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadUserProfile()
        refreshProjects()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    _currentUser.value = snapshot?.toObject(User::class.java)
                }
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            repository.refreshProjects()
        }
    }

    fun quickAskAi(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isAiThinking.value = true
            _quickAiResponse.value = ""
            
            aiService.streamResponse(query, AgentType.DEVELOPER)
                .catch { e ->
                    _quickAiResponse.value = "AI unavailable: ${e.message}"
                }
                .onCompletion {
                    _isAiThinking.value = false
                }
                .collect { chunk ->
                    _quickAiResponse.value += chunk
                }
        }
    }

    fun createNewProject(name: String, description: String) {
        viewModelScope.launch {
            repository.createProject(name, description)
        }
    }
}
