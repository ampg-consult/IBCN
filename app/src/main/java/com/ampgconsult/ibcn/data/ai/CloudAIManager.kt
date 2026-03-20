package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.OpenAIClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudAIManager @Inject constructor(
    private val openAIClient: OpenAIClient
) {
    fun streamAIRequest(prompt: String, systemPrompt: String = "You are a senior AI assistant for IBCN."): Flow<String> {
        return openAIClient.streamChatCompletion(prompt, systemPrompt)
    }

    suspend fun requestAI(prompt: String, systemPrompt: String = "You are a senior AI assistant for IBCN."): Result<String> {
        return openAIClient.getChatCompletion(prompt, systemPrompt)
    }
}
