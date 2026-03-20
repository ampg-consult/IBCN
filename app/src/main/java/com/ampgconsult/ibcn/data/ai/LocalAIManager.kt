package com.ampgconsult.ibcn.data.ai

import android.util.Log
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.OllamaClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

enum class AIState {
    LOCAL_AVAILABLE,
    LOCAL_UNAVAILABLE
}

@Singleton
class LocalAIManager @Inject constructor(
    private val ollamaClient: OllamaClient
) {
    private val TAG = "LocalAIManager"
    private val _aiState = MutableStateFlow(AIState.LOCAL_UNAVAILABLE)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    private val _installedModels = MutableStateFlow<List<String>>(emptyList())
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        discoverOllama()
    }

    fun discoverOllama() {
        scope.launch {
            if (ollamaClient.isAvailable()) {
                _installedModels.value = ollamaClient.getInstalledModels()
                _aiState.value = AIState.LOCAL_AVAILABLE
                Log.i(TAG, "Ollama connected automatically")
            } else {
                _aiState.value = AIState.LOCAL_UNAVAILABLE
                Log.e(TAG, "Ollama unavailable")
            }
        }
    }

    private fun getModelToUse(agentType: AgentType?): String {
        // Objective: Local model is phi3
        val installed = _installedModels.value
        return if (installed.contains("phi3")) "phi3" else if (installed.isNotEmpty()) installed.first() else "phi3"
    }

    fun streamAIRequest(prompt: String, agentType: AgentType? = null): Flow<String> = flow {
        if (_aiState.value == AIState.LOCAL_AVAILABLE) {
            val model = getModelToUse(agentType)
            ollamaClient.generateStream(prompt, model)
                .catch { e ->
                    Log.e(TAG, "Local stream failed: ${e.message}")
                    emit("Error: Local AI connection lost.")
                }
                .collect { emit(it) }
        } else {
            emit("AI unavailable. Please try again later!.")
        }
    }

    suspend fun requestAI(prompt: String, agentType: AgentType? = null): Result<String> {
        if (_aiState.value == AIState.LOCAL_AVAILABLE) {
            val model = getModelToUse(agentType)
            return ollamaClient.generate(prompt, model)
        }
        return Result.failure(Exception("AI unavailable. Please try again later!."))
    }
}
