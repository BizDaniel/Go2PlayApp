package com.example.go2play.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.go2play.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :userId")
    fun getUserProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :userId")
    suspend fun getUserProfileOnce(userId: String): UserProfileEntity?

    @Query("SELECT * FROM profiles WHERE id != :excludeUserId ORDER BY username ASC")
    suspend fun getAllUsers(excludeUserId: String): List<UserProfileEntity>

    @Query("SELECT * FROM profiles WHERE username LIKE '%' || :query || '%' AND id != :excludeUserId")
    suspend fun searchUsers(query: String, excludeUserId: String): List<UserProfileEntity>

    @Query("SELECT * FROM profiles WHERE id IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<UserProfileEntity>)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM profiles WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    @Query("SELECT cachedAt FROM profiles WHERE id = :userId")
    suspend fun getCacheTime(userId: String): Long?
}