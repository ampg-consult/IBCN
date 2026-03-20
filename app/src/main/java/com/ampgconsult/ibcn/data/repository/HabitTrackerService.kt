package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitTrackerService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService,
    private val reputationService: ReputationService
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getRecordId(userId: String, projectId: String, date: Date): String {
        return "${userId}_${projectId}_${dateFormat.format(date)}"
    }

    suspend fun getDailyHabitRecord(projectId: String, date: Date = Date()): DailyHabitRecord? {
        val userId = auth.currentUser?.uid ?: return null
        val id = getRecordId(userId, projectId, date)
        
        return try {
            val doc = firestore.collection("habit_tracker").document(id).get().await()
            if (doc.exists()) {
                doc.toObject(DailyHabitRecord::class.java)
            } else {
                initializeDailyRecord(projectId, date)
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun initializeDailyRecord(projectId: String, date: Date): DailyHabitRecord? {
        val userId = auth.currentUser?.uid ?: return null
        val id = getRecordId(userId, projectId, date)
        
        val projectDoc = firestore.collection("projects").document(projectId).get().await()
        val project = projectDoc.toObject(Project::class.java) ?: return null

        val tasks = generateAITasks(project)
        
        val yesterday = Calendar.getInstance().apply {
            time = date
            add(Calendar.DATE, -1)
        }.time
        val prevId = getRecordId(userId, projectId, yesterday)
        val prevDoc = firestore.collection("habit_tracker").document(prevId).get().await()
        val currentStreak = prevDoc.getLong("streakCount")?.toInt() ?: 0

        val newRecord = DailyHabitRecord(
            id = id,
            userId = userId,
            projectId = projectId,
            date = Timestamp(date),
            tasks = tasks,
            streakCount = currentStreak,
            totalPoints = 0,
            lastUpdated = Timestamp.now()
        )

        firestore.collection("habit_tracker").document(id).set(newRecord).await()
        return newRecord
    }

    private suspend fun generateAITasks(project: Project): List<HabitTask> {
        val prompt = "Based on the project '${project.name}' described as '${project.description}', suggest 3 specific, actionable daily micro-tasks for a developer to complete today. Format each task as a short sentence. Respond only with the list, one per line."
        
        val aiResponse = aiService.getResponse(prompt, AgentType.HABIT_GENERATOR)
        val taskLines = aiResponse.getOrNull()?.lines()?.filter { it.isNotBlank() } ?: listOf(
            "Update project documentation",
            "Review recent code changes",
            "Plan next development milestone"
        )

        return taskLines.take(3).map { description ->
            HabitTask(
                taskId = UUID.randomUUID().toString(),
                description = description.trim().removePrefix("- ").removePrefix("1. "),
                status = HabitTaskStatus.PENDING,
                points = 10
            )
        }
    }

    suspend fun saveDailyTasks(projectId: String, tasks: List<HabitTask>): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val date = Date()
        val id = getRecordId(userId, projectId, date)
        
        val record = DailyHabitRecord(
            id = id,
            userId = userId,
            projectId = projectId,
            date = Timestamp(date),
            tasks = tasks,
            lastUpdated = Timestamp.now()
        )
        
        return try {
            firestore.collection("habit_tracker").document(id).set(record, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(projectId: String, taskId: String, newStatus: HabitTaskStatus): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val date = Date()
        val id = getRecordId(userId, projectId, date)

        return try {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection("habit_tracker").document(id)
                val snapshot = transaction.get(ref)
                val record = snapshot.toObject(DailyHabitRecord::class.java) ?: throw Exception("Record not found")

                val updatedTasks = record.tasks.map {
                    if (it.taskId == taskId) it.copy(status = newStatus) else it
                }

                val pointsEarned = if (newStatus == HabitTaskStatus.COMPLETED) 10 else 0
                val totalPoints = record.totalPoints + pointsEarned

                val allCompleted = updatedTasks.all { it.status == HabitTaskStatus.COMPLETED }
                var newStreak = record.streakCount
                if (allCompleted && record.tasks.any { it.status != HabitTaskStatus.COMPLETED }) {
                    newStreak += 1
                }

                transaction.update(ref, mapOf(
                    "tasks" to updatedTasks,
                    "totalPoints" to totalPoints,
                    "streakCount" to newStreak,
                    "lastUpdated" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
