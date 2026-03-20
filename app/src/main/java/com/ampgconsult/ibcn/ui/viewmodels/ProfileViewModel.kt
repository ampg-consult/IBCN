package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadProfile()
    }

    fun loadProfile(uid: String? = null) {
        val targetUid = uid ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            firestore.collection("users").document(targetUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    _userProfile.value = snapshot?.toObject(User::class.java)
                    _isLoading.value = false
                }
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "displayName" to displayName,
                        "bio" to bio
                    )
                ).await()
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun followUser(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val followData = mapOf(
                "followerId" to currentUserId,
                "targetUserId" to targetUserId,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("followers").add(followData)
        }
    }
}
