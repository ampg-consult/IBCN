package com.ampgconsult.ibcn.data.network

interface AIProvider {
    suspend fun generateResponse(prompt: String): Result<String>
    suspend fun isAvailable(): Boolean
}
