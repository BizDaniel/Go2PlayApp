package com.example.go2play.ui.findmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus
import com.example.go2play.data.model.Field
import com.example.go2play.data.repository.EventRepository
import com.example.go2play.data.repository.FieldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

data class EventWithFieldInfo(
    val event: Event,
    val field: Field
)

data class FindMatchState(
    val isLoading: Boolean = false,
    val events: List<EventWithFieldInfo> = emptyList(),
    val allEvents: List<EventWithFieldInfo> = emptyList(),
    val availableFields: List<Field> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedFieldId: String? = null,
    val error: String? = null,
    val isJoining: Boolean = false
)

class FindMatchViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(FindMatchState())
    val state: StateFlow<FindMatchState> = _state.asStateFlow()

    private val dateFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadPublicEvents()
    }

    fun loadPublicEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Carica tutti i campi
            val fieldsResult = fieldRepository.getAllFields()
            val fields = fieldsResult.getOrNull() ?: emptyList()

            // Calcola la data di oggi e 30 giorni avanti
            val today = LocalDate.now()
            val endDate = today.plusDays(30)

            // Carica eventi futuri per tutti i campi
            val allEventsWithFields = mutableListOf<EventWithFieldInfo>()

            fields.forEach { field ->
                val eventsResult = eventRepository.getEventsByFieldAndDateRange(
                    fieldId = field.id,
                    startDate = today.format(dateFormatter),
                    endDate = endDate.format(dateFormatter)
                )

                eventsResult.onSuccess { events ->
                    // Filtra solo eventi pubblici e non cancellati/completati
                    val publicEvents = events.filter { event ->
                        event.is_public &&
                                event.status != EventStatus.CANCELLED &&
                                event.status != EventStatus.COMPLETED &&
                                // Assicurati che la data sia futura o oggi
                                !LocalDate.parse(event.date).isBefore(today)
                    }

                    publicEvents.forEach { event ->
                        allEventsWithFields.add(EventWithFieldInfo(event, field))
                    }
                }
            }

            // Ordina per data
            val sortedEvents = allEventsWithFields.sortedBy { it.event.date }

            _state.value = _state.value.copy(
                isLoading = false,
                events = sortedEvents,
                allEvents = sortedEvents,
                availableFields = fields,
                error = null
            )
        }
    }

    fun selectDate(date: LocalDate?) {
        _state.value = _state.value.copy(selectedDate = date)
        applyFilters()
    }

    fun selectField(fieldId: String?) {
        _state.value = _state.value.copy(selectedFieldId = fieldId)
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _state.value.allEvents

        // Filtra per data
        _state.value.selectedDate?.let { date ->
            filtered = filtered.filter { eventInfo ->
                LocalDate.parse(eventInfo.event.date) == date
            }
        }

        // Filtra per campo
        _state.value.selectedFieldId?.let { fieldId ->
            filtered = filtered.filter { eventInfo ->
                eventInfo.field.id == fieldId
            }
        }

        _state.value = _state.value.copy(events = filtered)
    }

    fun clearFilters() {
        _state.value = _state.value.copy(
            selectedDate = null,
            selectedFieldId = null,
            events = _state.value.allEvents
        )
    }

    fun joinEvent(eventId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true, error = null)

            val userId = eventRepository.getCurrentUserId()
            if (userId == null) {
                _state.value = _state.value.copy(
                    isJoining = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = eventRepository.addPlayerToEvent(eventId, userId)

            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isJoining = false)
                    // Ricarica gli eventi per aggiornare la lista
                    loadPublicEvents()
                    onSuccess()
                },
                onFailure = { exception ->
                    _state.value = _state.value.copy(
                        isJoining = false,
                        error = exception.message ?: "Error joining event"
                    )
                }
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}