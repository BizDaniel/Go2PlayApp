package com.example.go2play.data.model

import android.util.Log
import com.example.go2play.data.remote.SupabaseClient
import com.example.go2play.data.remote.SupabaseClient.client
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate
import javax.inject.Inject

class ReviewRepository @Inject constructor() {
    private val client = SupabaseClient.client

    companion object {
        private const val TAG = "ReviewRepository"
    }

    /**
     * verify if the user can leave a review for another user in a specific event
     */
    suspend fun canReviewUser(eventId: String, reviewedUserId: String): Result<Boolean> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))


            // Non puoi recensire te stesso
            if (currentUserId == reviewedUserId) {
                return Result.success(false)
            }

            // Ottieni l'evento
            val event = client.from("events")
                .select {
                    filter {
                        eq("id", eventId)
                    }
                }
                .decodeSingle<Event>()

            // Verifica che l'evento sia completato
            if (event.status.name != "COMPLETED") {
                Log.d(TAG, "Event not completed yet")
                return Result.success(false)
            }

            // Verifica che l'evento sia passato
            val eventDate = LocalDate.parse(event.date)
            if (eventDate.isAfter(LocalDate.now())) {
                Log.d(TAG, "Event date is in the future")
                return Result.success(false)
            }

            // Verifica che entrambi gli utenti abbiano partecipato
            val bothParticipated = event.currentPlayers.contains(currentUserId) &&
                    event.currentPlayers.contains(reviewedUserId)

            if (!bothParticipated) {
                Log.d(TAG, "Both users must have participated in the event")
                return Result.success(false)
            }

            // Verifica che non esista già una recensione
            val existingReview = client.from("reviews")
                .select {
                    filter {
                        eq("reviewer_id", currentUserId)
                        eq("reviewed_user_id", reviewedUserId)
                        eq("event_id", eventId)
                    }
                }

            val alreadyReviewed = existingReview.data != "[]"

            if (alreadyReviewed) {
                Log.d(TAG, "User already reviewed for this event")
                return Result.success(false)
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user can review", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new match
     */
    suspend fun createReview(reviewCreate: ReviewCreate): Result<Review> {
        return try {
            // Validazione dei parametri (1-5)
            if (reviewCreate.sportsmanship !in 1..5 ||
                reviewCreate.punctuality !in 1..5 ||
                reviewCreate.reliability !in 1..5
            ) {
                return Result.failure(Exception("Rating values must be between 1 and 5"))
            }

            // Verifica che l'utente possa lasciare la recensione
            val canReview = canReviewUser(reviewCreate.eventId, reviewCreate.reviewedUserId)
                .getOrNull() ?: false

            if (!canReview) {
                return Result.failure(Exception("Cannot review this user for this event"))
            }

            val review = client.from("reviews")
                .insert(reviewCreate) {
                    select()
                }
                .decodeSingle<Review>()

            Log.d(TAG, "Review created successfully")
            Result.success(review)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating review", e)
            Result.failure(e)
        }
    }

    /**
     * Get all the review of a user
     */
    suspend fun getUserReviews(userId: String): Result<List<Review>> {
        return try {
            val reviews = client.from("reviews")
                .select {
                    filter {
                        eq("reviewed_user_id", userId)
                    }
                }
                .decodeList<Review>()
                .sortedByDescending { it.createdAt }

            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user reviews", e)
            Result.failure(e)
        }
    }

    /**
     * Get all the review that I leave as a user
     */
    suspend fun getReviewsByUser(userId: String): Result<List<Review>> {
        return try {
            val reviews = client.from("reviews")
                .select {
                    filter {
                        eq("reviewer_id", userId)
                    }
                }
                .decodeList<Review>()
                .sortedByDescending { it.createdAt }

            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reviews by user", e)
            Result.failure(e)
        }
    }

    /**
     * Get all the stats
     */
    suspend fun getUserReviewStats(userId: String): Result<ReviewStats> {
        return try {
            val reviews = getUserReviews(userId).getOrNull() ?: emptyList()

            if (reviews.isEmpty()) {
                return Result.success(
                    ReviewStats(
                        avgSportsmanship = 0.0,
                        avgPunctuality = 0.0,
                        avgReliability = 0.0,
                        totalReviews = 0
                    )
                )
            }

            val avgSportsmanship = reviews.map { it.sportsmanship }.average()
            val avgPunctuality = reviews.map { it.punctuality }.average()
            val avgReliability = reviews.map { it.reliability }.average()

            val stats = ReviewStats(
                avgSportsmanship = avgSportsmanship,
                avgPunctuality = avgPunctuality,
                avgReliability = avgReliability,
                totalReviews = reviews.size
            )

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating review stats", e)
            Result.failure(e)
        }
    }

    /**
     * Get all the users that could be reviewable by the current user
     * (completed event where both partecipated)
     */
    suspend fun getReviewableUsers(): Result<List<ReviewableUser>> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Ottieni tutti gli eventi completati dell'utente
            val completedEvents = client.from("events")
                .select {
                    filter {
                        contains("current_players", listOf(currentUserId))
                        eq("status", "completed")
                    }
                }
                .decodeList<Event>()

            val reviewableUsers = mutableListOf<ReviewableUser>()

            for (event in completedEvents) {
                // Verifica che l'evento sia passato
                val eventDate = LocalDate.parse(event.date)
                if (eventDate.isAfter(LocalDate.now())) {
                    continue
                }

                // Per ogni altro partecipante
                val otherPlayers = event.currentPlayers.filter { it != currentUserId }

                for (playerId in otherPlayers) {
                    // Verifica se esiste già una recensione
                    val existingReview = client.from("reviews")
                        .select {
                            filter {
                                eq("reviewer_id", currentUserId)
                                eq("reviewed_user_id", playerId)
                                eq("event_id", event.id)
                            }
                        }

                    val alreadyReviewed = existingReview.data != "[]"

                    if (!alreadyReviewed) {
                        reviewableUsers.add(
                            ReviewableUser(
                                userId = playerId,
                                eventId = event.id,
                                eventDate = event.date,
                                fieldId = event.fieldId
                            )
                        )
                    }
                }
            }

            Result.success(reviewableUsers.distinctBy { it.userId to it.eventId })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reviewable users", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a review (Only the review's creator can delete it)
     */
    suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Verifica che la recensione appartenga all'utente corrente
            val review = client.from("reviews")
                .select {
                    filter {
                        eq("id", reviewId)
                    }
                }
                .decodeSingle<Review>()

            if (review.reviewerId != currentUserId) {
                return Result.failure(Exception("You can only delete your own reviews"))
            }

            client.from("reviews")
                .delete {
                    filter {
                        eq("id", reviewId)
                    }
                }

            Log.d(TAG, "Review deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting review", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}

@Serializable
data class ReviewableUser(
    @SerialName("user_id")
    val userId: String,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("field_id")
    val fieldId: String
)
