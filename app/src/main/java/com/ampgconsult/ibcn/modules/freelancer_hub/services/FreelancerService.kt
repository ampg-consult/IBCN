package com.ampgconsult.ibcn.modules.freelancer_hub.services

import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.modules.freelancer_hub.models.Client
import com.ampgconsult.ibcn.modules.freelancer_hub.models.Invoice
import com.ampgconsult.ibcn.modules.freelancer_hub.models.Proposal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreelancerService @Inject constructor(
    private val aiService: AIService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun generateProposal(clientId: String, projectDescription: String): Result<Proposal> {
        val response = aiService.getResponse("Generate a professional freelance proposal for this project: $projectDescription", AgentType.PRODUCT_MANAGER)
        return response.fold(
            onSuccess = { content ->
                val proposal = Proposal(
                    id = java.util.UUID.randomUUID().toString(),
                    clientId = clientId,
                    title = "Proposal for $projectDescription",
                    description = content
                )
                saveProposal(proposal)
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun saveProposal(proposal: Proposal): Result<Proposal> {
        return try {
            firestore.collection("freelancer_proposals").document(proposal.id).set(proposal).await()
            Result.success(proposal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInvoice(invoice: Invoice): Result<Unit> {
        return try {
            firestore.collection("freelancer_invoices").document(invoice.id).set(invoice).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addClient(client: Client): Result<Unit> {
        return try {
            firestore.collection("freelancer_clients").document(client.id).set(client).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
