package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViralDistributionService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) {
    private val TAG = "ViralDistributionService"

    /**
     * Phase 1: Optimize Video for Virality via OpenAI
     */
    suspend fun optimizeForVirality(videoMetadata: ViralVideoMetadata): Result<ViralOptimization> {
        return try {
            val prompt = """
                Act as a Growth Hacking AI for IBCN. 
                Optimize this video for TikTok, Shorts, and Reels:
                Script: ${videoMetadata.script.hook} - ${videoMetadata.script.highlights}
                Caption: ${videoMetadata.caption}
                
                Return ONLY a raw valid JSON object:
                {
                  "optimizedScript": "string",
                  "hook": "string (first 3 seconds)",
                  "captions": "string (on-screen subtitles)",
                  "hashtags": ["#startup", "#ai", "#coding", "#business", "#tech"]
                }
            """.trimIndent()

            val aiResult = aiService.getResponse(prompt, AgentType.MEDIA_STRATEGIST)
            if (aiResult.isFailure) return Result.failure(aiResult.exceptionOrNull()!!)

            val json = JSONObject(aiResult.getOrThrow())
            val optimization = ViralOptimization(
                optimizedScript = json.optString("optimizedScript"),
                hook = json.optString("hook"),
                captions = json.optString("captions"),
                hashtags = json.optJSONArray("hashtags")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
            )

            // Update video metadata with optimization results
            val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            firestore.document("users/$uid/videos/${videoMetadata.id}").update("optimization", optimization).await()

            Result.success(optimization)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 2: Schedule Social Media Post
     */
    suspend fun schedulePost(
        videoId: String,
        platform: SocialPlatform,
        scheduledAt: Timestamp,
        customCaption: String? = null
    ): Result<ScheduledPost> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val videoDoc = firestore.document("users/$uid/videos/$videoId").get().await()
            val video = videoDoc.toObject(ViralVideoMetadata::class.java) ?: throw Exception("Video not found")
            
            val post = ScheduledPost(
                id = UUID.randomUUID().toString(),
                videoId = videoId,
                userId = uid,
                platform = platform,
                scheduledAt = scheduledAt,
                status = PostStatus.PENDING,
                caption = customCaption ?: "${video.optimization?.hook ?: video.script.hook} ${video.optimization?.hashtags?.joinToString(" ")}"
            )

            firestore.collection("scheduled_posts").document(post.id).set(post).await()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phase 3: Track Real-time Viral Analytics
     */
    fun getAnalytics(videoId: String): Flow<VideoAnalytics?> {
        val uid = auth.currentUser?.uid ?: return emptyFlow<VideoAnalytics?>()
        return firestore.document("users/$uid/videos/$videoId/metrics/performance")
            .snapshots()
            .map { it.toObject(VideoAnalytics::class.java) }
    }

    /**
     * AI-Driven Reposting Logic
     */
    suspend fun suggestRepostingStrategy(videoId: String): Result<String> {
        // Logic where AI analyzes performance and suggests best time/platform to repost
        val prompt = "Analyze video $videoId performance and suggest a reposting strategy for maximum reach."
        return aiService.getResponse(prompt, AgentType.ANALYTICS_LAB)
    }
}
