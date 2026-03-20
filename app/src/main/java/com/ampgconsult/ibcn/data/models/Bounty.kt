package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class Bounty(
    val id: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val requirements: String = "",
    val rewardCredits: Double = 0.0,
    val status: BountyStatus = BountyStatus.OPEN,
    val claimantId: String? = null,
    val submissionUrl: String? = null,
    val aiEvaluationScore: Int? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val deadline: Timestamp? = null
)

enum class BountyStatus {
    OPEN, CLAIMED, SUBMITTED, COMPLETED, CANCELLED
}
