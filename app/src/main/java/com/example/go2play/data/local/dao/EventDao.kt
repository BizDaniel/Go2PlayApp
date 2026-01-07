package com.example.go2play.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.go2play.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // Recupera tutti gli eventi salvati (miei) ordinati per lista
    @Query("SELECT * FROM events ORDER BY date DESC, timeslot ASC")
    fun getMyEvents(): Flow<List<EventEntity>>

    // Recupera un singolo evento per ID
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    // Inserisce o aggiorna una lista di eventi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    // cancella tutti gli eventi
    @Query("DELETE FROM events")
    suspend fun clearAll()

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Transaction
    suspend fun refreshEvents(events: List<EventEntity>) {
        clearAll()
        insertEvents(events)
    }
}