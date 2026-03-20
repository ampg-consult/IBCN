package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

enum class AgreementType {
    INVESTMENT, COFOUNDER, NDA, SERVICE_CONTRACT
}

data class LegalAgreement(
    val id: String = "",
    val type: AgreementType = AgreementType.NDA,
    val parties: List<String> = emptyList(), // User IDs
    val content: String = "",
    val terms: Map<String, String> = emptyMap(), // Dynamic terms like equity, amount, roles
    val status: String = "pending", // "pending", "signed", "voided"
    val signatures: Map<String, Signature> = emptyMap(),
    val createdAt: Timestamp = Timestamp.now(),
    val signedAt: Timestamp? = null,
    val disclaimer: String = "This document is AI-generated and should be reviewed by a qualified legal professional."
)

data class Signature(
    val userId: String = "",
    val name: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val ipAddress: String = "",
    val publicKey: String = ""
)
