package com.ampgconsult.ibcn.data.repository

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

@Singleton
class InvestorService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val aiService: AIService,
    private val legalService: LegalService,
    private val paymentManager: PaymentManager
) {
    /**
     * Create a startup profile and generate AI score/pitch deck content.
     */
    suspend fun createStartupProfile(
        name: String,
        description: String,
        industry: String,
        fundingSought: Double,
        equityOffered: Double
    ): Result<StartupProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val prompt = "Act as a Venture Capitalist. Score this startup (0-100) and generate a short pitch deck outline. Name: $name, Description: $description, Industry: $industry."
            val aiResult = aiService.getResponse(prompt, AgentType.ANALYTICS_LAB)
            
            // Simplified score extraction
            val scoreMatch = Regex("Score:? (\\d+)").find(aiResult.getOrDefault(""))
            val aiScore = scoreMatch?.groupValues?.get(1)?.toIntOrNull() ?: 75

            val startup = StartupProfile(
                id = UUID.randomUUID().toString(),
                ownerId = uid,
                name = name,
                description = description,
                industry = industry,
                fundingSought = fundingSought,
                equityOffered = equityOffered,
                aiScore = aiScore,
                createdAt = Timestamp.now()
            )

            firestore.collection("startups").document(startup.id).set(startup).await()
            Result.success(startup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initiate investment and create escrow.
     */
    suspend fun initiateInvestment(
        startupId: String,
        amount: Double,
        equity: Double
    ): Result<InvestmentRound> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val roundId = UUID.randomUUID().toString()
            val escrowId = UUID.randomUUID().toString()
            
            val round = InvestmentRound(
                id = roundId,
                startupId = startupId,
                investorId = uid,
                amount = amount,
                equity = equity,
                status = "escrow",
                escrowId = escrowId,
                createdAt = Timestamp.now()
            )

            val escrow = EscrowAccount(
                id = escrowId,
                investmentId = roundId,
                totalAmount = amount,
                status = "active",
                createdAt = Timestamp.now()
            )

            firestore.runTransaction { transaction ->
                transaction.set(firestore.collection("investments").document(roundId), round)
                transaction.set(firestore.collection("escrows").document(escrowId), escrow)
            }.await()

            // Automatically generate Investment Agreement
            val startup = firestore.collection("startups").document(startupId).get().await()
                .toObject(StartupProfile::class.java)
            
            if (startup != null) {
                legalService.generateAgreement(
                    type = AgreementType.INVESTMENT,
                    parties = listOf(uid, startup.ownerId),
                    terms = mapOf(
                        "amount" to "$$amount",
                        "equity" to "$equity%",
                        "startup" to startup.name
                    )
                )
            }

            Result.success(round)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Release milestone funds from escrow.
     */
    suspend fun releaseMilestone(escrowId: String, milestoneId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val escrowRef = firestore.collection("escrows").document(escrowId)
                val escrow = transaction.get(escrowRef).toObject(EscrowAccount::class.java) ?: throw Exception("Escrow not found")
                
                // Find and update milestone
                val updatedMilestones = escrow.milestones.map { 
                    if (it.id == milestoneId) it.copy(status = "released", completedAt = Timestamp.now()) else it
                }
                
                val releasedAmount = updatedMilestones.find { it.id == milestoneId }?.amount ?: 0.0
                
                transaction.update(escrowRef, 
                    "milestones", updatedMilestones,
                    "releasedAmount", FieldValue.increment(releasedAmount)
                )
                
                // In production, trigger real fund transfer here via Stripe Connect
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
