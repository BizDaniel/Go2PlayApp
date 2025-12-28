package com.example.go2play.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.EventRepository
import com.example.go2play.data.repository.FieldRepository
import com.example.go2play.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

data class EventWithField(
    val event: Event,
    val field: Field,
    val players: List<UserProfile> = emptyList()
)

enum class EventFilter{
    UPCOMING,
    PAST
}

data class MyEventsState(
    val isLoading: Boolean = false,
    val allEvents: List<EventWithField> = emptyList(),
    val filteredEvents: List<EventWithField> = emptyList(),
    val selectedFilter: EventFilter = EventFilter.UPCOMING,
    val error: String? = null,
    val isLoadingPlayers: Boolean = false,
    val upcomingCount: Int = 0,
    val pastCount: Int = 0
)

class MyEventsViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
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
                        allEvents = sortedEvents,
                        error = null
                    )

                    applyFilter(_myEventsState.value.selectedFilter)
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

    fun setFilter(filter: EventFilter) {
        _myEventsState.value = _myEventsState.value.copy(selectedFilter = filter)
        applyFilter(filter)
    }

    private fun applyFilter(filter: EventFilter) {
        val today = LocalDate.now()

        val upcomingEvents = _myEventsState.value.allEvents.filter { eventWithField ->
            val eventDate = LocalDate.parse(eventWithField.event.date)
            !eventDate.isBefore(today)
        }.sortedBy { it.event.date }

        val pastEvents = _myEventsState.value.allEvents.filter { eventWithField ->
            val eventDate = LocalDate.parse(eventWithField.event.date)
            eventDate.isBefore(today)
        }.sortedByDescending { it.event.date }

        val filtered = when (filter) {
            EventFilter.UPCOMING -> upcomingEvents
            EventFilter.PAST -> pastEvents
        }

        _myEventsState.value = _myEventsState.value.copy(
            filteredEvents = filtered,
            upcomingCount = upcomingEvents.size,
            pastCount = pastEvents.size
        )
    }

    fun loadEventPlayers(eventId: String) {
        viewModelScope.launch {
            _myEventsState.value = _myEventsState.value.copy(isLoadingPlayers = true)

            // Trova l'evento
            val eventWithField = _myEventsState.value.allEvents.find { it.event.id == eventId }

            if (eventWithField != null) {
                val event = eventWithField.event

                // Carica i profili dei giocatori
                val players = mutableListOf<UserProfile>()
                event.currentPlayers.forEach { playerId ->
                    val profileResult = profileRepository.getUserProfile(playerId)
                    profileResult.onSuccess { profile ->
                        players.add(profile)
                    }
                }

                // Aggiorna l'evento con i giocatori caricati
                val updatedAllEvents = _myEventsState.value.allEvents.map {
                    if (it.event.id == eventId) {
                        it.copy(players = players)
                    } else {
                        it
                    }
                }

                val updatedFilteredEvents = _myEventsState.value.filteredEvents.map {
                    if(it.event.id == eventId) {
                        it.copy(players = players)
                    } else {
                        it
                    }
                }

                _myEventsState.value = _myEventsState.value.copy(
                    allEvents = updatedAllEvents,
                    filteredEvents = updatedFilteredEvents,
                    isLoadingPlayers = false
                )
            } else {
                _myEventsState.value = _myEventsState.value.copy(isLoadingPlayers = false)
            }
        }
    }

    fun getCurrentUserId(): String? {
        return eventRepository.getCurrentUserId()
    }

    fun clearError() {
        _myEventsState.value = _myEventsState.value.copy(error = null)
    }
}