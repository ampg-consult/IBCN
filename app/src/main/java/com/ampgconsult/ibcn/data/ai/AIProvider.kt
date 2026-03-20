package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentType
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    fun streamResponse(prompt: String, agentType: AgentType): Flow<String>
    suspend fun getResponse(prompt: String, agentType: AgentType): Result<String>
    suspend fun isAvailable(): Boolean
}
