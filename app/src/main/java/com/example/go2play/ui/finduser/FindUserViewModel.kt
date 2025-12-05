package com.example.go2play.ui.finduser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class FindUsersState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val filteredUsers: List<UserProfile> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedUser: UserProfile? = null,
    val showUserDialog: Boolean = false
)

class FindUsersViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(FindUsersState())
    val state: StateFlow<FindUsersState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadAllUsers()
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val currentUserId = repository.getCurrentUserId()
            if (currentUserId == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = repository.getAllUsers(currentUserId)
            result.fold(
                onSuccess = { users ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        users = users,
                        filteredUsers = users,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading users"
                    )
                }
            )
        }
    }

    fun searchUsers(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(300)

            val filtered = if (query.isBlank()) {
                _state.value.users
            } else {
                _state.value.users.filter { user ->
                    user.username.contains(query, ignoreCase = true) ||
                            user.level?.contains(query, ignoreCase = true) == true ||
                            user.preferredRoles?.contains(query, ignoreCase = true) == true
                }
            }

            _state.value = _state.value.copy(filteredUsers = filtered)
        }
    }

    fun showUserDetails(user: UserProfile) {
        _state.value = _state.value.copy(
            selectedUser = user,
            showUserDialog = true
        )
    }

    fun hideUserDialog() {
        _state.value = _state.value.copy(
            selectedUser = null,
            showUserDialog = false
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}