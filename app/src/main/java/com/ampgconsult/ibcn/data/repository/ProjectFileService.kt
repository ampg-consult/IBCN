package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.ProjectFile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectFileService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * MODULE 2: Save to specific production path: /users/{userId}/projects/{projectId}/files
     */
    suspend fun uploadFile(projectId: String, fileName: String, content: String, path: String = ""): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        val fileId = java.util.UUID.randomUUID().toString()
        val language = when {
            fileName.endsWith(".dart") -> "dart"
            fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> "yaml"
            fileName.endsWith(".json") -> "json"
            fileName.endsWith(".kt") -> "kotlin"
            else -> "text"
        }

        val projectFile = ProjectFile(
            id = fileId,
            projectId = projectId,
            fileName = fileName,
            path = path,
            content = content,
            language = language,
            updatedAt = Timestamp.now()
        )

        return try {
            firestore.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("files").document(fileId)
                .set(projectFile).await()
            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectFiles(projectId: String): List<ProjectFile> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            firestore.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("files")
                .get().await()
                .toObjects(ProjectFile::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFileContent(projectId: String, fileId: String, newContent: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestore.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("files").document(fileId)
                .update(mapOf(
                    "content" to newContent,
                    "updatedAt" to Timestamp.now()
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
