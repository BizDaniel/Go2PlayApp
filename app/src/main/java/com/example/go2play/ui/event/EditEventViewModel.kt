package com.example.go2play.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.SlotStatus
import com.example.go2play.data.model.TimeSlot
import com.example.go2play.data.model.TimeSlots
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.repository.EventRepository
import com.example.go2play.data.repository.FieldRepository
import com.example.go2play.data.repository.GroupRepository
import com.example.go2play.data.repository.NotificationRepository
import com.example.go2play.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

data class EditEventState(
    val isLoading: Boolean = false,
    val event: Event? = null,
    val field: Field? = null,
    val selectedDate: LocalDate? = null,
    val selectedTimeSlot: String? = null,
    val availableSlots: List<TimeSlot> = emptyList(),
    val bookedSlots: Set<String> = emptySet(),
    val showDateTimePicker: Boolean = false,
    val description: String = "",
    val isPrivate: Boolean = false,
    val players: List<UserProfile> = emptyList(),
    val isLoadingPlayers: Boolean = false,
    val showAddPlayerDialog: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserProfile> = emptyList(),
    val error: String? = null,
    val isUpdating: Boolean = false,
    val canMakePublic: Boolean = false
)

class EditEventViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val groupRepository: GroupRepository = GroupRepository()
): ViewModel() {

    private val _editEventState = MutableStateFlow(EditEventState())
    val editEventState: StateFlow<EditEventState> = _editEventState.asStateFlow()

    private val notificationRepository = NotificationRepository()

    private val dateFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isLoading = true, error = null)

            val eventResult = eventRepository.getEventById(eventId)

            eventResult.fold(
                onSuccess = { event ->
                    // Verifica che l'utente corrente sia il creatore
                    val currentUserId = eventRepository.getCurrentUserId()
                    if (currentUserId != event.organizerId) {
                        _editEventState.value = _editEventState.value.copy(
                            isLoading = false,
                            error = "Only the organizer can edit this event"
                        )
                        return@fold
                    }

                    // Carica il campo
                    val fieldResult = fieldRepository.getFieldById(event.fieldId)

                    fieldResult.fold(
                        onSuccess = { field ->
                            val date = LocalDate.parse(event.date)

                            _editEventState.value = _editEventState.value.copy(
                                isLoading = false,
                                event = event,
                                field = field,
                                selectedDate = date,
                                selectedTimeSlot = event.timeSlot,
                                description = event.description ?: "",
                                isPrivate = event.isPrivate,
                                canMakePublic = event.isPrivate, // Può diventare pubblico solo se è privato
                                error = null
                            )

                            loadEventPlayers(event.currentPlayers)
                            loadAvailableSlots(date, event.fieldId, event.timeSlot)
                        },
                        onFailure = { exception ->
                            _editEventState.value = _editEventState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Error loading field"
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _editEventState.value = _editEventState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading event"
                    )
                }
            )
        }
    }

    private fun loadEventPlayers(playerIds: List<String>) {
        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isLoadingPlayers = true)

            val players = mutableListOf<UserProfile>()
            playerIds.forEach { playerId ->
                val profileResult = profileRepository.getUserProfile(playerId)
                profileResult.onSuccess { profile ->
                    players.add(profile)
                }
            }

            _editEventState.value = _editEventState.value.copy(
                players = players,
                isLoadingPlayers = false
            )
        }
    }

    fun toggleDateTimePicker() {
        _editEventState.value = _editEventState.value.copy(
            showDateTimePicker = !_editEventState.value.showDateTimePicker
        )
    }

    fun selectDate(date: LocalDate) {
        _editEventState.value = _editEventState.value.copy(
            selectedDate = date,
            selectedTimeSlot = null
        )
        val fieldId = _editEventState.value.field?.id ?: return
        loadAvailableSlots(date, fieldId, null)
    }

    private fun loadAvailableSlots(date: LocalDate, fieldId: String, currentSlot: String?) {
        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isLoading = true)

            val dateString = date.format(dateFormatter)
            val result = eventRepository.getEventsByFieldAndDate(fieldId, dateString)

            result.fold(
                onSuccess = { events ->
                    // Rimuovi lo slot corrente dell'evento dai booked (così può essere riselezionato)
                    val bookedSlots = events
                        .filter { it.id != _editEventState.value.event?.id }
                        .map { it.timeSlot }
                        .toSet()

                    val allSlots = TimeSlots.generateSlots()

                    val availableSlots = allSlots.map { slot ->
                        TimeSlot(
                            startTime = slot.split("-")[0],
                            endTime = slot.split("-")[1],
                            status = when {
                                slot in bookedSlots -> SlotStatus.BOOKED
                                slot == _editEventState.value.selectedTimeSlot -> SlotStatus.SELECTED
                                else -> SlotStatus.AVAILABLE
                            }
                        )
                    }

                    _editEventState.value = _editEventState.value.copy(
                        isLoading = false,
                        availableSlots = availableSlots,
                        bookedSlots = bookedSlots,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _editEventState.value = _editEventState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading slots"
                    )
                }
            )
        }
    }

    fun selectTimeSlot(slot: String) {
        if (slot in _editEventState.value.bookedSlots) return

        _editEventState.value = _editEventState.value.copy(selectedTimeSlot = slot)

        val updatedSlots = _editEventState.value.availableSlots.map { timeSlot ->
            timeSlot.copy(
                status = when {
                    timeSlot.displayTime in _editEventState.value.bookedSlots -> SlotStatus.BOOKED
                    timeSlot.displayTime == slot -> SlotStatus.SELECTED
                    else -> SlotStatus.AVAILABLE
                }
            )
        }

        _editEventState.value = _editEventState.value.copy(availableSlots = updatedSlots)
    }

    fun updateDescription(description: String) {
        _editEventState.value = _editEventState.value.copy(description = description)
    }

    fun togglePrivacy(isPrivate: Boolean) {
        // Può solo cambiare da privato a pubblico, non viceversa
        if (_editEventState.value.event?.isPrivate == true && !isPrivate) {
            _editEventState.value = _editEventState.value.copy(isPrivate = isPrivate)
        }
    }

    fun toggleAddPlayerDialog() {
        _editEventState.value = _editEventState.value.copy(
            showAddPlayerDialog = !_editEventState.value.showAddPlayerDialog,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun updateSearchQuery(query: String) {
        _editEventState.value = _editEventState.value.copy(searchQuery = query)
        searchUsers(query)
    }

    private fun searchUsers(query: String) {
        if (query.isBlank()) {
            _editEventState.value = _editEventState.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isSearching = true)
            kotlinx.coroutines.delay(300)

            val result = groupRepository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    val currentPlayerIds = _editEventState.value.players.map { it.id }
                    val filteredUsers = users.filter { it.id !in currentPlayerIds }

                    _editEventState.value = _editEventState.value.copy(
                        searchResults = filteredUsers,
                        isSearching = false
                    )
                },
                onFailure = { exception ->
                    _editEventState.value = _editEventState.value.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    fun addPlayer(user: UserProfile) {
        val event = _editEventState.value.event ?: return
        val field = _editEventState.value.field ?: return

        if (_editEventState.value.players.size >= event.maxPlayers) {
            _editEventState.value = _editEventState.value.copy(
                error = "Event is full"
            )
            return
        }

        val updatedPlayers = _editEventState.value.players + user
        _editEventState.value = _editEventState.value.copy(
            players = updatedPlayers,
            showAddPlayerDialog = false,
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun removePlayer(userId: String) {
        val event = _editEventState.value.event ?: return

        // Non può rimuovere l'organizzatore
        if (userId == event.organizerId) {
            _editEventState.value = _editEventState.value.copy(
                error = "Cannot remove the organizer"
            )
            return
        }

        val updatedPlayers = _editEventState.value.players.filter { it.id != userId }
        _editEventState.value = _editEventState.value.copy(players = updatedPlayers)
    }

    fun updateEvent(onSuccess: () -> Unit) {
        val event = _editEventState.value.event ?: return
        val field = _editEventState.value.field ?: return
        val date = _editEventState.value.selectedDate
        val timeSlot = _editEventState.value.selectedTimeSlot

        if (date == null || timeSlot == null) {
            _editEventState.value = _editEventState.value.copy(
                error = "Please select date and time"
            )
            return
        }

        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isUpdating = true, error = null)

            val playerIds = _editEventState.value.players.map { it.id }

            val dateChanged = date.format(dateFormatter) != event.date
            val timeChanged = timeSlot != event.timeSlot

            val result = eventRepository.updateEvent(
                eventId = event.id,
                date = date.format(dateFormatter),
                timeSlot = timeSlot,
                description = _editEventState.value.description.ifBlank { null },
                isPrivate = _editEventState.value.isPrivate,
                currentPlayers = playerIds
            )

            result.fold(
                onSuccess = {
                    if (dateChanged || timeChanged) {
                        val membersToNotify = playerIds.filter { it != event.organizerId }
                        if (membersToNotify.isNotEmpty()) {
                            notificationRepository.createEventUpdateNotification(
                                userIds = membersToNotify,
                                eventId = event.id,
                                fieldName = field.name,
                                newDate = date.format(dateFormatter),
                                newTimeSlot = timeSlot
                            )
                        }
                    }

                    _editEventState.value = _editEventState.value.copy(isUpdating = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _editEventState.value = _editEventState.value.copy(
                        isUpdating = false,
                        error = exception.message ?: "Error updating event"
                    )
                }
            )
        }
    }

    fun cancelEvent(onSuccess: () -> Unit) {
        val event = _editEventState.value.event ?: return
        val field = _editEventState.value.field ?: return

        viewModelScope.launch {
            _editEventState.value = _editEventState.value.copy(isUpdating = true, error = null)

            val result = eventRepository.cancelEvent(event.id)

            result.fold(
                onSuccess = {
                    val membersToNotify = event.currentPlayers.filter { it != event.organizerId }
                    if (membersToNotify.isNotEmpty()) {
                        notificationRepository.createEventCancelledNotification(
                            userIds = membersToNotify,
                            fieldName = field.name,
                            eventDate = event.date,
                            eventTimeSlot = event.timeSlot
                        )
                    }

                    _editEventState.value = _editEventState.value.copy(isUpdating = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _editEventState.value = _editEventState.value.copy(
                        isUpdating = false,
                        error = exception.message ?: "Error cancelling event"
                    )
                }
            )
        }
    }

    fun clearError() {
        _editEventState.value = _editEventState.value.copy(error = null)
    }
}