package com.ampgconsult.ibcn.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.User
import com.ampgconsult.ibcn.data.repository.UsernameService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val usernameService: UsernameService
) : ViewModel() {

    private val TAG = "AuthViewModel"

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser, val isNewUser: Boolean) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let {
            _authState.value = AuthState.Authenticated(it, false)
        }
    }

    private suspend fun verifyFirebaseSetup() {
        val authProjectId = auth.app.options.projectId
        val firestoreProjectId = firestore.app.options.projectId
        Log.d(TAG, "PART 2: Auth Project: $authProjectId, Firestore Project: $firestoreProjectId")
        if (authProjectId != firestoreProjectId) {
            Log.e(TAG, "THROW ERROR: WRONG FIREBASE PROJECT CONNECTED")
            throw Exception("WRONG FIREBASE PROJECT CONNECTED")
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                verifyFirebaseSetup()
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    Log.d(TAG, "PART 1: Signed in user UID: ${user.uid}")
                    
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    if (!userDoc.exists()) {
                        _authState.value = AuthState.Authenticated(user, true)
                    } else {
                        val userData = userDoc.toObject(User::class.java)
                        if (userData?.username.isNullOrEmpty()) {
                            _authState.value = AuthState.Authenticated(user, true)
                        } else {
                            _authState.value = AuthState.Authenticated(user, false)
                        }
                    }
                }
            } catch (e: Exception) {
                logDetailedError("SignIn", e)
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                verifyFirebaseSetup()
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    Log.d(TAG, "PART 1: Created user UID: ${user.uid}")
                    
                    // PART 7 — GOOGLE/SIGNUP FIX: Wait for auth propagation
                    delay(800)
                    user.reload().await()
                    
                    // PART 5 — AUTOMATION: Auto-assign username
                    usernameService.autoGenerateAndReserveForGoogle(user)
                    
                    _authState.value = AuthState.Authenticated(user, false)
                }
            } catch (e: Exception) {
                logDetailedError("SignUp", e)
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun handleOAuthSignIn(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                verifyFirebaseSetup()
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Google Sign-In failed")

                Log.d(TAG, "PART 1: Google Auth UID: ${user.uid}")

                // PART 7 — GOOGLE SIGN-IN FIX
                delay(800)
                user.reload().await()

                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val needsUsername = !userDoc.exists() || userDoc.toObject(User::class.java)?.username.isNullOrEmpty()

                if (needsUsername) {
                    usernameService.autoGenerateAndReserveForGoogle(user)
                }
                _authState.value = AuthState.Authenticated(user, false)
            } catch (e: Exception) {
                logDetailedError("GoogleSignIn", e)
                _authState.value = AuthState.Error(e.message ?: "Google Authentication failed")
            }
        }
    }

    private fun logDetailedError(operation: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e(TAG, "PART 9: $operation Failed. Error Code: ${e.code}, Message: ${e.message}")
        } else {
            Log.e(TAG, "PART 9: $operation Failed. Message: ${e.message}")
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}
