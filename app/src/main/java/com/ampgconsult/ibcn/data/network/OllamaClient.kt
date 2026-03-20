package com.ampgconsult.ibcn.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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
class OllamaClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "OllamaClient"
    
    // PART 2 — AUTO CONNECT (Hardcoded for Android Emulator)
    private val baseUrl = "http://10.0.2.2:11434"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val responseCache = mutableMapOf<String, String>()

    // PART 3 — HEALTH CHECK
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ollama health check failed: ${e.message}")
            false
        }
    }

    suspend fun getInstalledModels(): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models") ?: return@withContext emptyList()
                val models = mutableListOf<String>()
                for (i in 0 until modelsArray.length()) {
                    models.add(modelsArray.getJSONObject(i).getString("name"))
                }
                models
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // PART 4 — FIX REQUEST FORMAT (Streaming)
    fun generateStream(prompt: String, model: String): Flow<String> = callbackFlow {
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
                                try {
                                    val jsonResponse = JSONObject(line)
                                    val chunk = jsonResponse.optString("response", "")
                                    if (chunk.isNotEmpty()) {
                                        trySend(chunk)
                                    }
                                    if (jsonResponse.optBoolean("done", false)) {
                                        break
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing JSON line: $line", e)
                                }
                            }
                        }
                        close()
                    } catch (e: Exception) {
                        close(e)
                    }
                } ?: close(IOException("Empty response body"))
            }
        })

        awaitClose { call.cancel() }
    }

    // PART 4 — FIX REQUEST FORMAT (Unary)
    suspend fun generate(prompt: String, model: String, retries: Int = 1): Result<String> = withContext(Dispatchers.IO) {
        val cacheKey = "$model:$prompt"
        if (responseCache.containsKey(cacheKey)) {
            return@withContext Result.success(responseCache[cacheKey]!!)
        }

        var lastException: Exception? = null
        repeat(retries + 1) { attempt ->
            try {
                val json = JSONObject().apply {
                    put("model", model)
                    put("prompt", prompt)
                    put("stream", false)
                }

                val request = Request.Builder()
                    .url("$baseUrl/api/generate")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: throw IOException("Empty body")
                        val result = JSONObject(body).getString("response")
                        responseCache[cacheKey] = result
                        return@withContext Result.success(result)
                    } else {
                        throw IOException("HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < retries) delay(1000)
            }
        }
        Result.failure(lastException ?: Exception("Unknown error"))
    }
}
