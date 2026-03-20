package com.ampgconsult.ibcn.modules.content_studio.services

import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.modules.content_studio.models.ContentType
import com.ampgconsult.ibcn.modules.content_studio.models.GeneratedContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentStudioService @Inject constructor(
    private val aiService: AIService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun generateArticle(prompt: String): Result<GeneratedContent> {
        val response = aiService.getResponse("Write a professional article about: $prompt", AgentType.DOC_GENERATOR)
        return response.fold(
            onSuccess = { content ->
                saveContent(content, ContentType.ARTICLE, prompt)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun generateSocialPost(prompt: String): Result<GeneratedContent> {
        val response = aiService.getResponse("Create a viral social media post for: $prompt", AgentType.PRODUCT_MANAGER)
        return response.fold(
            onSuccess = { content ->
                saveContent(content, ContentType.SOCIAL_POST, prompt)
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun saveContent(output: String, type: ContentType, prompt: String): Result<GeneratedContent> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        val content = GeneratedContent(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            prompt = prompt,
            output = output,
            authorUid = user.uid
        )
        return try {
            firestore.collection("generated_content").document(content.id).set(content).await()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
