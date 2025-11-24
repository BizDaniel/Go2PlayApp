package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val age: Int? = null,
    val level: String? = null,
    @SerialName("preferred_roles")
    val preferredRoles: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("groups_ids")
    val groupIds: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ProfileUpdate(
    val username: String? = null,
    val age: Int? = null,
    val level: String? = null,
    @SerialName("preferred_roles")
    val preferredRoles: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)