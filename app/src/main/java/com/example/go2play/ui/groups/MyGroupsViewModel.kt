package com.example.go2play.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Group
import com.example.go2play.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyGroupsState(
    val isLoading: Boolean = false,
    val groups: List<Group> = emptyList(),
    val error: String? = null
)

class MyGroupsViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MyGroupsState())
    val state: StateFlow<MyGroupsState> = _state.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.getUserGroups()
            result.fold(
                onSuccess = { groups ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        groups = groups,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading groups"
                    )
                }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

