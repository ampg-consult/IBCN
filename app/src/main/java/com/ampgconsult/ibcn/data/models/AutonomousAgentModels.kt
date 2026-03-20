package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class AutonomousPlan(
    val id: String = "",
    val projectId: String = "",
    val userId: String = "",
    val description: String = "",
    val steps: List<AutonomousStep> = emptyList(),
    val status: AutonomousStatus = AutonomousStatus.IDLE,
    val currentStepIndex: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null
)

data class AutonomousStep(
    val id: String = "",
    val action: AgentActionType = AgentActionType.CREATE_FILE,
    val filePath: String = "",
    val content: String = "",
    val command: String = "",
    val description: String = "",
    val status: StepStatus = StepStatus.PENDING,
    val logs: String = ""
)

enum class AgentActionType {
    CREATE_FILE, UPDATE_FILE, DELETE_FILE, RUN_COMMAND, INSTALL_PACKAGE, CONNECT_API
}

enum class AutonomousStatus {
    IDLE, PLANNING, EXECUTING, VALIDATING, COMPLETED, FAILED
}

enum class StepStatus {
    PENDING, IN_PROGRESS, SUCCESS, FAILED
}

data class AutonomousLog(
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType {
    INFO, SUCCESS, ERROR, WARNING, AI_THOUGHT
}
