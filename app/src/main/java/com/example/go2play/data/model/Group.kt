package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("creator_id")
    val creatorId: String,
    @SerialName("member_ids")
    val memberIDs: List<String> = emptyList(),
    @SerialName("group_image_url")
    val groupImageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class GroupCreate(
    val name: String,
    val description: String? = null,
    @SerialName("creator_id")
    val creatorId: String,
    @SerialName("member_ids")
    val memberIDs: List<String>,
    @SerialName("group_image_url")
    val groupImageUrl: String? = null
)
