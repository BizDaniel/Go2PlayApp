package com.example.go2play.ui.profile

import androidx.lifecycle.ViewModel
import com.example.go2play.data.repository.ProfileRepository
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isInitialLoad: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
): ViewModel() {
    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    init {
        loadProfile(isInitial = true)
    }

    fun loadProfile(isInitial: Boolean = false) {
        viewModelScope.launch {
            if(isInitial && _profileState.value.profile != null) {
                return@launch
            }
            _profileState.value = _profileState.value.copy(
                isLoading = isInitial,
                isInitialLoad = isInitial,
                error = null)

            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    isInitialLoad = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = repository.getUserProfile(userId)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        isInitialLoad = false,
                        profile = profile,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        isInitialLoad = false,
                        error = exception.message ?: "Error in user profile loading"
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
                    refreshProfile() // Ricarica il profilo aggiornato
                    onUpdateSuccess()
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error in profile updating"
                    )
                }
            )
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isRefreshing = true, error = null)

            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _profileState.value = _profileState.value.copy(isRefreshing = false)
                return@launch
            }

            val result = repository.getUserProfile(userId)
            result.fold(
                onSuccess = { profile ->
                    _profileState.value = _profileState.value.copy(
                        isRefreshing = false,
                        profile = profile,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isRefreshing = false,
                        error = exception.message ?: "Error refreshing profile"
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
                            refreshProfile()
                        },
                        onFailure = { exception ->
                            _profileState.value = _profileState.value.copy(
                                isUploading = false,
                                error = exception.message ?: "Error in avatar updating"
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _profileState.value = _profileState.value.copy(
                        isUploading = false,
                        error = exception.message ?: "Error in image updating"
                    )
                }
            )
        }
    }

    fun clearError() {
        _profileState.value = _profileState.value.copy(error = null)
    }
}