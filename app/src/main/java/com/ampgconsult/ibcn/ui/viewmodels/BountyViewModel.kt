package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.Bounty
import com.ampgconsult.ibcn.data.repository.BountyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BountyViewModel @Inject constructor(
    private val bountyService: BountyService
) : ViewModel() {

    private val _bounties = MutableStateFlow<List<Bounty>>(emptyList())
    val bounties: StateFlow<List<Bounty>> = _bounties

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadBounties()
    }

    fun loadBounties() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _bounties.value = bountyService.getOpenBounties()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load bounties"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun claimBounty(bountyId: String) {
        viewModelScope.launch {
            val result = bountyService.claimBounty(bountyId)
            if (result.isSuccess) {
                loadBounties()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }
}
