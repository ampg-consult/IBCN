package com.ampgconsult.ibcn.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIAssistantService @Inject constructor(
    private val client: OkHttpClient
) {
    // PART 2 — AUTO CONNECT
    private val baseUrl = "http://10.0.2.2:11434/api/generate"

    suspend fun getArchitectureSuggestion(prompt: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            // PART 4 — FIX REQUEST FORMAT
            put("model", "qwen2.5-coder:7b")
            put("prompt", "As an AI System Architect for IBCN, suggest an architecture for: $prompt")
            put("stream", false)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "AI unavailable. Please try again later!."
                val body = response.body?.string() ?: return@withContext "AI unavailable. Please try again later!."
                JSONObject(body).getString("response")
            }
        } catch (e: Exception) {
            // PART 6 — ERROR MESSAGE
            "AI unavailable. Please try again later!."
        }
    }
}
