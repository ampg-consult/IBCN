package com.ampgconsult.ibcn.data.local.dao

import androidx.room.*
import com.ampgconsult.ibcn.data.local.entities.ChatMessageEntity
import com.ampgconsult.ibcn.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun getProjectByName(name: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM chat_history WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getChatHistory(projectId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)
}
