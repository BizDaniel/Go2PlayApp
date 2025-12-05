package com.example.go2play.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.go2play.data.model.Notification
import com.example.go2play.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val pendingCount: Int = 0,
    val error: String? = null,
    val isAccepting: Boolean = false,
    val isDeclining: Boolean = false
)

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationState.value = _notificationState.value.copy(
                isLoading = true,
                error = null
            )

            val notificationsResult = repository.getUserNotifications()
            val countResult = repository.getPendingNotificationCount()

            notificationsResult.fold(
                onSuccess = { notifications ->
                    countResult.fold(
                        onSuccess = { count ->
                            _notificationState.value = _notificationState.value.copy(
                                isLoading = false,
                                notifications = notifications,
                                pendingCount = count,
                                error = null
                            )
                        },
                        onFailure = { exception ->
                            _notificationState.value = _notificationState.value.copy(
                                isLoading = false,
                                notifications = notifications,
                                error = exception.message
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _notificationState.value = _notificationState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error loading notifications"
                    )
                }
            )
        }
    }

    fun acceptInvite(notification: Notification) {
        viewModelScope.launch {
            _notificationState.value = _notificationState.value.copy(isAccepting = true, error = null)

            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _notificationState.value = _notificationState.value.copy(
                    isAccepting = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            val result = repository.acceptEventInvite(
                eventId = notification.eventId,
                userId = userId,
                notificationId = notification.id
            )

            result.fold(
                onSuccess = {
                    _notificationState.value = _notificationState.value.copy(
                        isAccepting = false
                    )
                    loadNotifications()
                },
                onFailure = { exception ->
                    _notificationState.value = _notificationState.value.copy(
                        isAccepting = false,
                        error = exception.message ?: "Error accepting invite"
                    )
                }
            )
        }
    }

    fun declineInvite(notificationId: String) {
        viewModelScope.launch {
            _notificationState.value = _notificationState.value.copy(
                isDeclining = true,
                error = null
            )

            val result = repository.declineInvite(notificationId)

            result.fold(
                onSuccess = {
                    _notificationState.value = _notificationState.value.copy(
                        isDeclining = false
                    )
                    // Ricarica le notifiche
                    loadNotifications()
                },
                onFailure = { exception ->
                    _notificationState.value = _notificationState.value.copy(
                        isDeclining = false,
                        error = exception.message ?: "Error declining invite"
                    )
                }
            )
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(notificationId)
            result.fold(
                onSuccess = {
                    loadNotifications()
                },
                onFailure = { exception ->
                    _notificationState.value = _notificationState.value.copy(
                        error = exception.message ?: "Error deleting notification"
                    )
                }
            )
        }
    }

    fun clearError() {
        _notificationState.value = _notificationState.value.copy(error = null)
    }
}