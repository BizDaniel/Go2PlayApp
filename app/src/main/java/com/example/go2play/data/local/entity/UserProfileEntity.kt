package com.example.go2play.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.go2play.data.model.UserProfile

@Entity(tableName = "profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val age: Int?,
    val level: String?,
    val preferredRoles: String?,
    val avatarUrl: String?,
    val groupIds: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    email = email,
    username = username,
    age = age,
    level = level,
    preferredRoles = preferredRoles,
    avatarUrl = avatarUrl,
    groupIds = groupIds?.joinToString(","),
    cachedAt = System.currentTimeMillis()
)

fun UserProfileEntity.toModel() = UserProfile(
    id = id,
    email = email,
    username = username,
    age = age,
    level = level,
    preferredRoles = preferredRoles,
    avatarUrl = avatarUrl,
    groupIds = groupIds?.split(",")?.filter { it.isNotBlank() },
    createdAt = null
)