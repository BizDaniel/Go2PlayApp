package com.example.go2playproject.model

import com.google.firebase.firestore.DocumentId

data class Field(

    /*
    * DocumentId è una annotazione di Firestore che mappa
    * l'ID del documento a questa proprietà DocumentID
    * */

    @DocumentId
    val fieldId: String = "",
    val name: String = "",
    val address: String = "",
    val courtType: String = "", // Es. "Sintetico"
    val pricePerHour: Double = 0.0,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true

    /*
    * La disponibilità può essere gestita come un Map<String, List<Long>>
    dove la chiave è la data e il valore sono gli orari prenotati
    *
    * */
)
