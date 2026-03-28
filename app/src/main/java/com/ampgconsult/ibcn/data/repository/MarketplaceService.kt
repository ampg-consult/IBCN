package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.ampgconsult.ibcn.data.ai.AIAgentOrchestrator
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class MarketplaceService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService,
    private val aiOrchestrator: AIAgentOrchestrator
) {
    private val TAG = "MarketplaceService"

    private suspend fun ensureAuthReady() {
        var user = auth.currentUser
        var retries = 0
        while (user == null && retries < 50) { 
            delay(100)
            user = auth.currentUser
            retries++
        }
        user?.reload()?.await()
        if (auth.currentUser == null) throw Exception("NOT AUTHENTICATED")
    }

    // PART 4 — MARKETPLACE
    suspend fun publishAsset(
        title: String,
        description: String,
        price: Double,
        category: String,
        techStack: List<String>,
        assetUrl: String,
        previewImages: List<String> = emptyList(),
        type: String = "Templates" // FIX 4.1: Added type parameter
    ): Result<String> {
        return try {
            ensureAuthReady()
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val username = userDoc.getString("username") ?: "@Builder"

            val asset = MarketplaceAsset(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                price = price,
                category = category,
                authorUid = currentUser.uid,
                authorUsername = username,
                assetUrl = assetUrl,
                previewImages = previewImages,
                tags = techStack,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                type = type // FIX 4.1: Assign type
            )

            firestore.collection("marketplace_assets").document(asset.id).set(asset).await()
            Result.success(asset.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAssetsQuery(category: String? = null): Query {
        var query = firestore.collection("marketplace_assets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        if (category != null && category != "All") {
            query = query.whereEqualTo("category", category)
        }
        return query
    }

    // PART 8 — PURCHASE LOGIC & PART 11 — SECURE DOWNLOAD
    suspend fun processPurchase(asset: MarketplaceAsset): Result<Unit> {
        return try {
            ensureAuthReady()
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            
            firestore.runTransaction { transaction ->
                // 1. Increment downloads
                val assetRef = firestore.collection("marketplace_assets").document(asset.id)
                transaction.update(assetRef, "downloads", FieldValue.increment(1))

                // 2. Create order record
                val orderId = UUID.randomUUID().toString()
                val order = MarketplaceOrder(
                    id = orderId,
                    buyerId = currentUser.uid,
                    sellerId = asset.authorUid,
                    assetId = asset.id,
                    assetTitle = asset.title,
                    amount = asset.price,
                    createdAt = Timestamp.now()
                )
                transaction.set(firestore.collection("orders").document(orderId), order)
                
                // 3. Save to user purchases for secure download check
                val purchaseRef = firestore.collection("users").document(currentUser.uid)
                    .collection("purchases").document(asset.id)
                transaction.set(purchaseRef, mapOf("purchasedAt" to Timestamp.now()))

                // 4. Update Reputation
                updateReputation(transaction, asset.authorUid, isSale = asset.price > 0)
                
                // 5. Update Earnings
                if (asset.price > 0) {
                    val earningsRef = firestore.collection("user_earnings").document(asset.authorUid)
                    transaction.update(earningsRef, 
                        "totalRevenue", FieldValue.increment(asset.price),
                        "availableBalance", FieldValue.increment(asset.price)
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // PART 11 — SECURE DOWNLOAD CHECK
    suspend fun canDownloadAsset(assetId: String): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val doc = firestore.collection("users").document(uid)
                .collection("purchases").document(assetId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun updateReputation(transaction: com.google.firebase.firestore.Transaction, uid: String, isSale: Boolean) {
        val repRef = firestore.collection("user_reputation").document(uid)
        val snapshot = transaction.get(repRef)
        
        if (!snapshot.exists()) {
            val initialRep = UserReputation(
                uid = uid,
                totalSales = if (isSale) 1 else 0,
                totalDownloads = 1,
                rating = 5.0,
                badge = "New Builder"
            )
            transaction.set(repRef, initialRep)
        } else {
            val incrementSales = if (isSale) 1L else 0L
            transaction.update(repRef, 
                "totalSales", FieldValue.increment(incrementSales),
                "totalDownloads", FieldValue.increment(1)
            )
        }
    }

    suspend fun submitReview(assetId: String, rating: Int, comment: String): Result<Unit> {
        return try {
            ensureAuthReady()
            val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            // Check if user purchased first
            if (!canDownloadAsset(assetId)) throw Exception("Only buyers can review")

            val reviewId = UUID.randomUUID().toString()
            val review = AssetReview(
                id = reviewId,
                assetId = assetId,
                reviewerId = uid,
                reviewerName = auth.currentUser?.displayName ?: "Anonymous",
                rating = rating,
                comment = comment,
                createdAt = Timestamp.now()
            )
            firestore.collection("reviews").document(reviewId).set(review).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAIEnhancedContent(title: String, techStack: String): Result<Map<String, String>> {
        val prompt = "Optimize this asset listing for IBCN Marketplace. Title: $title, Tech: $techStack. Return Optimized Title and Professional Description."
        return aiService.getResponse(prompt, AgentType.PRODUCT_MANAGER).map { response ->
            mapOf("title" to title, "description" to response)
        }
    }

    suspend fun startChat(sellerId: String, assetId: String? = null): String {
        ensureAuthReady()
        val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val participants = listOf(uid, sellerId).sorted()
        val chatId = participants.joinToString("_")

        val chatRoom = ChatRoom(
            id = chatId,
            participants = participants,
            updatedAt = Timestamp.now(),
            assetContextId = assetId
        )
        firestore.collection("messages").document(chatId).set(chatRoom).await()
        return chatId
    }
}
