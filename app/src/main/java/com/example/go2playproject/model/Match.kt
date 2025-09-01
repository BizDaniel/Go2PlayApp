package com.example.go2playproject.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Match(
    @DocumentId
    val matchId: String = "",
    val fieldId: String = "",
    var creatorId: String = "",
    val date: Date = Date(),
    val timeSlot: String = "",
    val players: List<String> = emptyList(),
    val ispublic: Boolean,
    val maxPlayers: Int = 0,
    val groupId: String?= null, // L'ID del gruppo è opzionale per le partiute pubbliche
    val description: String = "",
    val level: String = "",
    val isCompleted: Boolean = false
)
