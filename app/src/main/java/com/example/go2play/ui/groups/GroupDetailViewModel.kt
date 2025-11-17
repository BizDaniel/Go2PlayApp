package com.example.go2play.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.GroupRepository
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
    val error: String? = null
)

class GroupDetailViewModel(
    private val repository: GroupRepository = GroupRepository()
): ViewModel() {

    private val _detailGroupState = MutableStateFlow(GroupDetailState())
    val detailGroupState: StateFlow<GroupDetailState> = _detailGroupState.asStateFlow()

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
        val groupId = _detailGroupState.value.group?.id ?: return

        viewModelScope.launch {
            val result = repository.deleteGroup(groupId)

            result.fold(
                onSuccess = {
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

    fun clearError() {
        _detailGroupState.value = _detailGroupState.value.copy(error = null)
    }
}