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
import javax.inject.Inject

data class ExploreState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val fields: List<Field> = emptyList(),
    val filteredFields: List<Field> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCapacity: Int? = null,
    val selectedIndoorFilter: Boolean? = null,
    val isSearching: Boolean = false
)

class ExploreViewModel @Inject constructor(
    private val repository: FieldRepository
) : ViewModel() {

    private val _fieldState = MutableStateFlow(ExploreState())
    val fieldState: StateFlow<ExploreState> = _fieldState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFields()
    }

    fun loadFields() {
        viewModelScope.launch {
            _fieldState.value = _fieldState.value.copy(isLoading = true, error = null)

            val result = repository.getAllFields()
            result.fold(
                onSuccess = { fields ->
                    _fieldState.value = _fieldState.value.copy(
                        isLoading = false,
                        fields = fields,
                        filteredFields = fields,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _fieldState.value = _fieldState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading fields"
                    )
                }
            )
        }
    }

    fun refreshFields() {
        viewModelScope.launch {
            _fieldState.value = _fieldState.value.copy(isRefreshing = true, error = null)

            val result = repository.getAllFields()
            result.fold(
                onSuccess = { fields ->
                    _fieldState.value = _fieldState.value.copy(
                        isRefreshing = false,
                        fields = fields,
                        error = null
                    )
                    // Riapplica i filtri correnti sui nuovi dati
                    applyFilters()
                },
                onFailure = { exception ->
                    _fieldState.value = _fieldState.value.copy(
                        isRefreshing = false,
                        error = exception.message ?: "Error refreshing fields"
                    )
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _fieldState.value = _fieldState.value.copy(searchQuery = query)
        searchFields(query)
    }

    private fun searchFields(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            applyFilters()
            return
        }

        searchJob = viewModelScope.launch {
            _fieldState.value = _fieldState.value.copy(isSearching = true)
            delay(300) // Debounce

            val result = repository.searchFields(query)
            result.fold(
                onSuccess = { fields ->
                    _fieldState.value = _fieldState.value.copy(
                        filteredFields = filterByCurrentFilters(fields),
                        isSearching = false
                    )
                },
                onFailure = { exception ->
                    _fieldState.value = _fieldState.value.copy(
                        isSearching = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    fun filterByCapacity(capacity: Int?) {
        _fieldState.value = _fieldState.value.copy(selectedCapacity = capacity)
        applyFilters()
    }

    fun filterByIndoor(isIndoor: Boolean?) {
        _fieldState.value = _fieldState.value.copy(selectedIndoorFilter = isIndoor)
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = if (_fieldState.value.searchQuery.isBlank()) {
            _fieldState.value.fields
        } else {
            _fieldState.value.fields.filter { field ->
                field.name.contains(_fieldState.value.searchQuery, ignoreCase = true) ||
                        field.address.contains(_fieldState.value.searchQuery, ignoreCase = true)
            }
        }

        // Filtra per capacità
        _fieldState.value.selectedCapacity?.let { capacity ->
            filtered = filtered.filter { it.playerCapacity == capacity }
        }

        // Filtra per indoor/outdoor
        _fieldState.value.selectedIndoorFilter?.let { isIndoor ->
            filtered = filtered.filter { it.isIndoor == isIndoor }
        }

        _fieldState.value = _fieldState.value.copy(filteredFields = filtered)
    }

    private fun filterByCurrentFilters(fields: List<Field>): List<Field> {
        var filtered = fields

        _fieldState.value.selectedCapacity?.let { capacity ->
            filtered = filtered.filter { it.playerCapacity == capacity }
        }

        _fieldState.value.selectedIndoorFilter?.let { isIndoor ->
            filtered = filtered.filter { it.isIndoor == isIndoor }
        }

        return filtered
    }

    fun clearFilters() {
        _fieldState.value = _fieldState.value.copy(
            selectedCapacity = null,
            selectedIndoorFilter = null,
            searchQuery = ""
        )
        applyFilters()
    }

    fun clearError() {
        _fieldState.value = _fieldState.value.copy(error = null)
    }
}