package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class HabitTask(
    val taskId: String = "",
    val description: String = "",
    val status: HabitTaskStatus = HabitTaskStatus.PENDING,
    val points: Int = 10
)

enum class HabitTaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, SKIPPED
}

data class DailyHabitRecord(
    val id: String = "", // userId_projectId_date
    val userId: String = "",
    val projectId: String = "",
    val date: Timestamp = Timestamp.now(),
    val tasks: List<HabitTask> = emptyList(),
    val streakCount: Int = 0,
    val totalPoints: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
)
