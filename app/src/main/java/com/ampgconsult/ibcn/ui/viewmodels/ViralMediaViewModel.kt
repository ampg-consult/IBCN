package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.MediaStatus
import com.ampgconsult.ibcn.data.models.ViralVideoMetadata
import com.ampgconsult.ibcn.data.repository.MediaGenerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViralMediaUiState(
    val media: ViralVideoMetadata? = null,
    val isGenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ViralMediaViewModel @Inject constructor(
    private val mediaService: MediaGenerationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViralMediaUiState())
    val uiState: StateFlow<ViralMediaUiState> = _uiState.asStateFlow()

    fun loadMedia(assetId: String) {
        viewModelScope.launch {
            val media = mediaService.getMediaForAsset(assetId)
            _uiState.update { it.copy(media = media) }
        }
    }

    fun generateMedia(assetId: String, title: String, description: String) {
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            val result = mediaService.generateViralMedia(assetId, title, description)
            if (result.isSuccess) {
                _uiState.update { it.copy(media = result.getOrNull(), isGenerating = false) }
                // Start polling for status update since it's async
                pollForReadyStatus(assetId)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isGenerating = false) }
            }
        }
    }

    private fun pollForReadyStatus(assetId: String) {
        viewModelScope.launch {
            var isReady = false
            while (!isReady) {
                kotlinx.coroutines.delay(2000)
                val media = mediaService.getMediaForAsset(assetId)
                if (media?.status == MediaStatus.READY) {
                    _uiState.update { it.copy(media = media) }
                    isReady = true
                }
            }
        }
    }

    fun trackShare(platform: String) {
        val mediaId = _uiState.value.media?.id ?: return
        viewModelScope.launch {
            mediaService.trackShare(mediaId, platform)
        }
    }
}
