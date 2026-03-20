package com.ampgconsult.ibcn.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeploymentService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val fileService: ProjectFileService
) {
    private val TAG = "DeploymentService"
    private val client = OkHttpClient()
    
    // This would be the Railway URL once deployed
    private val RAILWAY_BUILD_ENGINE_URL = "https://deploy.ibcn.site/build" 

    suspend fun deployProject(projectId: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            // 1. Update status to BUILDING
            firestore.collection("projects").document(projectId)
                .update("status", "BUILDING", "deploymentProgress", 0.1).await()

            // 2. Gather all project files
            val files = fileService.getProjectFiles(projectId)
            val filesJson = JSONObject()
            files.forEach { file ->
                filesJson.put(file.fileName, file.content)
            }

            // 3. Prepare payload for Railway Deploy Engine
            val payload = JSONObject().apply {
                put("projectId", projectId)
                put("ownerId", uid)
                put("files", filesJson)
                put("platform", "web") // Targeting ibcn.site/apps/{id}
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(RAILWAY_BUILD_ENGINE_URL)
                .post(body)
                .build()

            // 4. Trigger build
            // Use withContext(Dispatchers.IO) for blocking OkHttp calls
            val responseText: String = withContext(Dispatchers.IO) {
                val response: Response = client.newCall(request).execute()
                response.use { r ->
                    if (!r.isSuccessful) throw Exception("Railway build trigger failed: ${r.message}")
                    r.body?.string() ?: throw Exception("Empty response from build engine")
                }
            }

            val responseJson = JSONObject(responseText)
            val buildId = responseJson.optString("buildId", "unknown")
            val liveUrl = "https://ibcn.site/apps/$projectId"

            // 5. Update Firestore with build info
            firestore.collection("projects").document(projectId)
                .update(
                    "status", "DEPLOYED",
                    "liveUrl", liveUrl,
                    "lastBuildId", buildId,
                    "deploymentProgress", 1.0,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()

            // 6. Link to SaaS Engine automatically
            autoLinkToSaaS(projectId, liveUrl)

            Result.success(liveUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Deployment failed", e)
            try {
                firestore.collection("projects").document(projectId)
                    .update("status", "FAILED", "error", e.message).await()
            } catch (firestoreEx: Exception) {
                Log.e(TAG, "Failed to update project status to FAILED", firestoreEx)
            }
            Result.failure(e)
        }
    }

    private suspend fun autoLinkToSaaS(projectId: String, liveUrl: String) {
        try {
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            val name = projectDoc.getString("name") ?: "Project $projectId"
            val desc = projectDoc.getString("description") ?: "AI Generated SaaS"
            
            val saasData = mapOf(
                "id" to projectId,
                "projectId" to projectId,
                "ownerId" to auth.currentUser?.uid,
                "name" to name,
                "description" to desc,
                "liveUrl" to liveUrl,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("saas_products").document(projectId).set(saasData).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-link SaaS", e)
        }
    }
}
