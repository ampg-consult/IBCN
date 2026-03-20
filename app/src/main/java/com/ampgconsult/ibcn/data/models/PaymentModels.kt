package com.ampgconsult.ibcn.data.models

import com.google.firebase.Timestamp

data class Wallet(
    val uid: String = "",
    val balance: Double = 0.0,
    val currency: String = "USD",
    val totalEarnings: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)

data class Payout(
    val id: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val status: String = "pending", // "pending", "completed", "failed"
    val method: String = "bank",
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null,
    val bankAccount: String = "",
    val bankName: String = ""
)

data class Transaction(
    val id: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val type: String = "credit", // "credit" (earnings), "debit" (purchase/withdrawal)
    val description: String = "",
    val reference: String = "",
    val provider: String = "internal",
    val createdAt: Timestamp = Timestamp.now()
)

sealed class PaymentProvider {
    object Stripe : PaymentProvider()
    object Paystack : PaymentProvider()
}
