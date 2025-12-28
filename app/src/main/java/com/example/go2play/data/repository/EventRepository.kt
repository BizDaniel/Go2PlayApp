package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventCreate
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class EventInsertPayload(
    @SerialName("field_id")
    val fieldId: String,
    @SerialName("organizer_id")
    val organizerId: String,
    val date: String,
    @SerialName("time_slot")
    val timeSlot: String,
    @SerialName("max_players")
    val maxPlayers: Int,
    val description: String? = null,
    @SerialName("is_private")
    val isPrivate: Boolean = false,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("current_players")
    val currentPlayers: List<String>
)

@Serializable
private data class UpdateEventPayload(
    @SerialName("p_event_id")
    val eventId: String,
    @SerialName("p_user_id")
    val userId: String,
    @SerialName("p_date")
    val date: String? = null,
    @SerialName("p_time_slot")
    val timeSlot: String? = null,
    @SerialName("p_description")
    val description: String? = null,
    @SerialName("p_is_private")
    val isPrivate: Boolean? = null,
    @SerialName("p_current_players")
    val currentPlayers: List<String>? = null
)

@Serializable
private data class CancelEventPayload(
    @SerialName("p_event_id")
    val eventId: String,
    @SerialName("p_user_id")
    val userId: String
)

class EventRepository {
    private val client = SupabaseClient.client

    // Ottieni eventi per un campo in una data specifica
    suspend fun getEventsByFieldAndDate(fieldId: String, date: String): Result<List<Event>> {
        return try {
            val events = client.from("events")
                .select {
                    filter {
                        eq("field_id", fieldId)
                        eq("date", date)
                    }
                }
                .decodeList<Event>()
            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting events by field and date", e)
            Result.failure(e)
        }
    }

    // Ottieni eventi per un campo in un range di date
    suspend fun getEventsByFieldAndDateRange(
        fieldId: String,
        startDate: String,
        endDate: String
    ): Result<List<Event>> {
        return try {
            val events = client.from("events")
                .select {
                    filter {
                        eq("field_id", fieldId)
                        gte("date", startDate)
                        lte("date", endDate)
                    }
                }
                .decodeList<Event>()

            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting events by field and date range", e)
            Result.failure(e)
        }
    }

    // Crea un nuovo evento
    suspend fun createEvent(eventCreate: EventCreate): Result<Event> {
        return try {
            val insertPayload = EventInsertPayload(
                fieldId = eventCreate.fieldId,
                organizerId = eventCreate.organizerId,
                date = eventCreate.date,
                timeSlot = eventCreate.timeSlot,
                maxPlayers = eventCreate.maxPlayers,
                description = eventCreate.description,
                isPrivate = eventCreate.isPrivate,
                groupId = eventCreate.groupId,
                currentPlayers = listOf(eventCreate.organizerId)
            )

            val createdEvent = client.from("events")
                .insert(insertPayload) {
                    select()
                }
                .decodeSingle<Event>()

            Log.d("EventRepository", "Event created successfully. Organizer ${eventCreate.organizerId} included in players.")
            Result.success(createdEvent)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error creating event", e)
            Result.failure(e)
        }
    }

    // Ottieni evento per ID
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val event = client.from("events")
                .select {
                    filter {
                        eq("id", eventId)
                    }
                }
                .decodeSingle<Event>()
            Result.success(event)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting event", e)
            Result.failure(e)
        }
    }

    suspend fun addPlayerToEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            Log.d("EventRepository", "Calling RPC join_event for user $userId on event $eventId")

            // Chiama la funzione SQL creata sopra
            client.postgrest.rpc(
                "join_event",
                parameters = mapOf(
                    "p_event_id" to eventId,
                    "p_user_id" to userId
                )
            )

            Log.d("EventRepository", "Successfully joined event via RPC")
            Result.success(Unit)
        } catch (e: Exception) {
            // Supabase lancia un'eccezione se la funzione SQL fa "raise exception"
            Log.e("EventRepository", "Error joining event: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserEvents(): Result<List<Event>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Cerca tutti gli eventi dove l'utente è presente in current_players
            val events = client.from("events")
                .select {
                    filter {
                        contains("current_players", listOf(userId))
                    }
                }
                .decodeList<Event>()

            Log.d("EventRepository", "Found ${events.size} events for user $userId")
            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting user events", e)
            Result.failure(e)
        }
    }

    // Ottengo ID corrente utente
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    suspend fun updateEvent(
        eventId: String,
        date: String? = null,
        timeSlot: String? = null,
        description: String? = null,
        isPrivate: Boolean? = null,
        currentPlayers: List<String>? = null
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val payload = UpdateEventPayload(
                eventId = eventId,
                userId = userId,
                date = date,
                timeSlot = timeSlot,
                description = description,
                isPrivate = isPrivate,
                currentPlayers = currentPlayers
            )

            client.postgrest.rpc(
                "update_event",
                parameters = mapOf(
                    "p_event_id" to eventId,
                    "p_user_id" to userId,
                    "p_date" to date,
                    "p_time_slot" to timeSlot,
                    "p_description" to description,
                    "p_is_private" to isPrivate,
                    "p_current_players" to currentPlayers
                )
            )

            Log.d("EventRepository", "Event updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error updating event", e)
            Result.failure(e)
        }
    }

    suspend fun cancelEvent(eventId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            client.postgrest.rpc(
                "cancel_event",
                parameters = mapOf(
                    "p_event_id" to eventId,
                    "p_user_id" to userId
                )
            )

            Log.d("EventRepository", "Event cancelled successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error cancelling event", e)
            Result.failure(e)
        }
    }

    suspend fun removePlayerFromEvent(eventId: String, playerId: String): Result<Unit> {
        return try {
            val event = getEventById(eventId).getOrNull()
                ?: return Result.failure(Exception("Event not found"))

            val updatedPlayers = event.currentPlayers.filter { it != playerId }

            updateEvent(
                eventId = eventId,
                currentPlayers = updatedPlayers
            )
        } catch (e: Exception) {
            Log.e("EventRepository", "Error removing player", e)
            Result.failure(e)
        }
    }
}