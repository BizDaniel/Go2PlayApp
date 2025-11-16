package com.example.go2play.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.GroupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateGroupState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupName: String = "",
    val groupDescription: String = "",
    val isGroupNameAvailable: Boolean? = null,
    val isCheckingName: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserProfile> = emptyList(),
    val selectedMembers: List<UserProfile> = emptyList(),
    val currentUserId: String? = null,
    val isCreating: Boolean = false,
    val isUploadingImage: Boolean = false,
    val groupImageUrl: String? = null
)

class CreateGroupViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupState())
    val state: StateFlow<CreateGroupState> = _state.asStateFlow()

    private var nameCheckJob: Job? = null
    private var searchJob: Job? = null

    init {
        val userId = repository.getCurrentUserId()
        _state.value = _state.value.copy(currentUserId = userId)
    }

    fun updateGroupName(name: String) {
        _state.value = _state.value.copy(groupName = name)
        checkGroupNameAvailability(name)
    }

    fun updateGroupDescription(description: String) {
        _state.value = _state.value.copy(groupDescription = description)
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchUsers(query)
    }

    fun uploadGroupImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingImage = true, error = null)

            val result = repository.uploadGroupImage(imageBytes)
            result.fold(
                onSuccess = { imageUrl ->
                    _state.value = _state.value.copy(
                        isUploadingImage = false,
                        groupImageUrl = imageUrl
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isUploadingImage = false,
                        error = exception.message ?: "Error uploading image"
                    )
                }
            )
        }
    }

    private fun checkGroupNameAvailability(name: String) {
        nameCheckJob?.cancel()

        if (name.isBlank() || name.length < 3) {
            _state.value = _state.value.copy(
                isGroupNameAvailable = null,
                isCheckingName = false
            )
            return
        }

        nameCheckJob = viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingName = true)
            delay(500) // Debounce

            val result = repository.checkGroupNameAvailable(name)
            result.fold(
                onSuccess = { available ->
                    _state.value = _state.value.copy(
                        isGroupNameAvailable = available,
                        isCheckingName = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isGroupNameAvailable = null,
                        isCheckingName = false
                    )
                }
            )
        }
    }

    private fun searchUsers(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            delay(300) // Debounce

            val result = repository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    // Filtra l'utente corrente dai risultati
                    val filteredUsers = users.filter { it.id != _state.value.currentUserId }
                    _state.value = _state.value.copy(
                        searchResults = filteredUsers,
                        isSearching = false
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    fun toggleMemberSelection(user: UserProfile) {
        val currentMembers = _state.value.selectedMembers
        val newMembers = if (currentMembers.contains(user)) {
            currentMembers - user
        } else {
            if (currentMembers.size < 29) { // Massimo 29 + il creatore = 30
                currentMembers + user
            } else {
                _state.value = _state.value.copy(error = "Maximum 30 members (including you)")
                currentMembers
            }
        }
        _state.value = _state.value.copy(selectedMembers = newMembers)
    }

    fun removeMember(user: UserProfile) {
        val newMembers = _state.value.selectedMembers - user
        _state.value = _state.value.copy(selectedMembers = newMembers)
    }

    fun canCreateGroup(): Boolean {
        val state = _state.value
        return state.groupName.isNotBlank() &&
                state.groupName.length >= 3 &&
                state.isGroupNameAvailable == true &&
                state.selectedMembers.isNotEmpty() && // Almeno 1 altro membro
                state.selectedMembers.size <= 29 &&
                !state.isCreating
    }

    fun createGroup(onSuccess: () -> Unit) {
        if (!canCreateGroup()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)

            val memberIds = _state.value.selectedMembers.map { it.id }
            val result = repository.createGroup(
                name = _state.value.groupName,
                description = _state.value.groupDescription.ifBlank { null }.toString(),
                memberIds = memberIds,
                groupImageUrl = _state.value.groupImageUrl
            )

            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isCreating = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isCreating = false,
                        error = exception.message ?: "Error creating group"
                    )
                }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}