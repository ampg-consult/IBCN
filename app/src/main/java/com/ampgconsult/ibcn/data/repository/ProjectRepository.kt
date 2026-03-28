package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.local.dao.ProjectDao
import com.ampgconsult.ibcn.data.local.entities.ChatMessageEntity
import com.ampgconsult.ibcn.data.local.entities.ProjectEntity
import com.ampgconsult.ibcn.data.models.Project
import com.ampgconsult.ibcn.data.models.ChatMessage
import com.ampgconsult.ibcn.data.network.ApiService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val apiService: ApiService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toDomainModel() }
    }

    fun getProject(id: String): Flow<Project?> = projectDao.getProjectById(id).map { entity ->
        entity?.toDomainModel()
    }

    suspend fun getProjectById(id: String): Result<Project> {
        return try {
            val project = projectDao.getProjectById(id).map { it?.toDomainModel() }.firstOrNull()
            if (project != null) Result.success(project) else Result.failure(Exception("Project not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshProjects() {
        try {
            val remoteProjects = apiService.getProjects()
            remoteProjects.forEach { project ->
                projectDao.insertProject(project.toEntity())
            }
        } catch (e: Exception) {
        }
    }

    suspend fun createProject(name: String, description: String): String {
        val uid = auth.currentUser?.uid ?: "anonymous"
        val projectId = java.util.UUID.randomUUID().toString()
        val newProject = Project(
            id = projectId,
            name = name,
            description = description,
            ownerUid = uid,
            status = "DRAFT",
            progress = 0f,
            createdAt = Timestamp.now()
        )
        
        // Save to Local DB
        projectDao.insertProject(newProject.toEntity())
        
        // MODULE 1: Save to Production Firestore path: /projects/{projectId}
        try {
            firestore.collection("projects").document(projectId).set(newProject).await()
            
            // Also save to user-specific subcollection for Module 2 compliance
            firestore.collection("users").document(uid).collection("projects").document(projectId).set(newProject).await()
            
            apiService.createProject(newProject)
        } catch (e: Exception) {
        }
        return projectId
    }

    fun getChatHistory(projectId: String): Flow<List<ChatMessage>> {
        return projectDao.getChatHistory(projectId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun saveChatMessage(projectId: String, sender: String, message: String, isUser: Boolean) {
        val chatEntity = ChatMessageEntity(
            projectId = projectId,
            sender = sender,
            message = message,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        )
        projectDao.insertChatMessage(chatEntity)
    }

    private fun ProjectEntity.toDomainModel() = Project(
        id = id,
        name = name,
        description = description,
        ownerUid = "unknown", 
        status = status,
        progress = progress,
        createdAt = Timestamp(Date(createdAt))
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id,
        name = name,
        description = description,
        status = status,
        progress = progress,
        createdAt = createdAt.toDate().time
    )

    private fun ChatMessageEntity.toDomainModel() = ChatMessage(
        id = id.toString(),
        senderName = sender,
        text = message,
        isUser = isUser,
        timestamp = Timestamp(Date(timestamp))
    )
}
