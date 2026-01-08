package com.example.go2play.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventCreate
import com.example.go2play.data.model.TimeSlot
import org.threeten.bp.LocalDate
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.SlotStatus
import com.example.go2play.data.model.TimeSlots
import com.example.go2play.data.repository.EventRepository
import com.example.go2play.data.repository.FieldRepository
import com.example.go2play.data.repository.GroupRepository
import com.example.go2play.data.repository.NotificationRepository
import com.example.go2play.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrganizeEventState(
    val isLoading: Boolean = false,
    val field: Field? = null,
    val selectedDate: LocalDate? = null,
    val selectedTimeSlot: String? = null,
    val availableSlots: List<TimeSlot> = emptyList(),
    val bookedSlots: Set<String> = emptySet(),
    val showDateTimePicker: Boolean = false,
    val description: String = "",
    val isPrivate: Boolean = false,
    val selectedGroup: Group? = null,
    val userGroups: List<Group> = emptyList(),
    val showGroupPicker: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false
)

class OrganizeEventViewModel(
    private val eventRepository: EventRepository = EventRepository(),
    private val fieldRepository: FieldRepository = FieldRepository(),
    private val groupRepository: GroupRepository = GroupRepository()
): ViewModel() {

    private val _eventState = MutableStateFlow(OrganizeEventState())
    val eventState: StateFlow<OrganizeEventState> = _eventState.asStateFlow()
    
    private val dateFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun loadField(fieldId: String) {
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true, error = null)

            val fieldResult = fieldRepository.getFieldById(fieldId)
            val groupResult = groupRepository.getUserGroups()

            fieldResult.fold(
                onSuccess = { field ->
                    groupResult.fold(
                        onSuccess = { groups ->
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                field = field,
                                userGroups = groups,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            _eventState.value = _eventState.value.copy(
                                isLoading = false,
                                field = field,
                                userGroups = emptyList(),
                                error = null
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading field"
                    )
                }
            )
        }
    }

    fun toggleDateTimePicker() {
        _eventState.value = _eventState.value.copy(
            showDateTimePicker = !_eventState.value.showDateTimePicker
        )
    }

    fun toggleGroupPicker() {
        _eventState.value = _eventState.value.copy(
            showGroupPicker = !_eventState.value.showGroupPicker
        )
    }

    fun selectDate(date: LocalDate) {
        _eventState.value = _eventState.value.copy(
            selectedDate = date,
            selectedTimeSlot = null
        )
        loadAvailableSlots(date)
    }

    private fun loadAvailableSlots(date: LocalDate) {
        val field = _eventState.value.field ?: return

        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isLoading = true)

            val dateString = date.format(dateFormatter)
            val result = eventRepository.getEventsByFieldAndDate(field.id, dateString)

            result.fold(
                onSuccess = { events ->
                    val bookedSlots = events.map { it.timeSlot }.toSet()
                    val allSlots = TimeSlots.generateSlots()

                    val availableSlots = allSlots.map { slot ->
                        TimeSlot(
                            startTime = slot.split("-")[0],
                            endTime = slot.split("-")[1],
                            status = when {
                                slot in bookedSlots -> SlotStatus.BOOKED
                                slot == _eventState.value.selectedTimeSlot -> SlotStatus.SELECTED
                                else -> SlotStatus.AVAILABLE
                            }
                        )
                    }

                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        availableSlots = availableSlots,
                        bookedSlots = bookedSlots,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _eventState.value = _eventState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading slots"
                    )
                }
            )
        }
    }

    fun selectTimeSlot(slot: String) {
        if (slot in _eventState.value.bookedSlots) return

        _eventState.value = _eventState.value.copy(selectedTimeSlot = slot)

        // Aggiorna lo stato degli slot
        val updatedSlots = _eventState.value.availableSlots.map { timeSlot ->
            timeSlot.copy(
                status = when {
                    timeSlot.displayTime in _eventState.value.bookedSlots -> SlotStatus.BOOKED
                    timeSlot.displayTime == slot -> SlotStatus.SELECTED
                    else -> SlotStatus.AVAILABLE
                }
            )
        }

        _eventState.value = _eventState.value.copy(availableSlots = updatedSlots)
    }

    fun updateDescription(description: String) {
        _eventState.value = _eventState.value.copy(description = description)
    }

    fun togglePrivacy(isPrivate: Boolean) {
        _eventState.value = _eventState.value.copy(
            isPrivate = isPrivate,
            selectedGroup = if (!isPrivate) null else _eventState.value.selectedGroup
        )
    }

    fun selectGroup(group: Group) {
        _eventState.value = _eventState.value.copy(
            selectedGroup = group,
            showGroupPicker = true
        )
    }

    fun removeSelectedGroup() {
        _eventState.value = _eventState.value.copy(selectedGroup = null)
    }

    fun createEvent(onSuccess: () -> Unit) {
        val field = _eventState.value.field
        val date = _eventState.value.selectedDate
        val timeSlot = _eventState.value.selectedTimeSlot
        val userId = eventRepository.getCurrentUserId()

        if (field == null || date == null || timeSlot == null || userId == null) {
            _eventState.value = _eventState.value.copy(
                error = "Please select date and time"
            )
            return
        }

        if (_eventState.value.isPrivate && _eventState.value.selectedGroup == null) {
            _eventState.value = _eventState.value.copy(
                error = "Please select a group for private events"
            )
            return
        }
        viewModelScope.launch {
            _eventState.value = _eventState.value.copy(isCreating = true, error = null)

            val eventCreate = EventCreate(
                fieldId = field.id,
                organizerId = userId,
                date = date.format(dateFormatter),
                timeSlot = timeSlot,
                maxPlayers = field.playerCapacity * 2, // es. 5v5 = 10 giocatori
                isPrivate = _eventState.value.isPrivate,
                groupId = _eventState.value.selectedGroup?.id,
                description = _eventState.value.description.ifBlank { null }
            )

            val result = eventRepository.createEvent(eventCreate)

            result.fold(
                onSuccess = { createdEvent ->

                    eventRepository.addPlayerToEvent(createdEvent.id, userId)

                    if(_eventState.value.isPrivate && _eventState.value.selectedGroup != null) {
                        createNotificationsForGroup(createdEvent, field, userId, onSuccess)
                    } else {
                        _eventState.value = _eventState.value.copy(isCreating = false)
                        onSuccess()
                    }
                },
                onFailure = { exception ->
                    _eventState.value = _eventState.value.copy(
                        isCreating = false,
                        error = exception.message ?: "Error creating event"
                    )
                }
            )
        }
    }

    private suspend fun createNotificationsForGroup(
        event: Event,
        field: Field,
        organizerId: String,
        onSuccess: () -> Unit
    ) {
        val group = _eventState.value.selectedGroup ?: return
        val notificationRepository = NotificationRepository()
        val profileRepository = ProfileRepository()

        // Ottieni il nome dell'organizzatore
        val organizerProfile = profileRepository.getUserProfile(organizerId).getOrNull()
        val organizerName = organizerProfile?.username ?: "Someone"

        // Filtra i membri del gruppo escludendo l'organizzatore
        val membersToNotify = group.memberIDs.filter { it != organizerId }

        if (membersToNotify.isEmpty()) {
            _eventState.value = _eventState.value.copy(isCreating = false)
            onSuccess()
            return
        }

        // Crea le notifiche
        val title = "New Event Invitation"
        val message = "$organizerName invited you to play at ${field.name} on ${event.date} at ${event.timeSlot}"

        val result = notificationRepository.createNotificationsForGroup(
            userIds = membersToNotify,
            eventId = event.id,
            title = title,
            message = message
        )

        result.fold(
            onSuccess = {
                _eventState.value = _eventState.value.copy(isCreating = false)
                onSuccess()
            },
            onFailure = { exception ->
                // Anche se le notifiche falliscono, l'evento è stato creato con successo
                _eventState.value = _eventState.value.copy(
                    isCreating = false,
                    error = "Event created but notifications failed: ${exception.message}"
                )
                onSuccess()
            }
        )
    }

    fun canCreateEvent(): Boolean {
        return _eventState.value.field != null &&
                _eventState.value.selectedDate != null &&
                _eventState.value.selectedTimeSlot != null &&
                (!_eventState.value.isPrivate || _eventState.value.selectedGroup != null) &&
                !_eventState.value.isCreating
    }

    fun clearError() {
        _eventState.value = _eventState.value.copy(error = null)
    }
}