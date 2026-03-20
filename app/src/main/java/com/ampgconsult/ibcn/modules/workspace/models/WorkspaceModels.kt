package com.ampgconsult.ibcn.modules.workspace.models

import com.google.firebase.Timestamp

data class WorkspaceProject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUid: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class KanbanTask(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assigneeUid: String? = null,
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
)

enum class TaskStatus {
    TODO, IN_PROGRESS, REVIEW, DONE
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class ProjectDocument(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val content: String = "",
    val authorUid: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
