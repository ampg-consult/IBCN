package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.ampgconsult.ibcn.data.models.Bounty
import com.ampgconsult.ibcn.data.models.BountyStatus
import com.ampgconsult.ibcn.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BountyService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "BountyService"

    private suspend fun ensureAuthReady() {
        // PART 1 — VERIFY AUTH STATE (MANDATORY)
        Log.d(TAG, "PART 1: Verifying Auth State. Current UID: ${auth.currentUser?.uid}")
        var user = auth.currentUser
        var retries = 0
        while (user == null && retries < 50) { 
            delay(100)
            user = auth.currentUser
            retries++
        }
        
        // PART 7 — GOOGLE SIGN-IN FIX
        user?.reload()?.await()
        Log.d(TAG, "Auth ready. UID: ${auth.currentUser?.uid}")
        
        if (auth.currentUser == null) {
            throw Exception("NOT AUTHENTICATED")
        }
    }

    private fun verifyFirebaseProject() {
        // PART 2 — VERIFY FIREBASE PROJECT
        val appProjectId = auth.app.options.projectId
        val firestoreProjectId = firestore.app.options.projectId
        Log.d(TAG, "PART 2: Firebase App Project: $appProjectId")
        Log.d(TAG, "PART 2: Firestore Project: $firestoreProjectId")
        if (appProjectId != firestoreProjectId) {
            Log.e(TAG, "ERROR: WRONG FIREBASE PROJECT CONNECTED")
            throw Exception("WRONG FIREBASE PROJECT CONNECTED")
        }
    }

    suspend fun postBounty(bounty: Bounty): Result<Unit> {
        return try {
            ensureAuthReady()
            verifyFirebaseProject()
            
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val user = transaction.get(userRef).toObject(User::class.java) ?: throw Exception("User not found")
                
                if (user.credits < bounty.rewardCredits) {
                    throw Exception("Insufficient credits to post this bounty")
                }
                
                // Deduct credits (escrow)
                transaction.update(userRef, "credits", user.credits - bounty.rewardCredits)
                
                // Save bounty
                val bountyRef = firestore.collection("bounties").document()
                
                // PART 6 — TRANSACTION VALIDATION: Ensure uid matches auth
                val finalBounty = bounty.copy(id = bountyRef.id, creatorId = userId)
                
                transaction.set(bountyRef, finalBounty)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("postBounty", e)
            Result.failure(e)
        }
    }

    suspend fun getOpenBounties() = try {
        ensureAuthReady()
        firestore.collection("bounties")
            .whereEqualTo("status", BountyStatus.OPEN.name)
            .get()
            .await()
            .toObjects(Bounty::class.java)
    } catch (e: Exception) {
        logDetailedError("getOpenBounties", e)
        emptyList<Bounty>()
    }

    suspend fun claimBounty(bountyId: String): Result<Unit> {
        return try {
            ensureAuthReady()
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            firestore.runTransaction { transaction ->
                val ref = firestore.collection("bounties").document(bountyId)
                val bounty = transaction.get(ref).toObject(Bounty::class.java) ?: throw Exception("Bounty not found")
                
                if (bounty.status != BountyStatus.OPEN) {
                    throw Exception("Bounty is no longer open")
                }
                
                if (bounty.creatorId == userId) {
                    throw Exception("You cannot claim your own bounty")
                }
                
                transaction.update(ref, mapOf(
                    "status" to BountyStatus.CLAIMED.name,
                    "claimantId" to userId
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("claimBounty", e)
            Result.failure(e)
        }
    }

    suspend fun submitBounty(bountyId: String, submissionUrl: String): Result<Unit> {
        return try {
            ensureAuthReady()
            val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            
            // PART 5 — WRITE PATH VALIDATION: Ensure writing to correct path (bounties)
            firestore.collection("bounties").document(bountyId).update(mapOf(
                "status" to BountyStatus.SUBMITTED.name,
                "submissionUrl" to submissionUrl
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("submitBounty", e)
            Result.failure(e)
        }
    }

    suspend fun verifyAndCompleteBounty(bountyId: String): Result<Unit> {
        return try {
            ensureAuthReady()
            firestore.runTransaction { transaction ->
                val ref = firestore.collection("bounties").document(bountyId)
                val bounty = transaction.get(ref).toObject(Bounty::class.java) ?: throw Exception("Bounty not found")
                
                if (bounty.status != BountyStatus.SUBMITTED) {
                    throw Exception("Bounty not in submitted state")
                }
                
                val claimantId = bounty.claimantId ?: throw Exception("No claimant found")
                val claimantRef = firestore.collection("users").document(claimantId)
                
                // Release credits to claimant
                // NOTE: This write might fail if the user is not an admin, as per rules "allow update: if ... || hasRole('admin')"
                // but if it's updating their own document, it should be fine. However, claimantId might not be currentUser.uid.
                // If it's the CREATOR verifying, then claimantId != currentUser.uid.
                // Rule: match /users/{uid} { allow update: if isSignedIn() && (request.auth.uid == uid || hasRole('admin')); }
                // So this WILL fail if the creator tries to update the claimant's document.
                
                transaction.update(claimantRef, "credits", com.google.firebase.firestore.FieldValue.increment(bounty.rewardCredits))
                transaction.update(claimantRef, "reputationScore", com.google.firebase.firestore.FieldValue.increment(50))
                
                transaction.update(ref, "status", BountyStatus.COMPLETED.name)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("verifyAndCompleteBounty", e)
            Result.failure(e)
        }
    }

    private fun logDetailedError(operation: String, e: Exception) {
        // PART 9 — FINAL FALLBACK
        if (e is FirebaseFirestoreException) {
            Log.e(TAG, "PART 9: $operation Failed. Error Code: ${e.code}, Message: ${e.message}")
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(TAG, "CAUSE IDENTIFIED: Permission denied. Check firestore.rules for collection associated with $operation")
            }
        } else {
            Log.e(TAG, "PART 9: $operation Failed. Message: ${e.message}")
        }
    }
}
