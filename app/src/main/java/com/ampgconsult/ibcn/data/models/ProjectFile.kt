package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class ProjectFile(
    val id: String = "",
    val projectId: String = "",
    val fileName: String = "",
    val path: String = "",
    val content: String = "",
    val language: String = "dart",
    val updatedAt: Timestamp = Timestamp.now()
)
