package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.local.dao.EventDao
import com.example.go2play.data.local.entity.toEntity
import com.example.go2play.data.local.entity.toModel
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventCreate
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
){
    private val client = SupabaseClient.client

    companion object {
        private const val CACHE_VALIDITY_HOURS = 2L
        private const val TAG = "EventRepository"
    }

    /**
     * Get events by fields and date, check local cache at the beginning
     */
    suspend fun getEventsByFieldAndDate(fieldId: String, date: String): Result<List<Event>> {
        return try {
            val cachedEvents = eventDao.getEventsByFieldAndDate(fieldId, date)

            if (cachedEvents.isNotEmpty()) {
                val cacheAge = System.currentTimeMillis() - (cachedEvents.firstOrNull()?.cachedAt ?: 0)
                val validityMs = CACHE_VALIDITY_HOURS * 60 * 60 * 1000

                if (cacheAge < validityMs) {
                    Log.d(TAG, "Using ${cachedEvents.size} cached events for field $fieldId on $date")
                }
            }

            Log.d(TAG, "Fetching events for field $fieldId on $date from Supabase")
            val events = client.from("events")
                .select {
                    filter {
                        eq("field_id", fieldId)
                        eq("date", date)
                    }
                }
                .decodeList<Event>()

            eventDao.insertAll(events.map { it.toEntity() })

            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting events by field and date", e)

            // Fallback at expired cache
            try {
                val cachedEvents = eventDao.getEventsByFieldAndDate(fieldId, date)
                if (cachedEvents.isNotEmpty()) {
                    Log.d(TAG, "Using expired cache as fallback")
                    return Result.success(cachedEvents.map { it.toModel() })
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "Cache fallback failed", cacheError)
            }

            Result.failure(e)
        }
    }

    /**
     * Get event by field and date RANGE
     */
    suspend fun getEventsByFieldAndDateRange(
        fieldId: String,
        startDate: String,
        endDate: String
    ): Result<List<Event>> {
        return try {
            Log.d(TAG, "Fetching events for field $fieldId from $startDate to $endDate")
            val events = client.from("events")
                .select {
                    filter {
                        eq("field_id", fieldId)
                        gte("date", startDate)
                        lte("date", endDate)
                    }
                }
                .decodeList<Event>()

            eventDao.insertAll(events.map { it.toEntity() })

            Result.success(events)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting events by field and date range", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new event
     */
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

            eventDao.insert(createdEvent.toEntity())

            Log.d(TAG, "Event created successfully. Organizer ${eventCreate.organizerId} included in players.")
            Result.success(createdEvent)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single event by ID
     */
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val cachedEvent = eventDao.getEventById(eventId).firstOrNull()

            if (cachedEvent != null) {
                Log.d(TAG, "Event $eventId found in cache")
                return Result.success(cachedEvent.toModel())
            }

            Log.d(TAG, "Fetching event $eventId from Supabase")
            val event = client.from("events")
                .select {
                    filter {
                        eq("id", eventId)
                    }
                }
                .decodeSingle<Event>()

            eventDao.insert(event.toEntity())

            Result.success(event)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting event", e)
            Result.failure(e)
        }
    }

    /**
     * Flow to observe an event
     */
    fun observeEvent(eventId: String): Flow<Event?> {
        return eventDao.getEventById(eventId).map { entity ->
            entity?.toModel()
        }
    }

    /**
     * Add player to event
     */
    suspend fun addPlayerToEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Calling RPC join_event for user $userId on event $eventId")

            // Chiama la funzione SQL creata sopra
            client.postgrest.rpc(
                "join_event",
                parameters = mapOf(
                    "p_event_id" to eventId,
                    "p_user_id" to userId
                )
            )

            eventDao.deleteById(eventId)

            getEventById(eventId)

            Log.d("EventRepository", "Successfully joined event via RPC")
            Result.success(Unit)
        } catch (e: Exception) {
            // Supabase lancia un'eccezione se la funzione SQL fa "raise exception"
            Log.e("EventRepository", "Error joining event: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all the user's events with cache
     */
    suspend fun getUserEvents(): Result<List<Event>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val cachedEvents = eventDao.getUserEventsOnce(userId)

            if (cachedEvents.isNotEmpty()) {
                val cacheAge = System.currentTimeMillis() - (cachedEvents.firstOrNull()?.cachedAt ?: 0)
                val validityMs = CACHE_VALIDITY_HOURS * 60 * 60 * 1000

                if (cacheAge < validityMs) {
                    Log.d(TAG, "Using ${cachedEvents.size} cached user events")
                    return Result.success(cachedEvents.map { it.toModel() })
                }
            }

            Log.d(TAG, "Fetching user events from Supabase")
            val events = client.from("events")
                .select {
                    filter {
                        contains("current_players", listOf(userId))
                    }
                }
                .decodeList<Event>()

            eventDao.insertAll(events.map { it.toEntity() })

            Log.d(TAG, "Found ${events.size} events for user $userId")
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user events", e)

            try {
                val userId = getCurrentUserId() ?: return Result.failure(e)
                val cachedEvents = eventDao.getUserEventsOnce(userId)
                if (cachedEvents.isNotEmpty()) {
                    Log.d(TAG, "Using expired cache as fallback")
                    return Result.success(cachedEvents.map { it.toModel() })
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "Cache fallback failed", cacheError)
            }

            Result.failure(e)
        }
    }

    /**
     * Flow to observe user's events
     */
    fun observeUserEvents(): Flow<List<Event>> {
        val userId = getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())

        return eventDao.getUserEvents(userId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    // Ottengo ID corrente utente
    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    /**
     * Update an event
     */
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
                payload
            )

            eventDao.deleteById(eventId)

            getEventById(eventId)

            Log.d("EventRepository", "Event updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error updating event", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an event
     */
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

            val cachedEvent = eventDao.getEventByIdOnce(eventId)
            if (cachedEvent != null) {
                eventDao.updateStatus(eventId, "CANCELLED")
            }

            Log.d("EventRepository", "Event cancelled successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error cancelling event", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a Player from an event
     */
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

    /**
     * Clean Old Events from cache
     */
    suspend fun cleanOldEvents() {
        try {
            val sevenDaysAgo = LocalDate.now().minusDays(7).toString()
            eventDao.deleteEventsBeforeDate(sevenDaysAgo)
            Log.d(TAG, "Cleaned events before $sevenDaysAgo")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old events", e)
        }
    }

    /**
     * Clean old cancelled events
     */
    suspend fun cleanCancelledEvents() {
        try {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            eventDao.deleteCancelledEventsBefore(oneDayAgo)
            Log.d(TAG, "Cleaned cancelled events")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cancelled events", e)
        }
    }

    /**
     * Invalidate cache for a specific event
     */
    suspend fun invalidateEventCache(eventId: String) {
        try {
            eventDao.deleteById(eventId)
            Log.d(TAG, "Cache invalidated for event $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "Error invalidating event cache", e)
        }
    }

    /**
     * Invalidate cache of user's events
     */
    suspend fun invalidateUserEventsCache() {
        try {
            val userId = getCurrentUserId() ?: return
            eventDao.deleteUserEvents(userId)
            Log.d(TAG, "User events cache invalidated")
        } catch (e: Exception) {
            Log.e(TAG, "Error invalidating user events cache", e)
        }
    }

    /**
     * Clean all the event's cache
     */
    suspend fun clearAllEventsCache() {
        try {
            eventDao.deleteAll()
            Log.d(TAG, "All events cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all events cache", e)
        }
    }

    /**
     * Force user's events refresh
     */
    suspend fun forceRefreshUserEvents(): Result<List<Event>> {
        val userId = getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            Log.d(TAG, "Force refreshing user events")

            // Elimina cache
            eventDao.deleteUserEvents(userId)

            // Fetch da Supabase
            val events = client.from("events")
                .select {
                    filter {
                        contains("current_players", listOf(userId))
                    }
                }
                .decodeList<Event>()

            // Salva in cache
            eventDao.insertAll(events.map { it.toEntity() })

            Log.d(TAG, "Force refresh completed: ${events.size} events")
            Result.success(events)

        } catch (e: Exception) {
            Log.e(TAG, "Error force refreshing user events", e)
            Result.failure(e)
        }
    }

    /**
     * Get info about the cache
     */
    suspend fun getCacheInfo(userId: String): EventCacheInfo {
        return try {
            val userEventCount = eventDao.countUserEvents(userId)
            val today = LocalDate.now().toString()
            val publicEventCount = eventDao.countAvailablePublicEvents(today)
            val latestCache = eventDao.getLatestCacheTime() ?: 0L
            val ageHours = (System.currentTimeMillis() - latestCache) / (60 * 60 * 1000)

            EventCacheInfo(
                userEventCount = userEventCount,
                publicEventCount = publicEventCount,
                ageHours = ageHours,
                isValid = ageHours < CACHE_VALIDITY_HOURS
            )
        } catch (e: Exception) {
            EventCacheInfo(0, 0, 0, false)
        }
    }
}

data class EventCacheInfo(
    val userEventCount: Int,
    val publicEventCount: Int,
    val ageHours: Long,
    val isValid: Boolean
)
