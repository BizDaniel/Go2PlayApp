package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventCreate
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

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
            val createdEvent = client.from("events")
                .insert(eventCreate) {
                    select()
                }
                .decodeSingle<Event>()

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
            val event = client.from("events")
                .select {
                    filter {
                        eq("id", eventId)
                    }
                }
                .decodeSingle<Event>()

            if (event.currentPlayers.contains(userId)) {
                Log.d("EventRepository", "Player $userId already in event $eventId")
                return Result.success(Unit) // Giocatore già presente, non fare nulla
            }

            // 3. Aggiungi il giocatore alla lista
            val updatedPlayers = event.currentPlayers + userId

            val newStatus = if (updatedPlayers.size >= event.maxPlayers) {
                "full"
            } else {
                event.status.name.lowercase()
            }

            // 4. Aggiorna l'evento nel database
            client.from("events")
                .update(
                    mapOf("current_players" to updatedPlayers,
                        "status" to newStatus
                        )
                ) {
                    filter {
                        eq("id", eventId)
                    }
                }

            Log.d("EventRepository", "Player $userId added to event $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error adding player to event", e)
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
}