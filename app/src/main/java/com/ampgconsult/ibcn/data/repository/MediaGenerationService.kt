package com.ampgconsult.ibcn.data.repository

import android.content.Context
import android.util.Log
import com.ampgconsult.ibcn.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*
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
    
    // Using api.ibcn.site as primary, falling back to Railway domain if needed
    private val videoEngineUrl = "https://api.ibcn.site" 

    /**
     * Unified entry point for all AI Jobs (Video, Launchpad, etc.)
     * Returns the jobId confirmed by the server.
     */
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
            val finalUrl = "$videoEngineUrl/generate-$type"
            
            Log.d(TAG, "Requesting AI Job: $finalUrl")

            val request = Request.Builder()
                .url(finalUrl)
                .post(body)
                .build()

            val responseBody = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: ""
                        Log.e(TAG, "Server Error ${response.code}: $err")
                        throw Exception("Engine error: ${response.code}")
                    }
                    response.body?.string() ?: ""
                }
            }

            val json = JSONObject(responseBody)
            val serverJobId = json.optString("jobId", jobId)

            if (type == "video" && assetId != null) {
                val initialMedia = ViralVideoMetadata(
                    id = serverJobId,
                    assetId = assetId,
                    status = MediaStatus.GENERATING,
                    caption = "Initializing..."
                )
                saveMediaToFirestore(initialMedia)
            }
            Result.success(serverJobId)
        } catch (e: Exception) {
            Log.e(TAG, "AI Job failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Legacy compatibility wrapper for existing business logic components.
     */
    suspend fun generateViralMedia(assetId: String, title: String, description: String): Result<ViralVideoMetadata> {
        val result = generateAIJob("video", "$title: $description", assetId)
        return if (result.isSuccess) {
            val jobId = result.getOrNull() ?: ""
            Result.success(ViralVideoMetadata(
                id = jobId,
                assetId = assetId,
                status = MediaStatus.GENERATING,
                caption = "Generating video..."
            ))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Video generation failed"))
        }
    }

    /**
     * Authoritative job status polling.
     * Uses JobStatusUpdate from com.ampgconsult.ibcn.data.models
     */
    suspend fun getJobStatus(jobId: String): JobStatusUpdate? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$videoEngineUrl/status/$jobId")
                .header("Cache-Control", "no-cache")
                .build()
            
            val responseBody = okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: return@withContext null

            val json = JSONObject(responseBody)
            
            // Map the result JSONObject to a Map for compatibility with our global model
            val resultMap = mutableMapOf<String, Any>()
            json.optJSONObject("result")?.let { res ->
                val keys = res.keys()
                while(keys.hasNext()) {
                    val key = keys.next()
                    resultMap[key] = res.get(key)
                }
            }

            JobStatusUpdate(
                status = json.optString("status"),
                stage = json.optString("stage"),
                progress = json.optInt("progress"),
                videoUrl = if (json.isNull("videoUrl")) null else json.optString("videoUrl"),
                result = if (resultMap.isEmpty()) null else resultMap,
                error = if (json.isNull("error")) null else json.optString("error")
            )
        } catch (e: Exception) { 
            Log.e(TAG, "Status check error: ${e.message}")
            null 
        }
    }

    suspend fun saveMediaToFirestore(media: ViralVideoMetadata) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val updateData = hashMapOf(
                "id" to media.id,
                "assetId" to media.assetId,
                "videoUrl" to media.videoUrl,
                "thumbnailUrl" to media.thumbnailUrl,
                "caption" to media.caption,
                "hashtags" to media.hashtags,
                "status" to media.status.name,
                "createdAt" to media.createdAt
            )
            firestore.collection("users").document(uid)
                .collection("videos").document(media.id)
                .set(updateData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore save error: ${e.message}")
        }
    }

    suspend fun getMediaForAsset(assetId: String): ViralVideoMetadata? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid)
                .collection("videos")
                .whereEqualTo("assetId", assetId)
                .limit(1).get().await()
                .toObjects(ViralVideoMetadata::class.java).firstOrNull()
        } catch (e: Exception) { null }
    }

    suspend fun trackShare(mediaId: String, platform: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val ref = firestore.collection("users").document(uid)
                .collection("videos").document(mediaId)
                .collection("metrics").document("shares")
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                if (!snapshot.exists()) {
                    transaction.set(ref, mapOf(platform to 1))
                } else {
                    val count = snapshot.getLong(platform) ?: 0L
                    transaction.update(ref, platform, count + 1)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking share: ${e.message}")
        }
    }
}
