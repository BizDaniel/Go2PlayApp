package com.example.go2play.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
