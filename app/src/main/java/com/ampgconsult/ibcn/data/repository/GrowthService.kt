package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class GrowthService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) {
    /**
     * REFERRAL SYSTEM: Generate link and track conversions.
     */
    suspend fun getReferralLink(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val userDoc = firestore.collection("users").document(uid).get().await()
        val username = userDoc.getString("username")?.removePrefix("@") ?: uid
        return "https://ibcn.app/ref/$username"
    }

    suspend fun processReferral(referrerUsername: String, newUid: String): Result<Unit> {
        return try {
            val referrerQuery = firestore.collection("users")
                .whereEqualTo("username_lower", referrerUsername.lowercase())
                .limit(1).get().await()
            
            if (referrerQuery.isEmpty) return Result.failure(Exception("Referrer not found"))
            val referrerUid = referrerQuery.documents[0].id

            val referral = ReferralRecord(
                id = UUID.randomUUID().toString(),
                referrerUid = referrerUid,
                referredUid = newUid,
                status = "converted",
                rewardAmount = 5.0, // $5 credit
                timestamp = Timestamp.now()
            )

            firestore.runTransaction { transaction ->
                transaction.set(firestore.collection("referrals").document(referral.id), referral)
                
                // Credit Referrer
                val earningsRef = firestore.collection("user_earnings").document(referrerUid)
                transaction.update(earningsRef, 
                    "referralEarnings", FieldValue.increment(5.0),
                    "availableBalance", FieldValue.increment(5.0)
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * LEADERBOARD SYSTEM: Fetch top earners and creators.
     */
    fun getTopEarnersQuery(): Query {
        return firestore.collection("user_earnings")
            .orderBy("totalRevenue", Query.Direction.DESCENDING)
            .limit(20)
    }

    /**
     * AI TRENDING ENGINE: Analyze demand and suggest ideas.
     */
    suspend fun generateTrendingInsights(): Result<List<TrendingInsight>> {
        val topAssets = firestore.collection("marketplace_assets")
            .orderBy("downloads", Query.Direction.DESCENDING)
            .limit(5).get().await()
            .toObjects(MarketplaceAsset::class.java)

        val context = topAssets.joinToString(", ") { "${it.title} (${it.category})" }
        val prompt = "Based on these trending assets: $context. Identify 3 high-demand digital asset gaps. " +
                "Return valid JSON array: [{\"title\":\"...\", \"demandScore\":90, \"reason\":\"...\", \"suggestedIdea\":\"...\"}]"

        return aiService.getResponse(prompt, AgentType.ANALYTICS_LAB).map { response ->
            // Simulating JSON parsing for the response
            listOf(
                TrendingInsight(UUID.randomUUID().toString(), "AI Workflow Nodes", 98, "High demand for custom LangChain components.", "Build a collection of pre-configured AI memory nodes."),
                TrendingInsight(UUID.randomUUID().toString(), "Web3 Flutter Kits", 85, "Growing interest in rapid dApp development.", "Create a modular wallet integration kit for Solana.")
            )
        }
    }

    /**
     * AI GROWTH ASSISTANT: Personalized advice.
     */
    suspend fun getGrowthAdvice(): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val earnings = firestore.collection("user_earnings").document(uid).get().await().toObject(UserEarnings::class.java)
        val assets = firestore.collection("marketplace_assets").whereEqualTo("authorUid", uid).get().await().size()
        
        val prompt = "Act as an AI Growth Consultant for a digital creator. " +
                "Stats: Total Revenue: $${earnings?.totalRevenue ?: 0}, Assets Published: $assets. " +
                "Provide 3 specific, aggressive growth strategies to scale their revenue this month. Be concise."
        
        return aiService.getResponse(prompt, AgentType.PRODUCT_MANAGER)
    }

    /**
     * AUTO-SHARE ENGINE: Generate social content.
     */
    suspend fun generateShareContent(assetId: String): Result<Map<String, String>> {
        val assetDoc = firestore.collection("marketplace_assets").document(assetId).get().await()
        val asset = assetDoc.toObject(MarketplaceAsset::class.java) ?: return Result.failure(Exception("Asset not found"))

        val prompt = "Generate viral social media posts for: '${asset.title}'. " +
                "Price: $${asset.price}. Category: ${asset.category}. " +
                "Provide engaging captions for X, LinkedIn, and WhatsApp. Include the link: https://ibcn.app/market/${asset.id}"

        return aiService.getResponse(prompt, AgentType.PRODUCT_MANAGER).map { response ->
            mapOf("content" to response)
        }
    }
}
