package com.example.go2play.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.SurfaceType
import com.example.go2play.data.repository.FieldRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreState(
    val isLoading: Boolean = false,
    val fields: List<Field> = emptyList(),
    val filteredFields: List<Field> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCapacity: Int? = null,
    val selectedIndoorFilter: Boolean? = null,
    val isSearching: Boolean = false
)

class ExploreViewModel(
    private val repository: FieldRepository = FieldRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFields()
    }

    fun loadFields() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = repository.getAllFields()
            result.fold(
                onSuccess = { fields ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        fields = fields,
                        filteredFields = fields,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading fields"
                    )
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchFields(query)
    }

    private fun searchFields(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            applyFilters()
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            delay(300) // Debounce

            val result = repository.searchFields(query)
            result.fold(
                onSuccess = { fields ->
                    _state.value = _state.value.copy(
                        filteredFields = filterByCurrentFilters(fields),
                        isSearching = false
                    )
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isSearching = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    fun filterByCapacity(capacity: Int?) {
        _state.value = _state.value.copy(selectedCapacity = capacity)
        applyFilters()
    }

    fun filterByIndoor(isIndoor: Boolean?) {
        _state.value = _state.value.copy(selectedIndoorFilter = isIndoor)
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = if (_state.value.searchQuery.isBlank()) {
            _state.value.fields
        } else {
            _state.value.fields.filter { field ->
                field.name.contains(_state.value.searchQuery, ignoreCase = true) ||
                        field.address.contains(_state.value.searchQuery, ignoreCase = true)
            }
        }

        // Filtra per capacità
        _state.value.selectedCapacity?.let { capacity ->
            filtered = filtered.filter { it.playerCapacity == capacity }
        }

        // Filtra per indoor/outdoor
        _state.value.selectedIndoorFilter?.let { isIndoor ->
            filtered = filtered.filter { it.isIndoor == isIndoor }
        }

        _state.value = _state.value.copy(filteredFields = filtered)
    }

    private fun filterByCurrentFilters(fields: List<Field>): List<Field> {
        var filtered = fields

        _state.value.selectedCapacity?.let { capacity ->
            filtered = filtered.filter { it.playerCapacity == capacity }
        }

        _state.value.selectedIndoorFilter?.let { isIndoor ->
            filtered = filtered.filter { it.isIndoor == isIndoor }
        }

        return filtered
    }

    fun clearFilters() {
        _state.value = _state.value.copy(
            selectedCapacity = null,
            selectedIndoorFilter = null,
            searchQuery = ""
        )
        applyFilters()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}