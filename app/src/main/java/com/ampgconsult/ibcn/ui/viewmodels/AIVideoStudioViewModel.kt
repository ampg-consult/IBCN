package com.ampgconsult.ibcn.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.repository.MediaGenerationService
import com.ampgconsult.ibcn.data.repository.ViralDistributionService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VideoStudioStatus {
    object Idle : VideoStudioStatus()
    data class Generating(val message: String, val progress: Int = 0) : VideoStudioStatus()
    object Completed : VideoStudioStatus()
    data class Error(val message: String) : VideoStudioStatus()
}

@HiltViewModel
class AIVideoStudioViewModel @Inject constructor(
    private val mediaGenerationService: MediaGenerationService,
    private val viralDistributionService: ViralDistributionService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoStudioStatus>(VideoStudioStatus.Idle)
    val uiState: StateFlow<VideoStudioStatus> = _uiState.asStateFlow()

    private val _generatedVideo = MutableStateFlow<ViralVideoMetadata?>(null)
    val generatedVideo: StateFlow<ViralVideoMetadata?> = _generatedVideo.asStateFlow()

    private val _viralOptimization = MutableStateFlow<ViralOptimization?>(null)
    val viralOptimization: StateFlow<ViralOptimization?> = _viralOptimization.asStateFlow()

    private val _isListedInMarketplace = MutableStateFlow(false)
    val isListedInMarketplace: StateFlow<Boolean> = _isListedInMarketplace.asStateFlow()

    private var pollingJob: Job? = null

    fun generateVideo(prompt: String) {
        viewModelScope.launch {
            _uiState.value = VideoStudioStatus.Generating("AI Director crafting script...", 10)
            _isListedInMarketplace.value = false
            
            val assetId = "custom_video_${System.currentTimeMillis()}"
            val result = mediaGenerationService.generateAIJob(
                type = "video",
                prompt = prompt,
                assetId = assetId
            )

            result.onSuccess { jobId ->
                val initialMedia = ViralVideoMetadata(
                    id = jobId,
                    assetId = assetId,
                    status = MediaStatus.GENERATING,
                    caption = "Initializing..."
                )
                _generatedVideo.value = initialMedia
                pollVideoStatus(jobId, assetId)
            }.onFailure { error ->
                _uiState.value = VideoStudioStatus.Error(error.message ?: "Failed to start generation")
            }
        }
    }

    private fun pollVideoStatus(jobId: String, assetId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var isPolling = true
            var retryCount = 0
            
            while (isPolling && retryCount < 60) { // Timeout after ~3 mins
                delay(3000)
                val statusUpdate = mediaGenerationService.getJobStatus(jobId)
                
                if (statusUpdate == null) {
                    retryCount++
                    continue
                }

                Log.d("AIVideoStudio", "Job Status: ${statusUpdate.status} - ${statusUpdate.progress}%")

                when (statusUpdate.status?.lowercase()) {
                    "processing", "queued", "generating" -> {
                        val displayStatusText = when (statusUpdate.stage?.lowercase()) {
                            "script" -> "AI Product Manager crafting script..."
                            "image" -> "Generating cinematic visuals..."
                            "audio" -> "Synthesizing AI voiceover..."
                            "rendering" -> "Rendering cinematic frames..."
                            "merging" -> "Finalizing production..."
                            "uploading" -> "Securing to cloud storage..."
                            else -> "Production in progress..."
                        }
                        _uiState.value = VideoStudioStatus.Generating(displayStatusText, statusUpdate.progress)
                    }
                    "completed", "ready" -> {
                        val videoUrl = statusUpdate.videoUrl ?: ""
                        if (videoUrl.isNotEmpty()) {
                            val finalMedia = _generatedVideo.value?.copy(
                                status = MediaStatus.READY,
                                videoUrl = videoUrl,
                                caption = "Video generated successfully!"
                            ) ?: ViralVideoMetadata(
                                id = jobId,
                                assetId = assetId,
                                videoUrl = videoUrl,
                                status = MediaStatus.READY,
                                caption = "Video ready!"
                            )
                            
                            _generatedVideo.value = finalMedia
                            _uiState.value = VideoStudioStatus.Completed
                            mediaGenerationService.saveMediaToFirestore(finalMedia)
                            checkMarketplaceListing()
                            isPolling = false
                        }
                    }
                    "failed" -> {
                        _uiState.value = VideoStudioStatus.Error(statusUpdate.error ?: "Generation failed")
                        isPolling = false
                    }
                }
            }
            
            if (retryCount >= 60) {
                _uiState.value = VideoStudioStatus.Error("Connection timed out. Check your network.")
            }
        }
    }

    private fun checkMarketplaceListing() {
        val video = _generatedVideo.value ?: return
        if (video.videoUrl.isEmpty()) return
        
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("marketplace_assets")
            .whereEqualTo("assetUrl", video.videoUrl)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    _isListedInMarketplace.value = true
                }
            }
    }

    fun makeViral() {
        val video = _generatedVideo.value ?: return
        viewModelScope.launch {
            _uiState.value = VideoStudioStatus.Generating("Growth AI optimizing for virality...", 50)
            val result = viralDistributionService.optimizeForVirality(video)
            result.onSuccess { optimization ->
                _viralOptimization.value = optimization
                _uiState.value = VideoStudioStatus.Completed
            }.onFailure { error ->
                _uiState.value = VideoStudioStatus.Error(error.message ?: "Viral optimization failed")
            }
        }
    }

    fun schedulePost(platform: SocialPlatform) {
        val video = _generatedVideo.value ?: return
        viewModelScope.launch {
            _uiState.value = VideoStudioStatus.Generating("Scheduling post for ${platform.name}...", 80)
            val scheduledTime = Timestamp(System.currentTimeMillis() / 1000 + 3600, 0)
            val result = viralDistributionService.schedulePost(video.id, platform, scheduledTime)
            result.onSuccess {
                _uiState.value = VideoStudioStatus.Completed
            }.onFailure { error ->
                _uiState.value = VideoStudioStatus.Error(error.message ?: "Scheduling failed")
            }
        }
    }

    fun sellVideo() {
        // Implement marketplace listing logic if needed
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
