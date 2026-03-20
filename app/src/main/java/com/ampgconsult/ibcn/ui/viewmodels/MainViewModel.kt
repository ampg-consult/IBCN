package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.ampgconsult.ibcn.data.devops.AutoFirebaseEngine
import com.ampgconsult.ibcn.data.devops.BackendState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val autoFirebase: AutoFirebaseEngine
) : ViewModel() {

    val backendState: StateFlow<BackendState> = autoFirebase.backendState

    fun retrySetup() {
        autoFirebase.retry()
    }
}
