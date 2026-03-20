package com.ampgconsult.ibcn.data.network

import com.ampgconsult.ibcn.BuildConfig
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "OpenAIClient"
    
    // Pulling from BuildConfig
    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val baseUrl = "https://api.openai.com/v1/chat/completions"
    
    // Primary model for production
    private val model = "gpt-4o-mini"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChatCompletion(prompt: String, systemPrompt: String): Flow<String> = callbackFlow {
        if (apiKey.isBlank()) {
            close(IOException("OpenAI API Key is missing."))
            return@callbackFlow
        }

        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("stream", true)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isClosedForSend) trySend("Error: ${e.message}")
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "OpenAI Error: ${response.code} - $errorBody")
                    close(IOException("OpenAI Error: ${response.code}"))
                    return
                }

                response.body?.source()?.let { source ->
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line()
                            if (line != null && line.startsWith("data: ")) {
                                val data = line.substring(6)
                                if (data == "[DONE]") break
                                
                                val jsonResponse = JSONObject(data)
                                val choices = jsonResponse.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    if (delta != null && delta.has("content")) {
                                        trySend(delta.getString("content"))
                                    }
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

    suspend fun getChatCompletion(prompt: String, systemPrompt: String, retries: Int = 1): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key missing"))

        var lastException: Exception? = null
        repeat(retries + 1) { attempt ->
            try {
                val json = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("stream", false)
                }

                val request = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", "Bearer $apiKey")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: throw IOException("Empty body")
                        val result = JSONObject(body)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        return@withContext Result.success(result)
                    } else {
                        val error = response.body?.string() ?: "Unknown error"
                        Log.e(TAG, "OpenAI API Error: ${response.code} - $error")
                        throw IOException("OpenAI Error: ${response.code}")
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
