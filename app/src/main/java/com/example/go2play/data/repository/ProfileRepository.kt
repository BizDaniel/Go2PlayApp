package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import java.util.UUID

class ProfileRepository {
    private val client = SupabaseClient.client

    // Funzione per avere il profilo di una persona
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val profile = client.from("profiles")
                .select {
                    // Usa il blocco filter per accedere alla funzione eq
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error getting profile", e)
            Result.failure(e)
        }
    }

    // Crea un profilo
    suspend fun createProfile(userId: String, email: String, username: String): Result<Unit> {
        return try {
            client.from("profiles").insert(
                mapOf(
                    "id" to userId,
                    "email" to email,
                    "username" to username
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error creating profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        username: String? = null,
        age: Int? = null,
        level: String? = null,
        preferredRoles: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            username?.let { updates["username"] = it }
            age?.let { updates["age"] = it }
            level?.let { updates["level"] = it }
            preferredRoles?.let { updates["preferred_roles"] = it }
            avatarUrl?.let { updates["avatar_url"] = it }

            client.from("profiles")
                .update(updates) {
                    // Usa il blocco filter per specificare le righe da aggiornare
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error updating profile", e)
            Result.failure(e)
        }
    }

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
}






