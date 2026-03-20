package com.ampgconsult.ibcn.modules.freelancer_hub.models

import com.google.firebase.Timestamp

data class Client(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val company: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)

data class Invoice(
    val id: String = "",
    val clientId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val items: List<InvoiceItem> = emptyList(),
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
)

data class InvoiceItem(
    val description: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0
)

enum class InvoiceStatus {
    DRAFT, SENT, PAID, OVERDUE, CANCELLED
}

data class Proposal(
    val id: String = "",
    val clientId: String = "",
    val title: String = "",
    val description: String = "",
    val budget: Double = 0.0,
    val status: ProposalStatus = ProposalStatus.DRAFT,
    val createdAt: Timestamp = Timestamp.now()
)

enum class ProposalStatus {
    DRAFT, SENT, ACCEPTED, REJECTED
}
