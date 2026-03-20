package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName") var senderName: String = "",
    @get:PropertyName("sender") @set:PropertyName("sender") var sender: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var text: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("projectId") @set:PropertyName("projectId") var projectId: String? = null,
    @get:PropertyName("isUser") @set:PropertyName("isUser") var isUser: Boolean = false,
    @get:PropertyName("isAI") @set:PropertyName("isAI") var isAI: Boolean = false
)
