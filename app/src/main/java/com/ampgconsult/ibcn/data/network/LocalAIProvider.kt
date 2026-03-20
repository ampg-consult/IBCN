package com.ampgconsult.ibcn.data.network

import com.ampgconsult.ibcn.data.models.AgentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAIProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    // PART 2 — AUTO CONNECT
    private val baseUrl = "http://10.0.2.2:11434"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) 
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // PART 3 — HEALTH CHECK
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .build()
        try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    // PART 4 — FIX REQUEST FORMAT
    fun generateStreamingResponse(prompt: String, agentType: AgentType): Flow<String> = callbackFlow {
        val model = agentType.preferredModel
        val json = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("stream", true)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(IOException("Ollama Error: ${response.code}"))
                    return
                }

                response.body?.source()?.let { source ->
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line()
                            if (line != null && line.isNotBlank()) {
                                val jsonResponse = JSONObject(line)
                                val chunk = jsonResponse.optString("response", "")
                                if (chunk.isNotEmpty()) {
                                    trySend(chunk)
                                }
                                if (jsonResponse.optBoolean("done", false)) {
                                    break
                                }
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        close(e)
                    }
                } ?: close(IOException("Empty body"))
            }
        })

        awaitClose { call.cancel() }
    }

    suspend fun generateResponse(prompt: String, agentType: AgentType): Result<String> = withContext(Dispatchers.IO) {
        val model = agentType.preferredModel
        val json = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("stream", false)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("Error: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                Result.success(JSONObject(body).getString("response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
