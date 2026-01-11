package com.example.go2play.ui.auth

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.repository.AuthRepository
import com.example.go2play.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val isUsernameAvailable: Boolean? = null,
    val isCheckingUsername: Boolean = false,
    val isCheckingSession: Boolean = true
)

@HiltViewModel
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
): ViewModel() {

    private val _authState  = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private var usernameCheckJob: Job? = null

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isCheckingSession = true)

            val restoreResult = authRepository.restoreSession()

            restoreResult.fold(
                onSuccess = { hasSession ->
                    val email: String?
                    if(hasSession) {
                        authRepository.startAutoRefresh(viewModelScope)
                        email = authRepository.getCurrentUSerEmail()
                    } else {
                        email = null
                    }
                    _authState.value = _authState.value.copy(
                        isAuthenticated = hasSession,
                        userEmail = email,
                        isCheckingSession = false
                    )
                },
                onFailure = {
                    _authState.value = _authState.value.copy(
                        isAuthenticated = false,
                        isCheckingSession = false
                    )
                }
            )
        }
    }

    fun checkUsernameAvailability(username: String) {
        // Cancella il job precedente se esiste
        usernameCheckJob?.cancel()

        if (username.isBlank() || username.length < 3) {
            _authState.value = _authState.value.copy(
                isUsernameAvailable = null,
                isCheckingUsername = false
            )
            return
        }

        usernameCheckJob = viewModelScope.launch {
            _authState.value = _authState.value.copy(isCheckingUsername = true)

            // Debounce di 500ms
            delay(500)

            // Usa un ID fittizio per il check durante la registrazione
            val result = profileRepository.checkUsernameAvailable(username, "")

            result.fold(
                onSuccess = { available ->
                    _authState.value = _authState.value.copy(
                        isUsernameAvailable = available,
                        isCheckingUsername = false
                    )
                },
                onFailure = {
                    _authState.value = _authState.value.copy(
                        isUsernameAvailable = null,
                        isCheckingUsername = false
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signUp(email, password, username)

            result.fold(
                onSuccess = {
                    authRepository.startAutoRefresh(viewModelScope)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userEmail = email,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error during sign up"
                    )
                }
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signIn(email, password)

            result.fold(
                onSuccess = {
                    authRepository.startAutoRefresh(viewModelScope)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userEmail = email,
                        error = null
                    )
                },
                onFailure = {
                    _authState.value = _authState.value.copy(
                        isLoading = false
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        userEmail = null,
                        error = null,
                        isCheckingSession = false
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false
                    )
                }
            )
        }
        _authState.value = AuthState(isAuthenticated = false)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun resetUsernameCheck() {
        _authState.value = _authState.value.copy(
            isUsernameAvailable = null,
            isCheckingUsername = false
        )
    }
}