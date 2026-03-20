package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.devops.AutoFirebaseEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsernameService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService,
    private val autoFirebase: AutoFirebaseEngine
) {
    private val TAG = "UsernameService"
    
    private val reservedUsernames = setOf("admin", "root", "system", "support", "ibcn")
    private val usernameRegex = Regex("^@[A-Za-z0-9_]{3,20}$")

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validateUsername(username: String): ValidationResult {
        if (!usernameRegex.matches(username)) {
            return ValidationResult.Invalid("Must start with @, 3-20 chars, letters/numbers/underscores only")
        }
        
        val normalized = normalize(username)
        if (reservedUsernames.contains(normalized)) {
            return ValidationResult.Invalid("This username is reserved")
        }
        
        return ValidationResult.Valid
    }

    fun normalize(username: String): String {
        return username.lowercase().replace("@", "")
    }

    suspend fun ensureAuthReady(): FirebaseUser? {
        var user = auth.currentUser
        var retries = 0
        while (user == null && retries < 50) { 
            delay(100)
            user = auth.currentUser
            retries++
        }
        user?.reload()?.await()
        return auth.currentUser
    }

    suspend fun checkAvailability(usernameLower: String): Boolean {
        return try {
            val result = firestore.collection("usernames").document(usernameLower).get().await()
            !result.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateSuggestions(nameOrEmail: String): List<String> {
        val base = nameOrEmail.split("@").first().replace(Regex("[^A-Za-z0-9_]"), "")
        val options = listOf("@$base", "@${base}Dev", "@${base}_01", "@${base}_99", "@${base}_x")
        
        return options.filter { 
            validateUsername(it) is ValidationResult.Valid && 
            checkAvailability(normalize(it)) 
        }.take(3)
    }

    suspend fun generateEliteSuggestions(displayName: String): List<String> {
        val prompt = "Act as an AI Username Assistant. Generate 10 unique, professional and catchy suggestions based on: $displayName. Return ONLY a comma-separated list of suggestions without quotes."
        
        val aiResult = aiService.getResponse(prompt, AgentType.USERNAME_ASSISTANT)
        if (aiResult.isSuccess) {
            val suggestions = aiResult.getOrNull()?.split(",")?.map { "@${it.trim().removePrefix("@").replace("\"", "")}" }
                ?.filter { validateUsername(it) is ValidationResult.Valid } ?: emptyList()
            val available = suggestions.filter { checkAvailability(normalize(it)) }
            if (available.size >= 3) return available.take(10)
        }
        return generateSuggestions(displayName)
    }

    suspend fun reserveUsernameAtomic(
        displayUsername: String, 
        firebaseUser: FirebaseUser,
        customDisplayName: String? = null
    ): Result<String> {
        var currentAttempt = 0
        var currentDisplay = displayUsername
        val suffixes = listOf("_01", "_99", "_x", "_ibcn", "_builder")
        
        while (currentAttempt < 6) {
            val usernameLower = normalize(currentDisplay)
            
            val result = try {
                firestore.runTransaction { transaction ->
                    val usernameRef = firestore.collection("usernames").document(usernameLower)
                    if (transaction.get(usernameRef).exists()) {
                        throw Exception("Username taken")
                    }

                    val userRef = firestore.collection("users").document(firebaseUser.uid)
                    val photo = firebaseUser.photoUrl?.toString() ?: ""
                    val finalDisplayName = customDisplayName ?: firebaseUser.displayName ?: "Builder"
                    
                    val userData = mutableMapOf<String, Any>(
                        "uid" to firebaseUser.uid,
                        "username" to currentDisplay,
                        "username_lower" to usernameLower,
                        "displayName" to finalDisplayName,
                        "photoURL" to photo,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    transaction.set(usernameRef, mapOf(
                        "owner" to firebaseUser.uid, 
                        "uid" to firebaseUser.uid,
                        "display" to currentDisplay, 
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                    
                    transaction.set(userRef, userData, SetOptions.merge())
                    
                    transaction.set(firestore.collection("user_public").document(firebaseUser.uid), 
                        mapOf(
                            "uid" to firebaseUser.uid, 
                            "username" to currentDisplay, 
                            "username_lower" to usernameLower, 
                            "displayName" to finalDisplayName, 
                            "avatarUrl" to photo
                        ), SetOptions.merge())
                }.await()
                Result.success(currentDisplay)
            } catch (e: Exception) {
                if (e.message == "Username taken") {
                    if (currentAttempt < suffixes.size) {
                        currentDisplay = "${displayUsername}${suffixes[currentAttempt]}"
                        currentAttempt++
                        null 
                    } else {
                        Result.failure(Exception("All suffix attempts failed"))
                    }
                } else {
                    Result.failure(e)
                }
            }
            if (result != null) return result
        }
        return Result.failure(Exception("Failed to reserve unique username"))
    }

    suspend fun autoGenerateAndReserveForGoogle(firebaseUser: FirebaseUser): String {
        delay(800) 
        ensureAuthReady()
        
        val cleanName = (firebaseUser.displayName ?: "Builder")
            .replace(Regex("[^A-Za-z0-9_]"), "")
            .take(15)
        
        val baseUsername = "@$cleanName"
        return reserveUsernameAtomic(baseUsername, firebaseUser).getOrThrow()
    }

    suspend fun reserveUsername(displayUsername: String, displayName: String? = null): Result<Unit> {
        val user = ensureAuthReady() ?: return Result.failure(Exception("Not authenticated"))
        return reserveUsernameAtomic(displayUsername, user, displayName).map { Unit }
    }
}
