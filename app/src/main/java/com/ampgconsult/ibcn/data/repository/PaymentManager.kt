package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.PaymentProvider
import com.ampgconsult.ibcn.data.models.Wallet
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class PaymentManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * PART 1 — PAYMENT PROVIDERS
     * Mocking the initialization of Stripe/Paystack checkouts.
     * In production, this would call your backend to create a Stripe Session or Paystack Transaction.
     */
    suspend fun initializePayment(
        amount: Double,
        currency: String = "USD",
        provider: PaymentProvider,
        metadata: Map<String, String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val reference = "IBCN_${UUID.randomUUID().toString().take(8)}"
            
            // 1. Create a pending transaction record
            val pendingTransaction = mapOf(
                "uid" to uid,
                "amount" to amount,
                "currency" to currency,
                "status" to "pending",
                "provider" to if (provider is PaymentProvider.Stripe) "stripe" else "paystack",
                "reference" to reference,
                "metadata" to metadata,
                "createdAt" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("pending_payments").document(reference).set(pendingTransaction).await()
            
            // In a real app, return the checkout URL from Stripe/Paystack API
            Result.success(reference)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PART 6 — WEBHOOK HANDLING / VERIFICATION
     * Securely verify transaction status
     */
    suspend fun verifyPayment(reference: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("pending_payments").document(reference).get().await()
            if (!doc.exists()) return Result.failure(Exception("Transaction not found"))
            
            val status = doc.getString("status")
            if (status == "completed") return Result.success(true)
            
            // Simulate calling Stripe/Paystack API to verify
            // For now, we manually complete it for the demo flow
            firestore.collection("pending_payments").document(reference).update("status", "completed").await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PART 2 — WALLET SYSTEM
     */
    suspend fun getWallet(): Wallet? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("wallets").document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(Wallet::class.java)
            } else {
                val newWallet = Wallet(uid = uid, currency = "USD")
                firestore.collection("wallets").document(uid).set(newWallet).await()
                newWallet
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * PART 4 — SELLER EARNINGS
     * Update seller wallet balance and track total earnings
     */
    suspend fun creditSeller(sellerId: String, amount: Double, reference: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val walletRef = firestore.collection("wallets").document(sellerId)
                val snapshot = transaction.get(walletRef)
                
                if (!snapshot.exists()) {
                    val newWallet = Wallet(
                        uid = sellerId,
                        balance = amount,
                        totalEarnings = amount,
                        lastUpdated = Timestamp.now()
                    )
                    transaction.set(walletRef, newWallet)
                } else {
                    transaction.update(walletRef, 
                        "balance", FieldValue.increment(amount),
                        "totalEarnings", FieldValue.increment(amount),
                        "lastUpdated", FieldValue.serverTimestamp()
                    )
                }
                
                // Record the credit transaction
                val txRef = firestore.collection("wallet_transactions").document()
                transaction.set(txRef, mapOf(
                    "uid" to sellerId,
                    "amount" to amount,
                    "type" to "credit",
                    "description" to "Sale of asset",
                    "reference" to reference,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PART 5 — PAYOUT SYSTEM
     */
    suspend fun requestPayout(amount: Double, bankName: String, accountNumber: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            firestore.runTransaction { transaction ->
                val walletRef = firestore.collection("wallets").document(uid)
                val wallet = transaction.get(walletRef).toObject(Wallet::class.java) 
                    ?: throw Exception("Wallet not found")
                
                if (wallet.balance < amount) {
                    throw Exception("Insufficient balance")
                }
                
                // Deduct from wallet immediately
                transaction.update(walletRef, "balance", FieldValue.increment(-amount))
                
                // Create payout record
                val payoutId = UUID.randomUUID().toString()
                val payout = mapOf(
                    "id" to payoutId,
                    "uid" to uid,
                    "amount" to amount,
                    "status" to "pending",
                    "method" to "bank",
                    "bankName" to bankName,
                    "accountNumber" to accountNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(firestore.collection("payouts").document(payoutId), payout)
                
                // Record transaction
                val txRef = firestore.collection("wallet_transactions").document()
                transaction.set(txRef, mapOf(
                    "uid" to uid,
                    "amount" to amount,
                    "type" to "debit",
                    "description" to "Withdrawal request",
                    "reference" to payoutId,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
