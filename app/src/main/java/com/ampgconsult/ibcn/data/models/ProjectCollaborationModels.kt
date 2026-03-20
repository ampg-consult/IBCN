package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class ProjectMember(
    val id: String = "",
    val projectId: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val role: String = "developer", // "owner", "developer", "viewer"
    val joinedAt: Timestamp = Timestamp.now()
)

data class ProjectInvite(
    val id: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val invitedUserId: String = "",
    val status: String = "pending", // "pending", "accepted", "declined"
    val createdAt: Timestamp = Timestamp.now()
)

data class ProjectActivity(
    val id: String = "",
    val projectId: String = "",
    val action: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

enum class ProjectRole(val value: String) {
    OWNER("owner"),
    DEVELOPER("developer"),
    VIEWER("viewer")
}
