package com.example.go2play.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.GroupRepository
import com.example.go2play.data.repository.NotificationRepository
import com.example.go2play.data.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailState(
    val isLoading: Boolean = false,
    val group: Group? = null,
    val members: List<UserProfile> = emptyList(),
    val isCreator: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isLeaving: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserProfile> = emptyList(),
    val showAddMemberDialog: Boolean = false
)

class GroupDetailViewModel(
    private val repository: GroupRepository = GroupRepository()
): ViewModel() {

    private val _detailGroupState = MutableStateFlow(GroupDetailState())
    val detailGroupState: StateFlow<GroupDetailState> = _detailGroupState.asStateFlow()

    private var searchJob: Job? = null

    private val notificationRepository = NotificationRepository()
    private val profileRepository = ProfileRepository()
    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _detailGroupState.update { it.copy(isLoading = true) }

            val groupResult = repository.getGroupBy(groupId)
            groupResult.onSuccess { group ->
                val membersResult = repository.getGroupMembers(group.memberIDs)

                membersResult.onSuccess { members ->
                    val currentUserId = repository.getCurrentUserId()
                    val isCreator = group.creatorId == currentUserId

                    _detailGroupState.update {
                        it.copy(
                            isLoading = false,
                            group = group,
                            members = members,
                            isCreator = isCreator,
                            error = null
                        )
                    }
                }.onFailure { memberError ->
                    _detailGroupState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error loading members: ${memberError.message}"
                        )
                    }
                }
            }.onFailure { groupError ->
                _detailGroupState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading group: ${groupError.message}"
                    )
                }
            }
        }
    }

    fun uploadGroupImage(imageBytes: ByteArray) {
        val currentGroup = _detailGroupState.value.group ?: return

        viewModelScope.launch {
            _detailGroupState.value = _detailGroupState.value.copy(
                isUploadingImage = true
            )

            val uploadResult = repository.uploadGroupImage(imageBytes)

            uploadResult.fold(
                onSuccess = { imageUrl ->
                    val updateResult = repository.updateGroup(
                        groupId = currentGroup.id,
                        groupImageUrl = imageUrl
                    )

                    updateResult.fold(
                        onSuccess = {
                            _detailGroupState.value = _detailGroupState.value.copy(
                                isUploadingImage = false,
                                group = currentGroup.copy(groupImageUrl = imageUrl)
                            )
                        },
                        onFailure = { updateError ->
                            _detailGroupState.value = _detailGroupState.value.copy(
                                isUploadingImage = false,
                                error = updateError.message ?: "Error updating group image"
                            )
                        }
                    )
                },
                onFailure = { uploadError ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        isUploadingImage = false,
                        error = uploadError.message ?: "Error uploading image"
                    )
                }
            )
        }
    }

    fun updateGroup(name: String, description: String, onSuccess: () -> Unit) {
        val currentGroup = _detailGroupState.value.group ?: return

        viewModelScope.launch {
            val result = repository.updateGroup(
                groupId = currentGroup.id,
                name = name,
                description = description
            )

            result.fold(
                onSuccess = {
                    _detailGroupState.value = _detailGroupState.value.copy(
                        group = currentGroup.copy(name = name, description = description)
                    )

                    val memberIds = currentGroup.memberIDs.filter { it != currentGroup.creatorId }
                    if (memberIds.isNotEmpty()) {
                        notificationRepository.createGroupUpdateNotification(
                            userIds = memberIds,
                            groupId = currentGroup.id,
                            groupName = name
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        error = error.message ?: "Error updating group"
                    )
                }
            )
        }
    }

    fun deleteGroup(onSuccess: () -> Unit) {
        val group = _detailGroupState.value.group ?: return

        viewModelScope.launch {
            val result = repository.deleteGroup(group.id)

            result.fold(
                onSuccess = {
                    val memberIds = group.memberIDs.filter { it != group.creatorId }
                    if (memberIds.isNotEmpty()) {
                        notificationRepository.createGroupDeletedNotification(
                            userIds = memberIds,
                            groupName = group.name
                        )
                    }

                    onSuccess() // Naviga indietro
                },
                onFailure = { error ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        error = "Error deleting group: ${error.message}"
                    )
                }
            )
        }
    }

    fun leaveGroup(onSuccess: () -> Unit) {
        val groupId = _detailGroupState.value.group?.id ?: return

        viewModelScope.launch {
            _detailGroupState.value = _detailGroupState.value.copy(isLeaving = true)
            val result = repository.leaveGroup(groupId)

            result.fold(
                onSuccess = {
                    _detailGroupState.value = _detailGroupState.value.copy(isLeaving = false)
                    onSuccess() // Naviga indietro
                },
                onFailure = { error ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        isLeaving = false,
                        error = "Error leaving group: ${error.message}"
                    )
                }
            )
        }
    }

    fun toggleAddMemberDialog() {
        _detailGroupState.update {
            it.copy(
                showAddMemberDialog = !it.showAddMemberDialog,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _detailGroupState.update { it.copy(searchQuery = query) }
        searchUsers(query)
    }

    private fun searchUsers(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _detailGroupState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            _detailGroupState.update { it.copy(isSearching = true) }
            delay(300) // Debounce

            val result = repository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    // Filtra utenti che sono già membri
                    val currentMemberIds = _detailGroupState.value.members.map { it.id }
                    val filteredUsers = users.filter { it.id !in currentMemberIds }

                    _detailGroupState.update {
                        it.copy(
                            searchResults = filteredUsers,
                            isSearching = false
                        )
                    }
                },
                onFailure = { exception ->
                    _detailGroupState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearching = false,
                            error = exception.message
                        )
                    }
                }
            )
        }
    }

    fun addMember(userId: String) {
        val group = _detailGroupState.value.group ?: return

        viewModelScope.launch {
            _detailGroupState.update { it.copy(isSearching = true) }
            val result = repository.addMemberToGroup(group.id, userId)

            result.fold(
                onSuccess = {
                    val creatorProfile = profileRepository.getUserProfile(group.creatorId).getOrNull()
                    val creatorUsername = creatorProfile?.username ?: "Someone"

                    notificationRepository.createGroupInviteNotification(
                        userId = userId,
                        groupId = group.id,
                        groupName = group.name,
                        inviterUsername = creatorUsername
                    )
                    // Ricarica il gruppo per aggiornare la lista membri
                    loadGroup(group.id)

                    _detailGroupState.update {
                        it.copy(
                            showAddMemberDialog = false,
                            searchQuery = "",
                            searchResults = emptyList(),
                            isSearching = false
                        )
                    }
                },
                onFailure = { error ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        isSearching = false,
                        error = "Error adding member: ${error.message}"
                    )
                }
            )
        }
    }

    fun removeMember(userId: String) {
        val group = _detailGroupState.value.group ?: return

        viewModelScope.launch {
            val result = repository.removeMemberFromGroup(group.id, userId)

            result.fold(
                onSuccess = {
                    notificationRepository.createRemovedFromGroupNotification(
                        userId = userId,
                        groupName = group.name
                    )
                    // Ricarica il gruppo per aggiornare la lista membri
                    loadGroup(group.id)
                },
                onFailure = { error ->
                    _detailGroupState.value = _detailGroupState.value.copy(
                        error = "Error removing member: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _detailGroupState.value = _detailGroupState.value.copy(error = null)
    }
}