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

    // Ottengo ID corrente utente
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}