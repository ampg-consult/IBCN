package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class StartupProject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val aiScore: Int = 0,
    val marketPotential: String = "Medium", // Low, Medium, High, Very High
    val creatorId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class InvestorInsight(
    val title: String = "",
    val content: String = ""
)
