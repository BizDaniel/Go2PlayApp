package com.example.go2play.data.repository

import android.util.Log
import com.example.go2play.data.local.dao.FieldDao
import com.example.go2play.data.local.entity.toEntity
import com.example.go2play.data.local.entity.toModel
import com.example.go2play.data.model.Field
import com.example.go2play.data.model.FieldCreate
import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldRepository @Inject constructor(
    private val fieldDao: FieldDao
){
    private val client = SupabaseClient.client

    companion object {
        private const val CACHE_VALIDITY_HOURS = 24L
        private const val TAG = "FieldRepository"
    }

    /** Get all fields with cache-first strategy
     * 1. Check the local cache
     * 2. If the cache is valid (< 24h), we use that
     * 3. Otherwise, fetch from Supabase and update the cache
     */
    suspend fun getAllFields(): Result<List<Field>> {
        return try {
            // Check cache validity
            val cacheValidityTimestamp = System.currentTimeMillis() - (CACHE_VALIDITY_HOURS * 60 * 60 * 1000)
            val cacheCount = fieldDao.countValidCache(cacheValidityTimestamp)

            if (cacheCount > 0) {
                // Cache is valid, we use local data
                Log.d(TAG, "Using cached fields ($cacheCount items)")
                val cachedFields = fieldDao.getAllFieldsOnce().map { it.toModel() }
                return Result.success(cachedFields)
            }

            // Cache not valid or empty, fetch from Supabase
            Log.d(TAG, "Fetching fields from Supabase")
            val fields = client.from("fields")
                .select()
                .decodeList<Field>()

            val entities = fields.map { it.toEntity() }
            fieldDao.deleteAll()
            fieldDao.insertAll(entities)

            Log.d(TAG, "Cached ${fields.size} fields")
            Result.success(fields)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting fields", e)

            // Fallback: try to use the cache although is expired
            try {
                val cachedFields = fieldDao.getAllFieldsOnce().map { it.toModel() }
                if (cachedFields.isNotEmpty()) {
                    Log.d(TAG, "USing expired cache as fallback (${cachedFields.size} items")
                    return Result.success(cachedFields)
                }
            } catch (cacheError: Exception) {
                Log.e(TAG, "Cache fallback failed", cacheError)
            }
            Result.failure(e)
        }
    }

    /**
     * Flow to observe the fields from local cache
     */
    fun observeAllFields(): Flow<List<Field>> {
        return fieldDao.getAllFields().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Get a specific field
     */
    suspend fun getFieldById(fieldId: String): Result<Field> {
        return try {
            val cachedField = fieldDao.getFieldById(fieldId)

            if (cachedField != null) {
                Log.d(TAG, "Field $fieldId found in cache")
                return Result.success(cachedField.toModel())
            }

            // fetch from SUpabase if not in cache
            Log.d(TAG, "Fetching field $fieldId from Supabase")
            val field = client.from("fields")
                .select {
                    filter {
                        eq("id", fieldId)
                    }
                }
                .decodeSingle<Field>()

            fieldDao.insert(field.toEntity())

            Result.success(field)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error getting field", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new field, (not used)
     */
    /*suspend fun createField(fieldCreate: FieldCreate): Result<Field> {
        return try {
            val createdField = client.from("fields")
                .insert(fieldCreate)
                .decodeSingle<Field>()

            Result.success(createdField)
        } catch (e: Exception) {
            Log.e("FieldRepository", "Error creating field", e)
            Result.failure(e)
        }
    }*/

    /**
     * Search fields method
     */
    suspend fun searchFields(query: String): Result<List<Field>> {
        return try {
            if (query.isBlank()) {
                return getAllFields()
            }

            val cachedFields = fieldDao.getAllFieldsOnce()

            if (cachedFields.isNotEmpty()) {
                // Filter the cache locally
                val filteredFields = cachedFields
                    .filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.address.contains(query, ignoreCase = true)
                    }
                    .map { it.toModel() }

                Log.d(TAG, "Search in cache: found ${filteredFields.size} results")
                return Result.success(filteredFields)
            }

            // Search in Supabase
            Log.d(TAG, "Searching fields on Supabase")
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

    /**
     * Filter the fields by players capacity (not used yet)
     */
    /*suspend fun getFieldsByCapacity(capacity: Int): Result<List<Field>> {
        return try {

            val cachedFields = fieldDao.getAllFieldsOnce()

            if (cachedFields.isNotEmpty()) {
                val filteredFields = cachedFields
                    .filter { it.playerCapacity == capacity }
                    .map { it.toModel() }

                Log.d(TAG, "Filter by capacity in cache: ${filteredFields.size} results")
                return Result.success(filteredFields)
            }

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
    }*/

    /**
     * Filter fields by outdoor or indoor (not used yet)
     */
    /*suspend fun getFieldsByIndoor(isIndoor: Boolean): Result<List<Field>> {
        return try {

            val cachedFields = fieldDao.getAllFieldsOnce()

            if (cachedFields.isNotEmpty()) {
                val filteredFields = cachedFields
                    .filter { it.isIndoor == isIndoor }
                    .map { it.toModel() }

                Log.d(TAG, "Filter by indoor in cache: ${filteredFields.size} results")
                return Result.success(filteredFields)
            }

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
    }*/

    /**
     * Force cache refresh, ignoring the validity
     */
    suspend fun forceRefresh(): Result<List<Field>> {
        return try {
            Log.d(TAG, "Force refreshing fields from Supabase")

            val fields = client.from("fields")
                .select()
                .decodeList<Field>()

            val entities = fields.map { it.toEntity() }
            fieldDao.deleteAll()
            fieldDao.insertAll(entities)

            Log.d(TAG, "Force refresh completed: ${fields.size} fields")
            Result.success(fields)
        } catch (e: Exception) {
            Log.e(TAG, "Error force refreshing", e)
            Result.failure(e)
        }
    }

    /**
     * Cache management functions
     */
    suspend fun clearCache() {
        try {
            fieldDao.deleteAll()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    suspend fun getCacheInfo(): CacheInfo {
        return try {
            val count = fieldDao.count()
            val latestCache = fieldDao.getLatestCacheTime() ?: 0L
            val ageHours = (System.currentTimeMillis() - latestCache) / (60 * 60 * 1000)

            CacheInfo(
                itemCount = count,
                ageHours = ageHours,
                isValid = ageHours < CACHE_VALIDITY_HOURS
            )
        } catch (e: Exception) {
            CacheInfo(0, 0, false)
        }
    }
}

data class CacheInfo(
    val itemCount: Int,
    val ageHours: Long,
    val isValid: Boolean
)