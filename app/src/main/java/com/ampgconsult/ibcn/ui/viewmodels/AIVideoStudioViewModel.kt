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
import kotlinx.coroutines.flow.*
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

    private var observerJob: Job? = null

    fun generateVideo(prompt: String) {
        viewModelScope.launch {
            _uiState.value = VideoStudioStatus.Generating("Generating script...", 10)
            _isListedInMarketplace.value = false
            
            val assetId = "custom_video_${System.currentTimeMillis()}"
            val result = mediaGenerationService.generateAIJob("video", prompt, assetId)

            result.onSuccess { jobId ->
                val initialMedia = ViralVideoMetadata(
                    id = jobId,
                    assetId = assetId,
                    status = MediaStatus.GENERATING,
                    caption = "Initializing..."
                )
                _generatedVideo.value = initialMedia
                startRealTimeObservation(jobId, assetId)
            }.onFailure { error ->
                _uiState.value = VideoStudioStatus.Error(error.message ?: "Failed to start generation")
            }
        }
    }

    private fun startRealTimeObservation(jobId: String, assetId: String) {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            mediaGenerationService.observeJob(jobId)
                .onEach { update ->
                    Log.d("VIDEO_STATUS", "SSE Update: $update")
                    
                    val status = update.status.lowercase()
                    if (status == "completed") {
                        val videoUrl = update.videoUrl ?: ""
                        if (videoUrl.isNotEmpty()) {
                            val finalMedia = _generatedVideo.value?.copy(
                                status = MediaStatus.READY,
                                videoUrl = videoUrl,
                                caption = "Completed 🎬"
                            ) ?: ViralVideoMetadata(id = jobId, assetId = assetId, videoUrl = videoUrl, status = MediaStatus.READY)
                            
                            _generatedVideo.value = finalMedia
                            _uiState.value = VideoStudioStatus.Completed
                            mediaGenerationService.saveMediaToFirestore(finalMedia)
                            checkMarketplaceListing()
                        }
                    } else if (status == "failed") {
                        _uiState.value = VideoStudioStatus.Error(update.error ?: "Generation failed")
                    } else {
                        val displayStatusText = when (update.stage.lowercase()) {
                            "script" -> "Generating script..."
                            "image" -> "Creating visuals..."
                            "audio" -> "Generating voice..."
                            "rendering" -> "Rendering video..."
                            "merging" -> "Finalizing video..."
                            "uploading" -> "Uploading..."
                            "done" -> "Completed 🎬"
                            else -> "Processing..."
                        }
                        _uiState.value = VideoStudioStatus.Generating(displayStatusText, update.progress)
                    }
                }
                .catch { e -> 
                    _uiState.value = VideoStudioStatus.Error("Connection lost: ${e.message}")
                }
                .collect()
        }
    }

    private fun checkMarketplaceListing() {
        val video = _generatedVideo.value ?: return
        if (video.videoUrl.isEmpty()) return
        
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

    fun sellVideo() { }

    override fun onCleared() {
        observerJob?.cancel()
        super.onCleared()
    }
}
