package com.example.go2play.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.SurfaceType

@Entity(tableName = "fields")
data class FieldEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val surfaceType: String,
    val playerCapacity: Int,
    val isIndoor: Boolean,
    val pricePerPerson: Double?,
    val description: String?,
    val imageUrl: String?,
    val lat: Double?,
    val lng: Double?,
    val cachedAt: Long = System.currentTimeMillis()
)

// Funzioni di conversione
fun Field.toEntity() = FieldEntity(
    id = id,
    name = name,
    address = address,
    surfaceType = surface.name,
    playerCapacity = playerCapacity,
    isIndoor = isIndoor,
    pricePerPerson = pricePerPerson,
    description = description,
    imageUrl = imageUrl,
    lat = lat,
    lng = lng,
    cachedAt = System.currentTimeMillis()
)

fun FieldEntity.toModel() = Field(
    id = id,
    name = name,
    address = address,
    surface = SurfaceType.valueOf(surfaceType),
    playerCapacity = playerCapacity,
    isIndoor = isIndoor,
    pricePerPerson = pricePerPerson,
    description = description,
    imageUrl = imageUrl,
    createdAt = null,
    lat = lat,
    lng = lng
)