package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class Project(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerUid: String = "",
    val status: String = "DRAFT",
    val progress: Float = 0f,
    val createdAt: Timestamp = Timestamp.now(),
    val collaborators: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)
