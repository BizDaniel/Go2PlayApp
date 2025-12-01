package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Notification
import com.example.go2play.data.model.NotificationCreate
import com.example.go2play.data.model.NotificationStatus
import com.example.go2play.data.model.NotificationType
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from

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
    suspend fun acceptInvite(notificationId: String, eventId: String): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            // Aggiorna lo stato della notifica
            client.from("notifications")
                .update(
                    mapOf("status" to "accepted")
                ) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            // Ottieni l'evento corrente
            val event = client.from("events")
                .select {
                    filter {
                        eq("id", eventId)
                    }
                }
                .decodeSingle<Event>()

            // Aggiungi l'utente alla lista dei giocatori
            val updatedPlayers = event.currentPlayers + userId

            // Verifica se l'evento è pieno
            val newStatus = if (updatedPlayers.size >= event.maxPlayers) {
                EventStatus.FULL
            } else {
                event.status
            }

            // Aggiorna l'evento
            client.from("events")
                .update(
                    mapOf(
                        "current_players" to updatedPlayers,
                        "status" to newStatus.name.lowercase()
                    )
                ) {
                    filter {
                        eq("id", eventId)
                    }
                }


            // Aggiorna lo stato della notifica
            client.from("notifications")
                .update(
                    mapOf("status" to NotificationStatus.ACCEPTED.name.lowercase())
                ) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error accepting invite", e)
            Result.failure(e)
        }
    }

    // Rifiuta invito
    suspend fun declineInvite(notificationId: String): Result<Unit> {
        return try {
            client.from("notifications")
                .update(
                    mapOf("status" to "declined")
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
                    mapOf("status" to "read")
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

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}