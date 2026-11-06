package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.FirebaseManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: FirebaseUser?) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class ResetEmailSent(val message: String) : AuthUiState
}

class AuthViewModel(
    private val firebaseManager: FirebaseManager,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = firebaseManager.currentUser

    val guestUserId = preferencesRepository.guestUserId.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    fun register(name: String, email: String, pass: String, confirmPass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = firebaseManager.registerUser(name, email, pass)
            result.fold(
                onSuccess = { user ->
                    preferencesRepository.setGuestUserId(null)
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Registration failed")
                }
            )
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = firebaseManager.loginUser(email, pass)
            result.fold(
                onSuccess = { user ->
                    preferencesRepository.setGuestUserId(null)
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Login failed")
                }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your registered email")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = firebaseManager.sendPasswordReset(email)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.ResetEmailSent("Password reset link sent to $email")
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to send reset link")
                }
            )
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            preferencesRepository.setGuestUserId("local_user")
            _uiState.value = AuthUiState.Success(null)
        }
    }

    fun signInWithGoogle(activity: android.app.Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = firebaseManager.signInWithGoogle(activity)
            result.fold(
                onSuccess = { user ->
                    preferencesRepository.setGuestUserId(null)
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Google Sign-In failed")
                }
            )
        }
    }

    fun logout() {
        _uiState.value = AuthUiState.Idle
        firebaseManager.logout()
        viewModelScope.launch {
            preferencesRepository.setGuestUserId(null)
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState.Idle
    }
}
