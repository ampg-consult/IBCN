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
            // INITIAL STATE (Requirement: 10% Script)
            _uiState.value = VideoStudioStatus.Generating("Generating script...", 10)
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
                pollJob(jobId, assetId)
            }.onFailure { error ->
                _uiState.value = VideoStudioStatus.Error(error.message ?: "Failed to start generation")
            }
        }
    }

    private fun pollJob(jobId: String, assetId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(2000) // Poll every 2 seconds as requested
                val response = mediaGenerationService.getJobStatus(jobId)
                
                if (response == null) continue

                Log.d("VIDEO_STATUS", response.toString())

                when (response.status?.lowercase()) {
                    "completed" -> {
                        val videoUrl = response.videoUrl ?: ""
                        if (videoUrl.isNotEmpty()) {
                            val finalMedia = ViralVideoMetadata(
                                id = jobId,
                                assetId = assetId,
                                videoUrl = videoUrl,
                                status = MediaStatus.READY,
                                caption = "Completed 🎬"
                            )
                            _generatedVideo.value = finalMedia
                            _uiState.value = VideoStudioStatus.Completed
                            mediaGenerationService.saveMediaToFirestore(finalMedia)
                            break // Stop polling
                        }
                    }
                    "failed" -> {
                        _uiState.value = VideoStudioStatus.Error(response.error ?: "Generation failed")
                        break // Stop polling
                    }
                    else -> {
                        // DYNAMIC STAGE MAPPING
                        val displayStatusText = when (response.stage?.lowercase()) {
                            "script" -> "Generating script..."
                            "image" -> "Creating visuals..."
                            "audio" -> "Generating voice..."
                            "rendering" -> "Rendering video..."
                            "merging" -> "Finalizing video..."
                            "uploading" -> "Uploading..."
                            "done" -> "Completed 🎬"
                            else -> "Processing..."
                        }
                        _uiState.value = VideoStudioStatus.Generating(displayStatusText, response.progress)
                    }
                }
            }
        }
    }

    fun makeViral() {
        val video = _generatedVideo.value ?: return
        viewModelScope.launch {
            _uiState.value = VideoStudioStatus.Generating("Growth AI optimizing for virality...", 50)
            viralDistributionService.optimizeForVirality(video).onSuccess {
                _viralOptimization.value = it
                _uiState.value = VideoStudioStatus.Completed
            }
        }
    }

    fun schedulePost(platform: SocialPlatform) {
        val video = _generatedVideo.value ?: return
        viewModelScope.launch {
            viralDistributionService.schedulePost(video.id, platform, Timestamp.now()).onSuccess {
                _uiState.value = VideoStudioStatus.Completed
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
