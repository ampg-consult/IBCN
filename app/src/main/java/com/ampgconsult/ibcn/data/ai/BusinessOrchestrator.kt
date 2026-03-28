package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessOrchestrator @Inject constructor(
    private val aiService: AIService,
    private val agentEngine: AutonomousAgentEngine,
    private val saasService: SaaSService,
    private val mediaService: MediaGenerationService,
    private val viralService: ViralDistributionService,
    private val deploymentService: DeploymentService,
    private val projectRepository: ProjectRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _launchFlow = MutableSharedFlow<AgentResponse>(replay = 20)
    val launchFlow: SharedFlow<AgentResponse> = _launchFlow.asSharedFlow()

    suspend fun startFullLaunch(name: String, idea: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            _launchFlow.emit(AgentResponse(AgentType.PRODUCT_MANAGER, "MODULE 1: Validating startup idea..."))
            val validationPrompt = "Validate this startup idea: Name: $name, Idea: $idea. Return JSON with validation_score and feedback."
            
            val valResult = aiService.getResponse(validationPrompt, AgentType.PRODUCT_MANAGER)
            val valJson = JSONObject(valResult.getOrThrow())
            val score = valJson.getInt("validation_score")
            
            _launchFlow.emit(AgentResponse(AgentType.PRODUCT_MANAGER, "Validation Score: $score/100"))

            val projectId = UUID.randomUUID().toString()
            val project = mapOf(
                "id" to projectId,
                "ownerUid" to uid,
                "name" to name,
                "description" to idea,
                "status" to "VALIDATED",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("projects").document(projectId).set(project).await()

            // Build MVP
            _launchFlow.emit(AgentResponse(AgentType.DEVELOPER, "MODULE 3: AI Developer Swarm activated..."))
            agentEngine.startAutonomousBuild(projectId, "Build MVP for: $idea").collect()

            // Deploy
            _launchFlow.emit(AgentResponse(AgentType.DEVOPS, "MODULE 4: Deploying to production..."))
            deploymentService.deployProject(projectId).getOrThrow()

            // Video Generation
            _launchFlow.emit(AgentResponse(AgentType.MEDIA_STRATEGIST, "MODULE 6: Generating 4K promotional video..."))
            val mediaResult = mediaService.generateViralMedia(projectId, name, idea)
            val videoMetadata: ViralVideoMetadata = mediaResult.getOrThrow()
            _launchFlow.emit(AgentResponse(AgentType.MEDIA_STRATEGIST, "Promo video ready.", isComplete = true))

            // Distribution
            val viralOpt = viralService.optimizeForVirality(videoMetadata)
            viralOpt.onSuccess {
                viralService.schedulePost(videoMetadata.id, SocialPlatform.TIKTOK, com.google.firebase.Timestamp.now())
            }

            _launchFlow.emit(AgentResponse(AgentType.LAUNCHPAD_PLANNER, "STARTUP LAUNCH SUCCESSFUL! 🚀", isComplete = true))
            Result.success(projectId)
        } catch (e: Exception) {
            _launchFlow.emit(AgentResponse(AgentType.PRODUCT_MANAGER, "LAUNCH FAILED: ${e.message}", isError = true))
            Result.failure(e)
        }
    }
}
