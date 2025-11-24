package com.example.go2play.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Field(
    val id: String,
    val name: String,
    val address: String,
    @SerialName("surface_type")
    val surface: SurfaceType,
    @SerialName("player_capacity")
    val playerCapacity: Int,
    @SerialName("is_indoor")
    val isIndoor: Boolean = false,
    @SerialName("price_per_person")
    val pricePerPerson: Double? = null,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
enum class SurfaceType {
    @SerialName("synthetic_grass")
    SYNTHETIC_GRASS,

    @SerialName("natural_grass")
    NATURAL_GRASS,

    @SerialName("parquet")
    PARQUET,

    @SerialName("cement")
    CEMENT,

    @SerialName("indoor_synthetic")
    INDOOR_SYNTHETIC
}

@Serializable
data class FieldCreate(
    val name: String,
    val address: String,
    @SerialName("surface_type")
    val surfaceType: SurfaceType,
    @SerialName("player_capacity")
    val playerCapacity: Int,
    @SerialName("is_indoor")
    val isIndoor: Boolean = false,
    @SerialName("price_per_person")
    val pricePerPerson: Double? = null,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null
)

// Dati predefiniti per i campetti di Trento
/*object TrentoFields {
    val fields = listOf(
        FieldCreate(
            name = "Complesso sportivo Trento Nord",
            address = "Via del Brennero, 276, 38121 Trento TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 5,
            isIndoor = false,
            pricePerPerson = 3.5,
            description = "Campo da calcio a 5 in erba sintetica"
        ),
        FieldCreate(
            name = "Campo calcetto UniTrento - Opera Universitaria",
            address = "Via della Malpensada, 162, 38123 Trento TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 5,
            isIndoor = false,
            pricePerPerson = 0.0,
            description = "Campo da calcio a 5 in erba sintetica"
        ),
        FieldCreate(
            name = "Centro Sportivo Universitario di Mattarello",
            address = "Via delle Regole, 38123 Mattarello TN",
            surfaceType = SurfaceType.PARQUET,
            playerCapacity = 5,
            isIndoor = true,
            pricePerPerson = 3.0,
            description = "Campo da calcio a 5 in parquet al coperto"
        ),
        FieldCreate(
            name = "Campetto dell'Oratorio di Povo",
            address = "Via dei Rivi, 4, 38123 Trento TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 5,
            isIndoor = false,
            pricePerPerson = 5.0,
            description = "Campo da calcio a 5 in erba sintetica"
        ),
        FieldCreate(
            name = "Centro sportivo Don Onorio Spada",
            address = "Via Valnigra, 69, 38123 Villazzano TN",
            surfaceType = SurfaceType.PARQUET,
            playerCapacity = 5,
            isIndoor = true,
            pricePerPerson = 5.0,
            description = "Campo da calcio a 5 in parquet al coperto"
        ),
        FieldCreate(
            name = "Campetto Collegio Clesio",
            address = "Via Santa Margherita, 3/15, 38122 Trento TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 5,
            isIndoor = false,
            pricePerPerson = 0.0,
            description = "Campo da calcio a 5 in erba sintetica"
        ),
        FieldCreate(
            name = "Campo da calcio San Donà",
            address = "Via per Cognola, 51, 38122 Trento TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 7,
            isIndoor = false,
            pricePerPerson = 3.5,
            description = "Campo da calcio a 7 in erba sintetica"
        ),
        FieldCreate(
            name = "Campo da calcio in località Sardagna",
            address = "Via alla Césa Vècia, 1, 38123 Sardagna TN",
            surfaceType = SurfaceType.SYNTHETIC_GRASS,
            playerCapacity = 7,
            isIndoor = false,
            pricePerPerson = 5.0,
            description = "Campo da calcio a 7 in erba sintetica"
        )
    )
}*/