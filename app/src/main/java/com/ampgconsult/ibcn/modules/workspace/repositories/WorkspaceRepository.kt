package com.ampgconsult.ibcn.modules.workspace.repositories

import com.ampgconsult.ibcn.modules.workspace.models.KanbanTask
import com.ampgconsult.ibcn.modules.workspace.models.ProjectDocument
import com.ampgconsult.ibcn.modules.workspace.models.WorkspaceProject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getWorkspaceProject(projectId: String): Result<WorkspaceProject> {
        return try {
            val doc = firestore.collection("workspace_projects").document(projectId).get().await()
            val project = doc.toObject(WorkspaceProject::class.java) ?: throw Exception("Project not found")
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasks(projectId: String): Result<List<KanbanTask>> {
        return try {
            val snapshot = firestore.collection("workspace_projects").document(projectId)
                .collection("tasks").get().await()
            val tasks = snapshot.toObjects(KanbanTask::class.java)
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(task: KanbanTask): Result<Unit> {
        return try {
            firestore.collection("workspace_projects").document(task.projectId)
                .collection("tasks").document(task.id).set(task).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocuments(projectId: String): Result<List<ProjectDocument>> {
        return try {
            val snapshot = firestore.collection("workspace_projects").document(projectId)
                .collection("documents").get().await()
            val docs = snapshot.toObjects(ProjectDocument::class.java)
            Result.success(docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
