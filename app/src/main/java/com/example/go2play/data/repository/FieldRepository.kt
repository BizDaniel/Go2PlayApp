package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.FieldCreate
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from

class FieldRepository {
    private val client = SupabaseClient.client

    // Ottieni tutti i campetti
    suspend fun getAllFields(): Result<List<Field>> {
        return try {
            val fields = client.from("fields")
                .select()
                .decodeList<Field>()

            Result.success(fields)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting fields", e)
            Result.failure(e)
        }
    }

    // Ottieni un campetto specifico
    suspend fun getFieldById(fieldId: String): Result<Field> {
        return try {
            val field = client.from("fields")
                .select {
                    filter {
                        eq("id", fieldId)
                    }
                }
                .decodeSingle<Field>()

            Result.success(field)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting field", e)
            Result.failure(e)
        }
    }

    // Crea un nuovo campetto (admin only - per popolare il database)
    suspend fun createField(fieldCreate: FieldCreate): Result<Field> {
        return try {
            val createdField = client.from("fields")
                .insert(fieldCreate)
                .decodeSingle<Field>()

            Result.success(createdField)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error creating field", e)
            Result.failure(e)
        }
    }

    // Cerca campetti per nome o indirizzo
    suspend fun searchFields(query: String): Result<List<Field>> {
        return try {
            if (query.isBlank()) {
                return getAllFields()
            }

            val fields = client.from("fields")
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("address", "%$query%")
                        }
                    }
                }
                .decodeList<Field>()

            Result.success(fields)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error searching fields", e)
            Result.failure(e)
        }
    }

    // Filtra campetti per capacità giocatori
    suspend fun getFieldsByCapacity(capacity: Int): Result<List<Field>> {
        return try {
            val fields = client.from("fields")
                .select {
                    filter {
                        eq("player_capacity", capacity)
                    }
                }
                .decodeList<Field>()

            Result.success(fields)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting fields by capacity", e)
            Result.failure(e)
        }
    }

    // Filtra campetti indoor/outdoor
    suspend fun getFieldsByIndoor(isIndoor: Boolean): Result<List<Field>> {
        return try {
            val fields = client.from("fields")
                .select {
                    filter {
                        eq("is_indoor", isIndoor)
                    }
                }
                .decodeList<Field>()

            Result.success(fields)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting fields by indoor", e)
            Result.failure(e)
        }
    }
}