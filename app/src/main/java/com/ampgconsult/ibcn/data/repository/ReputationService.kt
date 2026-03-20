package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.UserReputation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReputationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserReputation(uid: String): UserReputation? {
        return try {
            val doc = firestore.collection("user_reputation").document(uid).get().await()
            doc.toObject(UserReputation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateReputationAfterSale(uid: String, amount: Double) {
        val ref = firestore.collection("user_reputation").document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) {
                transaction.set(ref, UserReputation(uid = uid, totalSales = 1, badge = "Verified Seller"))
            } else {
                val currentSales = snapshot.getLong("totalSales") ?: 0L
                val newSales = currentSales + 1
                val newBadge = if (newSales >= 10) "Top Creator" else "Verified Seller"
                transaction.update(ref, "totalSales", newSales, "badge", newBadge)
            }
        }.await()
    }
}
