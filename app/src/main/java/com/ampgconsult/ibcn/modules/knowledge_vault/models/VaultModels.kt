package com.ampgconsult.ibcn.modules.knowledge_vault.models

import com.google.firebase.Timestamp

data class VaultDocument(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val authorUid: String = "",
    val embedding: List<Double>? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class VaultNote(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val authorUid: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
