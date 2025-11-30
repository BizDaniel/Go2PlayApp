package com.example.go2play.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.Field
import com.example.go2play.data.repository.EventRepository
import com.example.go2play.data.repository.FieldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventWithField(
    val event: Event,
    val field: Field
)

data class MyEventsState(
    val isLoading: Boolean = false,
    val events: List<EventWithField> = emptyList(),
    val error: String? = null
)

class MyEventsViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository()
) : ViewModel() {

    private val _myEventsState = MutableStateFlow(MyEventsState())
    val myEventsState: StateFlow<MyEventsState> = _myEventsState.asStateFlow()

    init {
        loadUserEvents()
    }

    fun loadUserEvents() {
        viewModelScope.launch {
            _myEventsState.value = _myEventsState.value.copy(isLoading = true, error = null)

            val result = eventRepository.getUserEvents()

            result.fold(
                onSuccess = { events ->
                    // Carica i dettagli dei campi per ogni evento
                    val eventsWithFields = mutableListOf<EventWithField>()

                    events.forEach { event ->
                        val fieldResult = fieldRepository.getFieldById(event.fieldId)
                        fieldResult.onSuccess { field ->
                            eventsWithFields.add(EventWithField(event, field))
                        }
                    }

                    // Ordina per data più recente
                    val sortedEvents = eventsWithFields.sortedByDescending { it.event.date }

                    _myEventsState.value = _myEventsState.value.copy(
                        isLoading = false,
                        events = sortedEvents,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _myEventsState.value = _myEventsState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading events"
                    )
                }
            )
        }
    }

    fun clearError() {
        _myEventsState.value = _myEventsState.value.copy(error = null)
    }
}