package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaGenerationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) {
    /**
     * Entry point: Trigger viral media generation for an asset.
     */
    suspend fun generateViralMedia(assetId: String, assetTitle: String, assetDescription: String): Result<ViralVideoMetadata> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            // 1. Generate Script, Caption, and Hashtags via OpenAI
            val prompt = """
                Act as a Viral Media Strategist for IBCN. 
                Create a high-converting short-form video script (9:16) for this digital asset:
                Title: $assetTitle
                Description: $assetDescription
                
                Return a JSON object with:
                - hook: A 2-second attention grabber.
                - highlights: 3 key features.
                - cta: A strong call to action.
                - caption: An engaging caption for TikTok/Shorts.
                - hashtags: List of 5 viral hashtags.
            """.trimIndent()

            val aiResult = aiService.getResponse(prompt, AgentType.MEDIA_STRATEGIST)
            if (aiResult.isFailure) return Result.failure(aiResult.exceptionOrNull()!!)

            val json = JSONObject(aiResult.getOrThrow())
            val script = VideoScript(
                hook = json.getString("hook"),
                highlights = json.getJSONArray("highlights").let { arr -> List(arr.length()) { arr.getString(it) } },
                cta = json.getString("cta")
            )

            val videoMetadata = ViralVideoMetadata(
                id = UUID.randomUUID().toString(),
                assetId = assetId,
                script = script,
                caption = json.getString("caption"),
                hashtags = json.getJSONArray("hashtags").let { arr -> List(arr.length()) { arr.getString(it) } },
                status = MediaStatus.GENERATING,
                createdAt = Timestamp.now()
            )

            // Save initial record
            firestore.collection("viral_media").document(videoMetadata.id).set(videoMetadata).await()

            // 2. Simulate Asynchronous Media Rendering (In production, this would call a Cloud Run / Remotion service)
            // Since OpenAI is the engine, we focus on the content. 
            // We'll generate a "simulated" video URL for the demo UI.
            renderVideoAsync(videoMetadata.id)

            Result.success(videoMetadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun renderVideoAsync(mediaId: String) {
        // Asynchronous update to READY status
        // In a real implementation, a backend webhook from a video service would trigger this.
        delay(5000) 
        firestore.collection("viral_media").document(mediaId).update(
            mapOf(
                "status" to MediaStatus.READY,
                "videoUrl" to "https://storage.ibcn.app/videos/$mediaId.mp4", // Mocked production path
                "thumbnailUrl" to "https://storage.ibcn.app/thumbnails/$mediaId.jpg"
            )
        )
    }

    suspend fun getMediaForAsset(assetId: String): ViralVideoMetadata? {
        return try {
            firestore.collection("viral_media")
                .whereEqualTo("assetId", assetId)
                .limit(1)
                .get()
                .await()
                .toObjects(ViralVideoMetadata::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun trackShare(mediaId: String, platform: String) {
        val ref = firestore.collection("viral_media").document(mediaId).collection("metrics").document("shares")
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) {
                transaction.set(ref, mapOf(platform to 1))
            } else {
                transaction.update(ref, platform, (snapshot.getLong(platform) ?: 0) + 1)
            }
        }.await()
    }
}
