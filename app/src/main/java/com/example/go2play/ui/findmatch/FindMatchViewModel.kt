package com.example.go2play.ui.findmatch

import androidx.compose.runtime.currentComposer
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
    val isJoining: Boolean = false,
    val currentUserId: String? = null
)

class FindMatchViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository()
) : ViewModel() {

    private val _findState = MutableStateFlow(FindMatchState())
    val findState: StateFlow<FindMatchState> = _findState.asStateFlow()

    private val dateFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        val userId = eventRepository.getCurrentUserId()
        _findState.value = _findState.value.copy(currentUserId = userId)
        loadPublicEvents()
    }

    fun loadPublicEvents() {
        viewModelScope.launch {
            _findState.value = _findState.value.copy(isLoading = true, error = null)

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
                        !event.isPrivate &&
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

            _findState.value = _findState.value.copy(
                isLoading = false,
                events = sortedEvents,
                allEvents = sortedEvents,
                availableFields = fields,
                error = null
            )
        }
    }

    fun selectDate(date: LocalDate?) {
        _findState.value = _findState.value.copy(selectedDate = date)
        applyFilters()
    }

    fun selectField(fieldId: String?) {
        _findState.value = _findState.value.copy(selectedFieldId = fieldId)
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _findState.value.allEvents

        // Filtra per data
        _findState.value.selectedDate?.let { date ->
            filtered = filtered.filter { eventInfo ->
                LocalDate.parse(eventInfo.event.date) == date
            }
        }

        // Filtra per campo
        _findState.value.selectedFieldId?.let { fieldId ->
            filtered = filtered.filter { eventInfo ->
                eventInfo.field.id == fieldId
            }
        }

        _findState.value = _findState.value.copy(events = filtered)
    }

    fun clearFilters() {
        _findState.value = _findState.value.copy(
            selectedDate = null,
            selectedFieldId = null,
            events = _findState.value.allEvents
        )
    }

    fun joinEvent(eventId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _findState.value = _findState.value.copy(isJoining = true, error = null)

            val userId = eventRepository.getCurrentUserId()
            if (userId == null) {
                _findState.value = _findState.value.copy(
                    isJoining = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = eventRepository.addPlayerToEvent(eventId, userId)

            result.fold(
                onSuccess = {
                    _findState.value = _findState.value.copy(isJoining = false)
                    // Ricarica gli eventi per aggiornare la lista
                    loadPublicEvents()
                    onSuccess()
                },
                onFailure = { exception ->
                    _findState.value = _findState.value.copy(
                        isJoining = false,
                        error = exception.message ?: "Error joining event"
                    )
                }
            )
        }
    }

    fun clearError() {
        _findState.value = _findState.value.copy(error = null)
    }
}