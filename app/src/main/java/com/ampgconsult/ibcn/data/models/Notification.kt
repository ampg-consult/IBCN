package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info", // info, message, alert
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)
