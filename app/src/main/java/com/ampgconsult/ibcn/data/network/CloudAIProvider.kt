package com.ampgconsult.ibcn.data.network

import com.ampgconsult.ibcn.data.models.AgentType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CloudAIProvider @Inject constructor(
    private val client: OkHttpClient
) {
    // This is designed for future expansion to OpenAI, Anthropic, or Google Gemini
    private val baseUrl = "https://api.ibcn.ai/v1/generate" 

    suspend fun generateResponse(prompt: String, agentType: AgentType? = null): Result<String> = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("prompt", prompt)
            agentType?.let { put("agent_role", it.name) }
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(json.toString().toRequestBody())
            .header("Authorization", "Bearer ${System.getenv("IBCN_CLOUD_KEY") ?: ""}")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Cloud Error: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty cloud response"))
                Result.success(JSONObject(body).getString("response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isAvailable(): Boolean = true // expansion logic for cloud availability check
}
