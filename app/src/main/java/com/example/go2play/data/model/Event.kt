package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    @SerialName("field_id")
    val fieldId: String,
    @SerialName("organizer_id")
    val organizerId: String,
    val date: String, // formato "2025-01-15"
    @SerialName("time_slot")
    val timeSlot: String,
    val status: EventStatus = EventStatus.OPEN,
    @SerialName("max_players")
    val maxPlayers: Int,
    @SerialName("current_players")
    val currentPlayers: List<String> = emptyList(),
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
enum class EventStatus {
    @SerialName("open")
    OPEN,

    @SerialName("full")
    FULL,

    @SerialName("cancelled")
    CANCELLED,

    @SerialName("completed")
    COMPLETED
}

@Serializable
data class EventCreate(
    @SerialName("field_id")
    val fieldId: String,
    @SerialName("organizer_id")
    val organizerId: String,
    val date: String,
    @SerialName("time_slot")
    val timeSlot: String,
    @SerialName("max_players")
    val maxPlayers: Int,
    val description: String? = null
)

// Time slot disponibili
data class TimeSlot(
    val startTime: String,
    val endTime: String,
    val status: SlotStatus
) {
    val displayTime: String
        get() = "$startTime-$endTime"
}

enum class SlotStatus {
    AVAILABLE,
    BOOKED,
    SELECTED
}

// Genera gli slot orari predefiniti
object TimeSlots {
    fun generateSlots(): List<String> {
        val slots = mutableListOf<String>()
        var hour = 10
        var minute = 0

        while (hour < 22 || (hour == 22 && minute == 0)) {
            val startHour = String.format("%02d", hour)
            val startMinute = String.format("%02d", minute)
            minute += 90
            if(minute >= 60) {
                hour += minute / 60
                minute %= 60
            }

            val endHour = String.format("%02d", hour)
            val endMinute = String.format("%02d", minute)

            if(hour <= 22) {
                slots.add("$startHour:$startMinute-$endHour:$endMinute")
            }

            if(hour > 22 || (hour == 22 && minute > 0)) {
                break
            }
        }
        return slots
    }
}

