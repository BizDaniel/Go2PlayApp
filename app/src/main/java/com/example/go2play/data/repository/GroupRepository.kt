package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.GroupCreate
import com.example.go2play.data.model.UserProfile
import com.example.go2play.data.remote.SupabaseClient
import kotlinx.serialization.Serializable
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.util.UUID
import javax.inject.Inject

class GroupRepository @Inject constructor(){
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

    // Ottengo un singolo gruppo per ID
    suspend fun getGroupBy(groupId: String): Result<Group> {
        return try {
            val group = client.from("groups")
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()
            Result.success(group)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting group", e)
            Result.failure(e)
        }
    }

    @Serializable
    private data class GroupUpdateData(
        val name: String? = null,
        val description: String? = null,
        val group_image_url: String? = null
    )

    // Aggiorna gruppo (solo per creatore)
    suspend fun updateGroup(
        groupId: String,
        name: String? = null,
        description: String? = null,
        groupImageUrl: String? = null
    ): Result<Unit> {
        return try {
            // Crea l'oggetto di aggiornamento solo con i campi non-null
            val updateData = when {
                name != null && description != null && groupImageUrl != null ->
                    GroupUpdateData(name = name, description = description, group_image_url = groupImageUrl)
                name != null && description != null ->
                    GroupUpdateData(name = name, description = description)
                name != null && groupImageUrl != null ->
                    GroupUpdateData(name = name, group_image_url = groupImageUrl)
                description != null && groupImageUrl != null ->
                    GroupUpdateData(description = description, group_image_url = groupImageUrl)
                name != null ->
                    GroupUpdateData(name = name)
                description != null ->
                    GroupUpdateData(description = description)
                groupImageUrl != null ->
                    GroupUpdateData(group_image_url = groupImageUrl)
                else -> return Result.success(Unit) // Nessun aggiornamento necessario
            }

            client.from("groups")
                .update(updateData) {
                    filter {
                        eq("id", groupId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error updating group", e)
            Result.failure(e)
        }
    }

    // Elimina gruppo (solo per creatore)
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            // Prima ottieni i membri del gruppo
            val group = client.from("groups")
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()

            // Rimuovi il gruppo dagli ID dei profili degli utenti
            removeGroupFromUserProfiles(group.memberIDs, groupId)

            // Elimina il gruppo
            client.from("groups")
                .delete {
                    filter {
                        eq("id", groupId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error deleting group", e)
            Result.failure(e)
        }
    }

    // Rimuovi il gruppo dai profili degli utenti, serve per la deleteGroup
    private suspend fun removeGroupFromUserProfiles(userIds: List<String>, groupId: String) {
        try {
            userIds.forEach { userId ->
                val profile = client.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingle<UserProfile>()

                val updatedGroupIds = (profile.groupIds ?: emptyList()).filter { it != groupId }

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
            Log.e("GroupRepository", "Error removing group from user profiles", e)
        }
    }

    // Lascia gruppo (per membri)
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            // Ottieni il gruppo
            val group = client.from("groups")
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()

            // Verifica che l'utente non sia il creatore
            if (group.creatorId == currentUserId) {
                return Result.failure(Exception("Creator cannot leave the group. Delete it instead."))
            }

            // Rimuovi l'utente dalla lista dei membri
            val updatedMemberIds = group.memberIDs.filter { it != currentUserId }

            // Aggiorna il gruppo
            client.from("groups")
                .update(
                    mapOf("member_ids" to updatedMemberIds)
                ) {
                    filter {
                        eq("id", groupId)
                    }
                }

            // Rimuovi il gruppo dal profilo dell'utente
            removeGroupFromUserProfiles(listOf(currentUserId), groupId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error leaving group", e)
            Result.failure(e)
        }
    }

    // Ottieni i profili dei membri del gruppo
    suspend fun getGroupMembers(memberIds: List<String>): Result<List<UserProfile>> {
        return try {
            if (memberIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val members = client.from("profiles")
                .select {
                    filter {
                        isIn("id", memberIds)
                    }
                }
                .decodeList<UserProfile>()

            Result.success(members)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error getting group members", e)
            Result.failure(e)
        }
    }

    // aggiungi un membro al gruppo (solo per creatore)
    suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            // Ottieni il gruppo corrente
            val group = client.from("groups")
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()

            // Verifica che l'utente non sia già membro
            if (group.memberIDs.contains(userId)) {
                return Result.failure(Exception("User is already a member"))
            }

            // Aggiungi il nuovo membro
            val updatedMemberIds = group.memberIDs + userId

            // Aggiorna il gruppo
            client.from("groups")
                .update(
                    mapOf("member_ids" to updatedMemberIds)
                ) {
                    filter {
                        eq("id", groupId)
                    }
                }

            // Aggiorna il profilo dell'utente
            updateUserGroupIds(listOf(userId), groupId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error adding member", e)
            Result.failure(e)
        }
    }

    // Rimuovi un membro dal gruppo (solo per creatore)
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            // Ottieni il gruppo
            val group = client.from("groups")
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()

            // Verifica che l'utente non stia cercando di rimuovere il creatore
            if (userId == group.creatorId) {
                return Result.failure(Exception("Cannot remove the group creator"))
            }

            // Verifica che chi richiede sia il creatore
            if (currentUserId != group.creatorId) {
                return Result.failure(Exception("Only the creator can remove members"))
            }

            // Rimuovi il membro
            val updatedMemberIds = group.memberIDs.filter { it != userId }

            // Aggiorna il gruppo
            client.from("groups")
                .update(
                    mapOf("member_ids" to updatedMemberIds)
                ) {
                    filter {
                        eq("id", groupId)
                    }
                }

            // Rimuovi il gruppo dal profilo dell'utente
            removeGroupFromUserProfiles(listOf(userId), groupId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error removing member", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}