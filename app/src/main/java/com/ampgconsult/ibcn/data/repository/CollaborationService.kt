package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class CollaborationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) {
    private val TAG = "CollaborationService"

    private suspend fun ensureAuthReady() {
        Log.d(TAG, "PART 1: Verifying Auth State. Current UID: ${auth.currentUser?.uid}")
        var user = auth.currentUser
        var retries = 0
        while (user == null && retries < 50) { 
            delay(100)
            user = auth.currentUser
            retries++
        }
        
        user?.reload()?.await()
        Log.d(TAG, "Auth ready. UID: ${auth.currentUser?.uid}")
        
        if (auth.currentUser == null) {
            throw Exception("NOT AUTHENTICATED")
        }
    }

    private fun verifyFirebaseProject() {
        val appProjectId = auth.app.options.projectId
        val firestoreProjectId = firestore.app.options.projectId
        Log.d(TAG, "PART 2: Firebase App Project: $appProjectId")
        Log.d(TAG, "PART 2: Firestore Project: $firestoreProjectId")
        if (appProjectId != firestoreProjectId) {
            Log.e(TAG, "ERROR: WRONG FIREBASE PROJECT CONNECTED")
            throw Exception("WRONG FIREBASE PROJECT CONNECTED")
        }
    }

    fun getProjectMembers(projectId: String): Flow<List<ProjectMember>> = callbackFlow {
        val subscription = firestore.collection("project_members")
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(ProjectMember::class.java) ?: emptyList()
                trySend(members)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addMember(projectId: String, uid: String, role: String): Result<Unit> {
        return try {
            ensureAuthReady()
            verifyFirebaseProject()
            
            val userDoc = firestore.collection("users").document(uid).get().await()
            val member = ProjectMember(
                id = "${projectId}_${uid}",
                projectId = projectId,
                uid = uid,
                username = userDoc.getString("username") ?: "@Builder",
                displayName = userDoc.getString("displayName") ?: "Builder",
                role = role
            )
            firestore.collection("project_members").document(member.id).set(member).await()
            logActivity(projectId, "Joined the team", uid, member.displayName)
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("addMember", e)
            Result.failure(e)
        }
    }

    suspend fun sendInvite(projectId: String, invitedUserId: String): Result<Unit> {
        return try {
            ensureAuthReady()
            verifyFirebaseProject()
            
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            val invite = ProjectInvite(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                projectName = projectDoc.getString("name") ?: "Project",
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "Owner",
                invitedUserId = invitedUserId,
                status = "pending"
            )
            firestore.collection("project_invites").document(invite.id).set(invite).await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("sendInvite", e)
            Result.failure(e)
        }
    }

    fun getMyInvites(): Flow<List<ProjectInvite>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = firestore.collection("project_invites")
            .whereEqualTo("invitedUserId", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val invites = snapshot?.toObjects(ProjectInvite::class.java) ?: emptyList()
                trySend(invites)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun respondToInvite(inviteId: String, accept: Boolean): Result<Unit> {
        return try {
            ensureAuthReady()
            val inviteDoc = firestore.collection("project_invites").document(inviteId).get().await()
            val invite = inviteDoc.toObject(ProjectInvite::class.java) ?: throw Exception("Invite not found")
            
            if (accept) {
                firestore.collection("project_invites").document(inviteId).update("status", "accepted").await()
                addMember(invite.projectId, invite.invitedUserId, ProjectRole.DEVELOPER.value)
            } else {
                firestore.collection("project_invites").document(inviteId).update("status", "declined").await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("respondToInvite", e)
            Result.failure(e)
        }
    }

    fun getProjectMessages(projectId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = firestore.collection("project_messages")
            .whereEqualTo("projectId", projectId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(projectId: String, text: String): Result<Unit> {
        return try {
            ensureAuthReady()
            val currentUser = auth.currentUser ?: throw Exception("Not authenticated")
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "@Builder",
                text = text,
                timestamp = Timestamp.now()
            )
            firestore.collection("project_messages").document(message.id).set(
                mapOf(
                    "id" to message.id,
                    "projectId" to projectId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "text" to message.text,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            logDetailedError("sendMessage", e)
            Result.failure(e)
        }
    }

    private suspend fun logActivity(projectId: String, action: String, userId: String, userName: String) {
        val activity = ProjectActivity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            action = action,
            userId = userId,
            userName = userName
        )
        firestore.collection("project_activity").document(activity.id).set(activity).await()
    }

    fun getActivityFeed(projectId: String): Flow<List<ProjectActivity>> = callbackFlow {
        val subscription = firestore.collection("project_activity")
            .whereEqualTo("projectId", projectId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                val activities = snapshot?.toObjects(ProjectActivity::class.java) ?: emptyList()
                trySend(activities)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getAiTaskSuggestions(projectId: String): Result<List<String>> {
        val projectDoc = firestore.collection("projects").document(projectId).get().await()
        val desc = projectDoc.getString("description") ?: ""
        
        val prompt = """
            Act as an AI Collaboration Assistant. 
            Analyze this project description: $desc.
            Suggest 5 critical micro-tasks for a team of developers to collaborate on.
            Return ONLY a comma-separated list of tasks.
        """.trimIndent()

        return aiService.getResponse(prompt, AgentType.PRODUCT_MANAGER).map { response ->
            response.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    private fun logDetailedError(operation: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e(TAG, "Firestore Error in $operation: Code: ${e.code}, Message: ${e.message}")
        } else {
            Log.e(TAG, "Error in $operation: Message: ${e.message}")
        }
    }
}
