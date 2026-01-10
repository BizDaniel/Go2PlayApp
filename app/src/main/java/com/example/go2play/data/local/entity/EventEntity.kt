package com.example.go2play.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val fieldId: String,
    val organizerId: String,
    val date: String,
    val timeSlot: String,
    val status: String,
    val maxPlayers: Int,
    val currentPlayers: String,
    val description: String?,
    val isPrivate: Boolean,
    val groupId: String?,
    val createdAt: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun Event.toEntity() = EventEntity(
    id = id,
    fieldId = fieldId,
    organizerId = organizerId,
    date = date,
    timeSlot = timeSlot,
    status = status.name,
    maxPlayers = maxPlayers,
    currentPlayers = currentPlayers.joinToString(","),
    description = description,
    isPrivate = isPrivate,
    groupId = groupId,
    createdAt = createdAt,
    cachedAt = System.currentTimeMillis()
)

fun EventEntity.toModel() = Event(
    id = id,
    fieldId = fieldId,
    organizerId = organizerId,
    date = date,
    timeSlot = timeSlot,
    status = EventStatus.valueOf(status),
    maxPlayers = maxPlayers,
    currentPlayers = currentPlayers.split(",").filter { it.isNotBlank() },
    description = description,
    isPrivate = isPrivate,
    groupId = groupId,
    createdAt = createdAt
)