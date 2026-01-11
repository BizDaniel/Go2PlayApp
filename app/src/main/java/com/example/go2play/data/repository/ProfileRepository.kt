package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.local.dao.UserProfileDao
import com.example.go2play.data.local.entity.toEntity
import com.example.go2play.data.local.entity.toModel
import com.example.go2play.data.model.ProfileUpdate
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao
){
    private val client = SupabaseClient.client

    companion object {
        private const val CACHE_VALIDITY_MINUTES = 30L
        private const val TAG = "ProfileRepository"
    }

    /**
     * Get user profile with cache
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val cachedProfile = userProfileDao.getUserProfile(userId).firstOrNull()

            if (cachedProfile != null) {
                val cacheAge = System.currentTimeMillis() - cachedProfile.cachedAt
                val validityMs = CACHE_VALIDITY_MINUTES * 60 * 1000

                if (cacheAge < validityMs) {
                    Log.d(TAG, "Using cached profile for user $userId")
                    return Result.success(cachedProfile.toModel())
                }
            }

            Log.d(TAG, "Fetching profile for user $userId from Supabase")
            val profile = client.from("profiles")
                .select {
                    // Usa il blocco filter per accedere alla funzione eq
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()

            userProfileDao.insert(profile.toEntity())

            Result.success(profile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error getting profile", e)
            try {
                val cachedProfile = userProfileDao.getUserProfileOnce(userId)
                if (cachedProfile != null) {
                    Log.d(TAG, "Using expired cache as fallback for user $userId")
                    return Result.success(cachedProfile.toModel())
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "Cache fallback failed", cacheError)
            }
            Result.failure(e)
        }
    }

    /**
     * Flow to observe a userProfile
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(userId).map { entity ->
            entity?.toModel()
        }
    }

    /**
     * Get all users
     */
    suspend fun getAllUsers(currentUserId: String): Result<List<UserProfile>> {
        return try {
            val cachedUsers = userProfileDao.getAllUsers(currentUserId)

            if (cachedUsers.isNotEmpty()) {
                Log.d(TAG, "Using ${cachedUsers.size} cached users")
                return Result.success(cachedUsers.map { it.toModel() })
            }

            Log.d(TAG, "Fetching all users from Supabase")
            val users = client.from("profiles")
                .select {
                    filter {
                        neq("id", currentUserId)
                    }
                }
                .decodeList<UserProfile>()

            userProfileDao.insertAll(users.map { it.toEntity() })

            Result.success(users)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error getting all users", e)
            Result.failure(e)
        }
    }

    /**
     * Create a profile
     */
    suspend fun createProfile(userId: String, email: String, username: String): Result<Unit> {
        return try {
            client.from("profiles").insert(
                mapOf(
                    "id" to userId,
                    "email" to email,
                    "username" to username
                )
            )

            userProfileDao.delete(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error creating profile", e)
            Result.failure(e)
        }
    }

    /**
     * Update the profile, except the email
     */
    suspend fun updateProfile(
        userId: String,
        username: String? = null,
        age: Int? = null,
        level: String? = null,
        preferredRoles: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = ProfileUpdate(
                username = username,
                age = age,
                level = level,
                preferredRoles = preferredRoles,
                avatarUrl = avatarUrl
            )

            client.from("profiles")
                .update(updates) {
                    // Usa il blocco filter per specificare le righe da aggiornare
                    filter {
                        eq("id", userId)
                    }
                }

            userProfileDao.delete(userId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating profile", e)
            Result.failure(e)
        }
    }

    /**
     * Upload the profile photo
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> {
        return try {
            val bucket = client.storage.from("avatars")
            val fileName = "$userId/${UUID.randomUUID()}.jpg"

            bucket.upload(fileName, imageBytes, upsert = true)

            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error uploading avatar", e)
            Result.failure(e)
        }
    }

    /**
     * For checking the username availability
     */
    suspend fun checkUsernameAvailable(username: String, currentUserId: String): Result<Boolean> {
        return try {
            val result = client.from("profiles")
                .select {
                    filter {
                        eq("username", username)
                    }
                }

            // Username disponibile se non esiste o se è dell'utente corrente
            val available = result.data == "[]" || result.data.contains(currentUserId)
            Result.success(available)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error checking username", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    /**
     * Cache management methods
     */
    suspend fun clearUserCache(userId: String) {
        try {
            userProfileDao.delete(userId)
            Log.d(TAG, "Cache cleared for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user cache", e)
        }
    }

    suspend fun clearAllCache() {
        try {
            userProfileDao.deleteAll()
            Log.d(TAG, "All profile cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache", e)
        }
    }

}






