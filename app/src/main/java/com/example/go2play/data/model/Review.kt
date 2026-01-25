package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    @SerialName("reviewer_id")
    val reviewerId: String,
    @SerialName("reviewer_user_id")
    val reviewerUserId: String,
    @SerialName("event_id")
    val eventId: String,
    val sportsmanship: Int,
    val punctuality: Int,
    val reliability: Int,
    @SerialName("created_at")
    val createdAt: String?
)

@Serializable
data class ReviewCreate(
    @SerialName("reviewer_id")
    val reviewerId: String,
    @SerialName("reviewed_user_id")
    val reviewedUserId: String,
    @SerialName("event_id")
    val eventId: String,
    val sportsmanship: Int,
    val punctuality: Int,
    val reliability: Int
)

@Serializable
data class ReviewStats(
    @SerialName("avg_sportsmanship")
    val avgSportsmanship: Double,
    @SerialName("avg_punctuality")
    val avgPunctuality: Double,
    @SerialName("avg_reliability")
    val avgReliability: Double,
    @SerialName("total_reviews")
    val totalReviews: Int
)

@Serializable
data class UserReviewWithDetails(
    val review: Review,
    @SerialName("reviewer_username")
    val reviewerUsername: String,
    @SerialName("reviewer_avatar_url")
    val reviewerAvatarUrl: String? = null,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("field_name")
    val fieldName: String
)