package com.example.go2playproject.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val groupsId: List<String> = emptyList(),
    val profileImageUrl: String? = null, // URL dell'immagine del profilo
    val mymatches: Int = 0
)
