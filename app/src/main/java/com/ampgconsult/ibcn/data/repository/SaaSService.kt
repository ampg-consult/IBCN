package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.SaaSAnalytics
import com.ampgconsult.ibcn.data.models.SaaSProduct
import com.ampgconsult.ibcn.data.models.SubscriptionTier
import com.ampgconsult.ibcn.data.models.UserSubscription
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class SaaSService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val paymentManager: PaymentManager
) {
    /**
     * Auto-converts a project into a SaaS product.
     */
    suspend fun convertProjectToSaaS(
        projectId: String,
        name: String,
        description: String,
        features: List<String>
    ): Result<SaaSProduct> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val productId = UUID.randomUUID().toString()
            val product = SaaSProduct(
                id = productId,
                projectId = projectId,
                ownerId = uid,
                name = name,
                description = description,
                features = features,
                landingPageUrl = "https://ibcn.app/s/$productId",
                createdAt = Timestamp.now()
            )
            
            firestore.collection("saas_products").document(productId).set(product).await()
            
            // Initialize analytics for this product
            val initialAnalytics = SaaSAnalytics(productId = productId)
            firestore.collection("saas_analytics").document(productId).set(initialAnalytics).await()
            
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Subscribe a user to a SaaS product.
     */
    suspend fun subscribeToProduct(
        productId: String,
        tier: SubscriptionTier
    ): Result<UserSubscription> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val product = getProduct(productId) ?: return Result.failure(Exception("Product not found"))
            val price = product.pricing[tier] ?: 0.0
            
            if (price > 0) {
                // Initialize payment
                val paymentResult = paymentManager.initializePayment(
                    amount = price,
                    provider = com.ampgconsult.ibcn.data.models.PaymentProvider.Stripe,
                    metadata = mapOf("productId" to productId, "tier" to tier.name, "type" to "subscription")
                )
                
                if (paymentResult.isFailure) return Result.failure(paymentResult.exceptionOrNull()!!)
                
                // Verify payment (Simplified for demo, in prod wait for webhook)
                paymentManager.verifyPayment(paymentResult.getOrThrow())
            }

            val subscription = UserSubscription(
                userId = uid,
                productId = productId,
                tier = tier,
                startDate = Timestamp.now(),
                endDate = null, // In a real app, calculate based on interval (monthly/yearly)
                status = "active"
            )

            firestore.collection("user_subscriptions")
                .document("${uid}_${productId}")
                .set(subscription).await()
                
            // Update product analytics
            firestore.collection("saas_products").document(productId)
                .update("activeSubscribers", FieldValue.increment(1)).await()
                
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a user has access to a specific feature based on their subscription.
     */
    suspend fun hasFeatureAccess(productId: String, requiredTier: SubscriptionTier): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        
        return try {
            val subDoc = firestore.collection("user_subscriptions")
                .document("${uid}_${productId}")
                .get().await()
                
            if (!subDoc.exists()) return requiredTier == SubscriptionTier.FREE
            
            val subscription = subDoc.toObject(UserSubscription::class.java) ?: return false
            if (subscription.status != "active") return false
            
            subscription.tier.ordinal >= requiredTier.ordinal
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProduct(productId: String): SaaSProduct? {
        return try {
            firestore.collection("saas_products").document(productId).get().await()
                .toObject(SaaSProduct::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAnalytics(productId: String): SaaSAnalytics? {
        return try {
            firestore.collection("saas_analytics").document(productId).get().await()
                .toObject(SaaSAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
