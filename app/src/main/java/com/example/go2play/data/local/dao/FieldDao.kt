package com.example.go2play.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.go2play.data.local.entity.FieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Query("SELECT * FROM fields ORDER BY name ASC")
    fun getAllFields(): Flow<List<FieldEntity>>

    @Query("SELECT * FROM fields ORDER BY name ASC")
    suspend fun getAllFieldsOnce(): List<FieldEntity>

    @Query("SELECT * FROM fields WHERE id = :id")
    suspend fun getFieldById(id: String): FieldEntity?

    @Query("SELECT * FROM fields WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchFields(query: String): Flow<List<FieldEntity>>

    @Query("SELECT * FROM fields WHERE playerCapacity = :capacity")
    fun getFieldsByCapacity(capacity: Int): Flow<List<FieldEntity>>

    @Query("SELECT * FROM fields WHERE isIndoor = :isIndoor")
    fun getFieldsByIndoor(isIndoor: Boolean): Flow<List<FieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<FieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: FieldEntity)

    @Update
    suspend fun update(field: FieldEntity)

    @Query("DELETE FROM fields")
    suspend fun deleteAll()

    @Query("DELETE FROM fields WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM fields WHERE cachedAt > :timestamp")
    suspend fun countValidCache(timestamp: Long): Int

    @Query("SELECT MAX(cachedAt) FROM fields")
    suspend fun getLatestCacheTime(): Long?

    @Query("SELECT COUNT(*) FROM fields")
    suspend fun count(): Int
}