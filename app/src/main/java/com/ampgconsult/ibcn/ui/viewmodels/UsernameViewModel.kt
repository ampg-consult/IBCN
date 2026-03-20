package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.repository.UsernameService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsernameViewModel @Inject constructor(
    private val usernameService: UsernameService
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    sealed class ValidationState {
        object Idle : ValidationState()
        object Valid : ValidationState()
        data class Error(val message: String) : ValidationState()
    }

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking

    private val _isAvailable = MutableStateFlow<Boolean?>(null)
    val isAvailable: StateFlow<Boolean?> = _isAvailable

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    init {
        setupDebouncedValidation()
    }

    fun onUsernameChanged(newUsername: String) {
        val formatted = if (newUsername.isEmpty()) "" 
                        else if (newUsername.startsWith("@")) newUsername 
                        else "@$newUsername"
        
        _username.value = formatted
        
        if (formatted.isEmpty() || formatted == "@") {
            _validationState.value = ValidationState.Idle
            _isAvailable.value = null
            return
        }

        val result = usernameService.validateUsername(formatted)
        _validationState.value = when (result) {
            is UsernameService.ValidationResult.Valid -> ValidationState.Valid
            is UsernameService.ValidationResult.Invalid -> ValidationState.Error(result.message)
        }
        
        if (result is UsernameService.ValidationResult.Invalid) {
            _isAvailable.value = null
            _suggestions.value = emptyList()
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedValidation() {
        viewModelScope.launch {
            _username
                .debounce(500)
                .filter { it.length >= 4 && _validationState.value is ValidationState.Valid }
                .collect { debouncedUsername ->
                    _isChecking.value = true
                    try {
                        val normalized = usernameService.normalize(debouncedUsername)
                        val available = usernameService.checkAvailability(normalized)
                        _isAvailable.value = available
                        if (!available) {
                            // Fetch Elite AI suggestions if taken
                            val base = debouncedUsername.removePrefix("@")
                            _suggestions.value = usernameService.generateEliteSuggestions(base)
                        } else {
                            _suggestions.value = emptyList()
                        }
                    } catch (e: Exception) {
                        _isAvailable.value = null
                        _validationState.value = ValidationState.Error(e.message ?: "Validation failed")
                    } finally {
                        _isChecking.value = false
                    }
                }
        }
    }

    fun selectSuggestion(suggestion: String) {
        onUsernameChanged(suggestion)
    }

    fun submitUsernameWithInfo(firstName: String, lastName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val fullName = "$firstName $lastName".trim()
            val result = usernameService.reserveUsername(
                displayUsername = _username.value,
                displayName = fullName
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                _validationState.value = ValidationState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
            _isSubmitting.value = false
        }
    }
}
