package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.AgreementType
import com.ampgconsult.ibcn.data.models.LegalAgreement
import com.ampgconsult.ibcn.data.models.Signature
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class LegalService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService
) {
    /**
     * Generate a legal agreement using AI.
     */
    suspend fun generateAgreement(
        type: AgreementType,
        parties: List<String>,
        terms: Map<String, String>
    ): Result<LegalAgreement> {
        val prompt = """
            Act as a Senior Legal Counsel. Generate a professional ${type.name} agreement.
            Parties involved: ${parties.joinToString(", ")}.
            Specific terms: ${terms.entries.joinToString("; ") { "${it.key}: ${it.value}" }}.
            Include sections for Definitions, Obligations, Confidentiality, Termination, and Governing Law.
            Format the output as a clean document.
            Include this disclaimer at the bottom: "This document is AI-generated and should be reviewed by a qualified legal professional."
        """.trimIndent()

        return try {
            val contentResult = aiService.getResponse(prompt, AgentType.DOC_GENERATOR)
            if (contentResult.isFailure) return Result.failure(contentResult.exceptionOrNull()!!)

            val agreement = LegalAgreement(
                id = UUID.randomUUID().toString(),
                type = type,
                parties = parties,
                content = contentResult.getOrThrow(),
                terms = terms,
                createdAt = Timestamp.now()
            )

            firestore.collection("legal_agreements").document(agreement.id).set(agreement).await()
            Result.success(agreement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Digitally sign an agreement.
     */
    suspend fun signAgreement(agreementId: String, name: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val signature = Signature(
                userId = uid,
                name = name,
                timestamp = Timestamp.now(),
                ipAddress = "0.0.0.0", // In production, capture real IP
                publicKey = UUID.randomUUID().toString() // Simplified digital signature
            )

            val agreementRef = firestore.collection("legal_agreements").document(agreementId)
            val agreement = agreementRef.get().await().toObject(LegalAgreement::class.java) 
                ?: return Result.failure(Exception("Agreement not found"))

            val updatedSignatures = agreement.signatures.toMutableMap()
            updatedSignatures[uid] = signature

            val allSigned = agreement.parties.all { updatedSignatures.containsKey(it) }
            
            val updateData = mutableMapOf<String, Any>(
                "signatures" to updatedSignatures
            )
            
            if (allSigned) {
                updateData["status"] = "signed"
                updateData["signedAt"] = Timestamp.now()
            }

            agreementRef.update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAgreement(agreementId: String): LegalAgreement? {
        return try {
            firestore.collection("legal_agreements").document(agreementId).get().await()
                .toObject(LegalAgreement::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
