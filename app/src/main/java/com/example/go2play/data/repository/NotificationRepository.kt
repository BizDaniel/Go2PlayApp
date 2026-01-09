package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Notification
import com.example.go2play.data.model.NotificationCreate
import com.example.go2play.data.model.NotificationStatus
import com.example.go2play.data.model.NotificationType
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus
import com.example.go2play.data.model.UpdateEventPlayers
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UpdateNotificationStatus(
    val status: String
)

class NotificationRepository {
    private val client = SupabaseClient.client

    // Ottieni tutte le notifiche per l'utente corrente
    suspend fun getUserNotifications(): Result<List<Notification>> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            val notifications = client.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Notification>()
                .sortedByDescending { it.createdAt }

            Result.success(notifications)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error getting notifications", e)
            Result.failure(e)
        }
    }

    // Ottieni notifiche pending (inviti non ancora accettati/rifiutati)
    suspend fun getPendingNotifications(): Result<List<Notification>> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            val notifications = client.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<Notification>()
                .sortedByDescending { it.createdAt }

            Result.success(notifications)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error getting pending notifications", e)
            Result.failure(e)
        }
    }

    // Crea una notifica
    suspend fun createNotification(notificationCreate: NotificationCreate): Result<Notification> {
        return try {
            val notification = client.from("notifications")
                .insert(notificationCreate) {
                    select()
                }
                .decodeSingle<Notification>()

            Result.success(notification)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating notification", e)
            Result.failure(e)
        }
    }

    // Crea notifiche multiple (per gruppo)
    suspend fun createNotificationsForGroup(
        userIds: List<String>,
        eventId: String,
        title: String,
        message: String
    ): Result<Unit> {
        return try {
            val notifications = userIds.map { userId ->
                NotificationCreate(
                    userId = userId,
                    eventId = eventId,
                    type = NotificationType.EVENT_INVITE,
                    title = title,
                    message = message
                )
            }

            notifications.forEach { notification ->
                client.from("notifications").insert(notification)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating group notifications", e)
            Result.failure(e)
        }
    }

    // Accetta invito (aggiorna notifica e aggiunge utente all'evento)
    suspend fun acceptEventInvite(notificationId: String, eventId: String?, userId: String): Result<Unit> {
        return try {
            Log.d("NotificationRepository", "Calling RPC accept_event_invite for user $userId on event $eventId (Notification: $notificationId)")

            client.postgrest.rpc(
                "accept_event_invite",
                parameters = mapOf(
                    "p_event_id" to eventId,
                    "p_user_id" to userId,
                    "p_notification_id" to notificationId
                )
            )

            Log.d("NotificationRepository", "Invite accepted successfully via RPC")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error accepting invite: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Rifiuta invito
    suspend fun declineInvite(notificationId: String): Result<Unit> {
        return try {
            client.from("notifications")
                .update(
                    UpdateNotificationStatus(status = "declined")
                ) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error declining invite", e)
            Result.failure(e)
        }
    }

    // Segna notifica come letta
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            client.from("notifications")
                .update(
                    UpdateNotificationStatus(status = "read")
                ) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    // Elimina notifica
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            client.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error deleting notification", e)
            Result.failure(e)
        }
    }

    // Conta notifiche pending
    suspend fun getPendingNotificationCount(): Result<Int> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            val notifications = client.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<Notification>()

            Result.success(notifications.size)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error getting notification count", e)
            Result.failure(e)
        }
    }

    // Notifica per aggiunta a gruppo
    suspend fun createGroupInviteNotification(
        userId: String,
        groupId: String,
        groupName: String,
        inviterUsername: String
    ): Result<Unit> {
        return try {
            val notification = NotificationCreate(
                userId = userId,
                groupId = groupId,
                type = NotificationType.GROUP_INVITE,
                title = "Added to Group",
                message = "$inviterUsername added you to \"$groupName\"",
                status = NotificationStatus.READ
            )

            client.from("notifications").insert(notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating group invite notification", e)
            Result.failure(e)
        }
    }

    // Notifica per modifica gruppo
    suspend fun createGroupUpdateNotification(
        userIds: List<String>,
        groupId: String,
        groupName: String
    ): Result<Unit> {
        return try {
            userIds.forEach { userId ->
                val notification = NotificationCreate(
                    userId = userId,
                    groupId = groupId,
                    type = NotificationType.GROUP_UPDATE,
                    title = "Group Updated",
                    message = "The group \"$groupName\" has been updated",
                    status = NotificationStatus.READ
                )
                client.from("notifications").insert(notification)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating group update notifications", e)
            Result.failure(e)
        }
    }

    // Notifica per eliminazione gruppo
    suspend fun createGroupDeletedNotification(
        userIds: List<String>,
        groupName: String
    ): Result<Unit> {
        return try {
            userIds.forEach { userId ->
                val notification = NotificationCreate(
                    userId = userId,
                    groupId = null,
                    type = NotificationType.GROUP_DELETED,
                    title = "Group Deleted",
                    message = "The group \"$groupName\" has been deleted",
                    status = NotificationStatus.READ
                )
                client.from("notifications").insert(notification)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating group deleted notifications", e)
            Result.failure(e)
        }
    }

    // Notifica per rimozione da gruppo
    suspend fun createRemovedFromGroupNotification(
        userId: String,
        groupName: String
    ): Result<Unit> {
        return try {
            val notification = NotificationCreate(
                userId = userId,
                groupId = null,
                type = NotificationType.REMOVED_FROM_GROUP,
                title = "Removed from Group",
                message = "You have been removed from \"$groupName\"",
                status = NotificationStatus.READ
            )

            client.from("notifications").insert(notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating removed from group notification", e)
            Result.failure(e)
        }
    }

    // Notifica per modifica evento
    suspend fun createEventUpdateNotification(
        userIds: List<String>,
        eventId: String,
        fieldName: String,
        newDate: String,
        newTimeSlot: String
    ): Result<Unit> {
        return try {
            userIds.forEach { userId ->
                val notification = NotificationCreate(
                    userId = userId,
                    eventId = eventId,
                    type = NotificationType.EVENT_UPDATE,
                    title = "Event Updated",
                    message = "Event at $fieldName has been updated to $newDate at $newTimeSlot",
                    status = NotificationStatus.READ
                )
                client.from("notifications").insert(notification)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating event update notifications", e)
            Result.failure(e)
        }
    }

    // Notifica per cancellazione evento
    suspend fun createEventCancelledNotification(
        userIds: List<String>,
        fieldName: String,
        eventDate: String,
        eventTimeSlot: String
    ): Result<Unit> {
        return try {
            userIds.forEach { userId ->
                val notification = NotificationCreate(
                    userId = userId,
                    eventId = null,
                    type = NotificationType.EVENT_CANCELLED,
                    title = "Event Cancelled",
                    message = "The event at $fieldName on $eventDate at $eventTimeSlot has been cancelled",
                    status = NotificationStatus.READ
                )
                client.from("notifications").insert(notification)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating event cancelled notifications", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}