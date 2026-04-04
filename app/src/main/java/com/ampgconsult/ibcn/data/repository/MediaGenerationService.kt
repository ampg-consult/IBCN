package com.ampgconsult.ibcn.data.repository

import android.content.Context
import android.util.Log
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MediaGenerationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val okHttpClient: OkHttpClient,
    @Named("baseUrl") private val baseUrl: String
) {
    private val TAG = "MediaGenerationService"
    
    private val videoEngineUrl = "https://ibcn-production-03ab.up.railway.app"

    private val client = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // For SSE
        .build()

    suspend fun generateAIJob(type: String, prompt: String, assetId: String? = null): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Please log in"))
        return try {
            val jobId = UUID.randomUUID().toString()
            val payload = JSONObject().apply {
                put("jobId", jobId)
                put("userId", uid)
                put("prompt", prompt)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$videoEngineUrl/generate-$type")
                .post(body)
                .build()

            val responseBody = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Engine error: ${response.code}")
                    response.body?.string() ?: ""
                }
            }

            val json = JSONObject(responseBody)
            Result.success(json.optString("jobId", jobId))
        } catch (e: Exception) {
            Log.e(TAG, "AI Job failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 📡 REAL-TIME SSE UPGRADE: Replaces polling with live updates
     */
    fun observeJob(jobId: String): Flow<JobStatusUpdate> = callbackFlow {
        val request = Request.Builder()
            .url("$videoEngineUrl/stream/$jobId")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    val update = JobStatusUpdate(
                        status = json.optString("status"),
                        stage = json.optString("stage"),
                        progress = json.optInt("progress"),
                        videoUrl = if (json.isNull("videoUrl")) null else json.optString("videoUrl"),
                        error = if (json.isNull("error")) null else json.optString("error")
                    )
                    trySend(update)
                    if (update.status == "completed" || update.status == "failed") {
                        close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SSE parse error", e)
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE Connection Failed"))
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    // --- Legacy Compatibility Methods ---
    
    suspend fun generateViralMedia(assetId: String, title: String, description: String): Result<ViralVideoMetadata> {
        val result = generateAIJob("video", "$title: $description", assetId)
        return if (result.isSuccess) {
            val jobId = result.getOrNull() ?: ""
            Result.success(ViralVideoMetadata(
                id = jobId,
                assetId = assetId,
                status = MediaStatus.GENERATING,
                caption = "Generating..."
            ))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed"))
        }
    }

    suspend fun getJobStatus(jobId: String): JobStatusUpdate? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$videoEngineUrl/status/$jobId").build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: "")
                JobStatusUpdate(
                    status = json.optString("status"),
                    stage = json.optString("stage"),
                    progress = json.optInt("progress"),
                    videoUrl = if (json.isNull("videoUrl")) null else json.optString("videoUrl"),
                    error = if (json.isNull("error")) null else json.optString("error")
                )
            }
        } catch (e: Exception) { null }
    }

    suspend fun saveMediaToFirestore(media: ViralVideoMetadata) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).collection("videos").document(media.id).set(media).await()
        } catch (e: Exception) { }
    }

    suspend fun getMediaForAsset(assetId: String): ViralVideoMetadata? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid).collection("videos")
                .whereEqualTo("assetId", assetId).limit(1).get().await()
                .toObjects(ViralVideoMetadata::class.java).firstOrNull()
        } catch (e: Exception) { null }
    }

    suspend fun trackShare(mediaId: String, platform: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val ref = firestore.collection("users").document(uid).collection("videos").document(mediaId).collection("metrics").document("shares")
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) tx.set(ref, mapOf(platform to 1))
                else tx.update(ref, platform, (snap.getLong(platform) ?: 0L) + 1)
            }.await()
        } catch (e: Exception) { }
    }
}
