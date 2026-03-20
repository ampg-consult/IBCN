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
            firestore.collection("projects").document(projectId)
                .collection("files").document(fileId)
                .set(projectFile).await()
            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectFiles(projectId: String): List<ProjectFile> {
        return try {
            firestore.collection("projects").document(projectId)
                .collection("files")
                .get().await()
                .toObjects(ProjectFile::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFileContent(projectId: String, fileId: String, newContent: String): Result<Unit> {
        return try {
            firestore.collection("projects").document(projectId)
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
    
    suspend fun getFile(projectId: String, fileId: String): ProjectFile? {
        return try {
            firestore.collection("projects").document(projectId)
                .collection("files").document(fileId)
                .get().await()
                .toObject(ProjectFile::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
