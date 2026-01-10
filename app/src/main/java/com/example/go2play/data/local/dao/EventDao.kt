package com.example.go2play.data.local.dao

import androidx.room.*
import com.example.go2play.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE currentPlayers LIKE '%' || :userId || '%' ORDER BY date ASC, timeSlot ASC")
    fun getUserEvents(userId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE currentPlayers LIKE '%' || :userId || '%' ORDER BY date ASC, timeSlot ASC")
    suspend fun getUserEventsOnce(userId: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE currentPlayers LIKE '%' || :userId || '%' AND date >= :today ORDER BY date ASC, timeSlot ASC")
    fun getUpcomingUserEvents(userId: String, today: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE currentPlayers LIKE '%' || :userId || '%' AND date < :today ORDER BY date DESC, timeSlot DESC")
    fun getPastUserEvents(userId: String, today: String): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isPrivate = 0 
        AND status NOT IN ('CANCELLED', 'COMPLETED')
        AND date >= :today 
        ORDER BY date ASC, timeSlot ASC
    """)
    fun getPublicEvents(today: String): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isPrivate = 0 
        AND status NOT IN ('CANCELLED', 'COMPLETED')
        AND date >= :today 
        ORDER BY date ASC, timeSlot ASC
    """)
    suspend fun getPublicEventsOnce(today: String): List<EventEntity>

    @Query("""
        SELECT * FROM events 
        WHERE isPrivate = 0 
        AND status NOT IN ('CANCELLED', 'COMPLETED')
        AND date = :date 
        ORDER BY timeSlot ASC
    """)
    suspend fun getPublicEventsByDate(date: String): List<EventEntity>

    @Query("""
        SELECT * FROM events 
        WHERE isPrivate = 0 
        AND status NOT IN ('CANCELLED', 'COMPLETED')
        AND fieldId = :fieldId 
        AND date >= :today 
        ORDER BY date ASC, timeSlot ASC
    """)
    suspend fun getPublicEventsByField(fieldId: String, today: String): List<EventEntity>
    @Query("SELECT * FROM events WHERE fieldId = :fieldId AND date = :date ORDER BY timeSlot ASC")
    suspend fun getEventsByFieldAndDate(fieldId: String, date: String): List<EventEntity>

    @Query("""
        SELECT * FROM events 
        WHERE fieldId = :fieldId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date ASC, timeSlot ASC
    """)
    suspend fun getEventsByFieldAndDateRange(
        fieldId: String,
        startDate: String,
        endDate: String
    ): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventById(eventId: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventByIdOnce(eventId: String): EventEntity?

    @Query("SELECT * FROM events WHERE id IN (:eventIds)")
    suspend fun getEventsByIds(eventIds: List<String>): List<EventEntity>
    @Query("SELECT * FROM events WHERE organizerId = :userId ORDER BY date ASC, timeSlot ASC")
    fun getOrganizedEvents(userId: String): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) FROM events WHERE organizerId = :userId")
    suspend fun countOrganizedEvents(userId: String): Int

    @Query("SELECT * FROM events WHERE status = :status ORDER BY date ASC, timeSlot ASC")
    fun getEventsByStatus(status: String): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) FROM events WHERE status = 'OPEN' AND date >= :today")
    suspend fun countOpenEvents(today: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET status = :status WHERE id = :eventId")
    suspend fun updateStatus(eventId: String, status: String)

    @Query("UPDATE events SET currentPlayers = :players WHERE id = :eventId")
    suspend fun updatePlayers(eventId: String, players: String)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: String)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
    @Query("DELETE FROM events WHERE currentPlayers LIKE '%' || :userId || '%'")
    suspend fun deleteUserEvents(userId: String)
    @Query("DELETE FROM events WHERE date < :date")
    suspend fun deleteEventsBeforeDate(date: String)

    @Query("DELETE FROM events WHERE status = 'CANCELLED' AND cachedAt < :timestamp")
    suspend fun deleteCancelledEventsBefore(timestamp: Long)
    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE id = :eventId AND currentPlayers LIKE '%' || :userId || '%')")
    suspend fun isUserParticipant(eventId: String, userId: String): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE id = :eventId AND organizerId = :userId)")
    suspend fun isUserOrganizer(eventId: String, userId: String): Boolean
    @Query("SELECT COUNT(*) FROM events WHERE currentPlayers LIKE '%' || :userId || '%'")
    suspend fun countUserEvents(userId: String): Int
    @Query("""
        SELECT COUNT(*) FROM events 
        WHERE isPrivate = 0 
        AND status = 'OPEN' 
        AND date >= :today
    """)
    suspend fun countAvailablePublicEvents(today: String): Int
    @Query("SELECT MAX(cachedAt) FROM events")
    suspend fun getLatestCacheTime(): Long?
    @Query("SELECT COUNT(*) FROM events WHERE cachedAt > :timestamp")
    suspend fun countValidCache(timestamp: Long): Int
    @Query("SELECT COUNT(*) FROM events WHERE fieldId = :fieldId AND date = :date")
    suspend fun countEventsByFieldAndDate(fieldId: String, date: String): Int
}