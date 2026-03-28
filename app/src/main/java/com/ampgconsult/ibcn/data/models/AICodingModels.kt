package com.ampgconsult.ibcn.data.models

data class AICodingMessage(val role: String, val content: String)

data class AICodingUiState(
    val messages: List<AICodingMessage> = emptyList(),
    val userInput: String = "",
    val isTyping: Boolean = false,
    val error: String? = null,
    val projectFiles: List<ProjectFile> = emptyList(),
    val deployedUrl: String? = null
)
