package com.ampgconsult.ibcn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val status: String,
    val progress: Float,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_history")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: String,
    val sender: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
