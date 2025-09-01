package com.example.go2playproject.model

import com.google.firebase.firestore.DocumentId

data class Group(
    @DocumentId
    val groupId: String = "",
    val name: String = "",
    val members: List<String> = emptyList(), // Lista di userId dei membri del gruppo
    val matchesCreated: Int = 0,
    val creatorId: String = ""
)
