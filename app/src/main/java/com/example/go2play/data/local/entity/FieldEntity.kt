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
    val surface: String,
    val playerCapacity: Int,
    val isIndoor: Boolean,
    val pricePerPerson: Double?,
    val description: String?,
    val imageUrl: String?,
    val createdAt: String?,
    val lat: Double?,
    val lng: Double?
)

fun FieldEntity.toField() = Field(
    id = id,
    name = name,
    address = address,
    surface = SurfaceType.valueOf(surface),
    playerCapacity = playerCapacity,
    isIndoor = isIndoor,
    pricePerPerson = pricePerPerson,
    description = description,
    imageUrl = imageUrl,
    createdAt = createdAt,
    lat = lat,
    lng = lng
)

fun Field.toEntity() = FieldEntity(
    id = id,
    name = name,
    address = address,
    surface = surface.name,
    playerCapacity = playerCapacity,
    isIndoor = isIndoor,
    pricePerPerson = pricePerPerson,
    description = description,
    imageUrl = imageUrl,
    createdAt = createdAt,
    lat = lat,
    lng = lng
)