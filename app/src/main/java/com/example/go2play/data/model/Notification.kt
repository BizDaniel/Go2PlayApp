package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("event_id")
    val eventId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val status: NotificationStatus = NotificationStatus.PENDING,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("event_date")
    val eventDate: String? = null,
    @SerialName("event_time_slot")
    val eventTimeSlot: String? = null,
    @SerialName("field_name")
    val fieldName: String? = null,
    @SerialName("organizer_username")
    val organizerUsername: String? = null
)

@Serializable
enum class NotificationType {
    @SerialName("event_invite")
    EVENT_INVITE,

    @SerialName("event_update")
    EVENT_UPDATE,

    @SerialName("event_cancelled")
    EVENT_CANCELLED,
}

@Serializable
enum class NotificationStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("accepted")
    ACCEPTED,

    @SerialName("declined")
    DECLINED,

    @SerialName("read")
    READ
}

@Serializable
data class NotificationCreate(
    @SerialName("user_id")
    val userId: String,
    @SerialName("event_id")
    val eventId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val status: NotificationStatus = NotificationStatus.PENDING
)