package com.example.go2play.ui.profile

import androidx.lifecycle.ViewModel
import com.example.go2play.data.repository.ProfileRepository
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
): ViewModel() {
    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true, error = null)

            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    error = "Utente non autenticato"
                )
                return@launch
            }

            val result = repository.getUserProfile(userId)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        profile = profile,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Errore nel caricamento del profilo"
                    )
                }
            )
        }
    }

    fun updateProfile(
        username: String,
        age: Int?,
        level: String?,
        preferredRoles: String?,
        onUpdateSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true, error = null)

            val userId = repository.getCurrentUserId() ?: return@launch

            val result = repository.updateProfile(
                userId = userId,
                username = username,
                age = age,
                level = level,
                preferredRoles = preferredRoles
            )

            result.fold(
                onSuccess = {
                    loadProfile() // Ricarica il profilo aggiornato
                    onUpdateSuccess()
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Errore nell'aggiornamento del profilo"
                    )
                }
            )
        }
    }

    fun uploadAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(
                isUploading = true,
                uploadSuccess = false,
                error = null
            )

            val userId = repository.getCurrentUserId() ?: return@launch

            // Upload dell'immagine
            val uploadResult = repository.uploadAvatar(userId, imageBytes)

            uploadResult.fold(
                onSuccess = { avatarUrl ->
                    // Aggiorna il profilo con la nuova URL
                    val updateResult = repository.updateProfile(
                        userId = userId,
                        avatarUrl = avatarUrl
                    )

                    updateResult.fold(
                        onSuccess = {
                            _profileState.value = _profileState.value.copy(
                                isUploading = false,
                                uploadSuccess = true
                            )
                            loadProfile()
                        },
                        onFailure = { exception ->
                            _profileState.value = _profileState.value.copy(
                                isUploading = false,
                                error = exception.message ?: "Errore nell'aggiornamento dell'avatar"
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isUploading = false,
                        error = exception.message ?: "Errore nell'upload dell'immagine"
                    )
                }
            )
        }
    }

    fun clearError() {
        _profileState.value = _profileState.value.copy(error = null)
    }
}