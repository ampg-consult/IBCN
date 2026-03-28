package com.ampgconsult.ibcn.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.MediaStatus
import com.ampgconsult.ibcn.data.models.ViralVideoMetadata
import com.ampgconsult.ibcn.data.repository.MediaGenerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViralMediaUiState(
    val media: ViralVideoMetadata? = null,
    val isGenerating: Boolean = false,
    val progress: Int = 0,
    val stage: String = "",
    val statusText: String = "",
    val error: String? = null
)

@HiltViewModel
class ViralMediaViewModel @Inject constructor(
    private val mediaService: MediaGenerationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViralMediaUiState())
    val uiState: StateFlow<ViralMediaUiState> = _uiState.asStateFlow()
    
    private var pollingJob: Job? = null

    fun loadMedia(assetId: String) {
        viewModelScope.launch {
            val media = mediaService.getMediaForAsset(assetId)
            _uiState.update { it.copy(media = media) }
            
            // If it's still generating or pending, resume polling
            if (media?.status == MediaStatus.GENERATING || media?.status == MediaStatus.PENDING) {
                pollForJobStatus(media.id, assetId)
            }
        }
    }

    fun generateMedia(assetId: String, title: String, description: String) {
        _uiState.update { it.copy(
            isGenerating = true, 
            error = null, 
            progress = 0, 
            stage = "script", 
            statusText = "AI Director crafting script..." 
        ) }
        
        viewModelScope.launch {
            val result = mediaService.generateAIJob("video", "$title: $description", assetId)
            result.onSuccess { jobId ->
                val initialMedia = ViralVideoMetadata(
                    id = jobId,
                    assetId = assetId,
                    status = MediaStatus.GENERATING,
                    caption = "Initializing..."
                )
                _uiState.update { it.copy(media = initialMedia) }
                pollForJobStatus(jobId, assetId)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isGenerating = false) }
            }
        }
    }

    private fun pollForJobStatus(jobId: String, assetId: String) {
        pollingJob?.cancel() // FIX: Avoid multiple polling loops
        pollingJob = viewModelScope.launch {
            var isPolling = true
            while (isPolling) {
                val statusUpdate = mediaService.getJobStatus(jobId)
                
                if (statusUpdate != null) {
                    Log.d("JOB_STATUS", "Job: $jobId | Status: ${statusUpdate.status} | Stage: ${statusUpdate.stage} | Progress: ${statusUpdate.progress}%")

                    val displayStatusText = when (statusUpdate.stage) {
                        "script" -> "AI Director crafting script..."
                        "image" -> "Generating cinematic visuals..."
                        "audio" -> "Synthesizing AI voiceover..."
                        "rendering" -> "Rendering 4K frames..."
                        "merging" -> "Finalizing production..."
                        "uploading" -> "Securing to cloud storage..."
                        "done" -> "Production Complete! 🎬"
                        else -> "Processing..."
                    }

                    _uiState.update { it.copy(
                        progress = statusUpdate.progress,
                        stage = statusUpdate.stage,
                        statusText = displayStatusText,
                        // TERMINAL CHECK: Strictly 'completed' or 'failed'
                        isGenerating = statusUpdate.status != "completed" && statusUpdate.status != "failed"
                    ) }

                    if (statusUpdate.status == "completed") {
                        val videoUrl = statusUpdate.videoUrl ?: ""
                        if (videoUrl.isNotEmpty()) {
                            val finalMedia = _uiState.value.media?.copy(
                                status = MediaStatus.READY, // Map 'completed' to local 'READY'
                                videoUrl = videoUrl,
                                caption = "Video ready!"
                            ) ?: ViralVideoMetadata(
                                id = jobId,
                                assetId = assetId,
                                status = MediaStatus.READY,
                                videoUrl = videoUrl
                            )
                            
                            _uiState.update { it.copy(media = finalMedia, isGenerating = false) }
                            mediaService.saveMediaToFirestore(finalMedia)
                            isPolling = false
                        }
                    } else if (statusUpdate.status == "failed") {
                        _uiState.update { it.copy(
                            error = statusUpdate.error ?: "Generation failed",
                            isGenerating = false
                        ) }
                        isPolling = false
                    }
                }
                
                if (isPolling) {
                    delay(3000) 
                }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    fun trackShare(platform: String) {
        val mediaId = _uiState.value.media?.id ?: return
        viewModelScope.launch {
            mediaService.trackShare(mediaId, platform)
        }
    }
}
