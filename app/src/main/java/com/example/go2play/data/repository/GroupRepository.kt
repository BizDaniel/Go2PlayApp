package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.GroupCreate
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.util.UUID

class GroupRepository {
    private val client = SupabaseClient.client

    // Cerca utenti per username
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            if(query.isBlank()) {
                return Result.success(emptyList())
            }

            val result = client.from("profiles")
                .select {
                    filter {
                        ilike("username", "%$query%")
                    }
                }
                .decodeList<UserProfile>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error searching users", e)
            Result.failure(e)
        }
    }

    // Verifica se il nome del gruppo è disponibile
    suspend fun checkGroupNameAvailable(name: String): Result<Boolean> {
        return try {
            val result = client.from("groups")
                .select {
                    filter {
                        eq("name", name)
                    }
                }
            val available = result.data == "[]"
            Result.success(available)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error checking group name", e)
            Result.failure(e)
        }
    }

    // Upload immagine del gruppo
    suspend fun uploadGroupImage(imageBytes: ByteArray): Result<String> {
        return try {
            val bucket = client.storage.from("group-images")
            val fileName = "${UUID.randomUUID()}.jpg"

            bucket.upload(fileName, imageBytes, upsert = true)

            val publicUrl = bucket.publicUrl(fileName)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error uploading group image", e)
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        groupImageUrl: String? = null
    ): Result<Group> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            // Verifica che il creatore sia incluso nei membri
            val allMemberIds = if (currentUserId !in memberIds) {
                listOf(currentUserId) + memberIds
            } else {
                memberIds
            }

            val groupCreate = GroupCreate(
                name = name,
                description = description,
                creatorId = currentUserId,
                memberIDs = allMemberIds,
                groupImageUrl = groupImageUrl
            )

            val createdGroup = client.from("groups")
                .insert(groupCreate)
                .decodeSingle<Group>()

            updateUserGroupIds(allMemberIds, createdGroup.id)

            Result.success(createdGroup)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error creating group", e)
            Result.failure(e)
        }
    }

    // Aggionra gli ID dei gruppi per ogni utente
    private suspend fun updateUserGroupIds(userIds: List<String>, groupId: String) {
        try {
            userIds.forEach { userId ->
                // Ottieni il profilo corrente
                val profile = client.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingle<UserProfile>()

                // Aggiungo il nuovo groupId
                val updatedGroupIds = (profile.groupIds ?: emptyList()) + groupId

                // Aggiorno il profilo
                client.from("profiles")
                    .update(
                        mapOf("group_ids" to updatedGroupIds)
                    ) {
                        filter {
                            eq("id", userId)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error updating user group IDs", e)
        }
    }

    // Ottengo i gruppi dell'utente corrente
    suspend fun getUserGroups(): Result<List<Group>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            val groups = client.from("groups")
                .select {
                    filter {
                        contains("member_ids", listOf(currentUserId))
                    }
                }
                .decodeList<Group>()

            Result.success(groups)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting user groups", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}