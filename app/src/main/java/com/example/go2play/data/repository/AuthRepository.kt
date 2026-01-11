package com.example.go2play.data.repository

import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.classPropertyNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fieldRepository: FieldRepository,
    private val eventRepository: EventRepository
){

    private val client = SupabaseClient.client
    private var refreshJob: Job? = null

    // Registrazione con email, password e username
    suspend fun signUp(email: String, password: String, username: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // Ottieni l'ID dell'utente appena creato
            // L'utente appena creato è ora l'utente corrente nel client auth
            val userId = client.auth.currentUserOrNull()?.id ?: throw Exception("User ID not found after sign up")

            profileRepository.createProfile(userId, email, username)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login con email e password
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    suspend fun signOut(): Result<Unit> {
        return try {
            stopAutoRefresh()

            clearAllCache()

            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verifico se l'utente è loggato
    fun isUserLoggedIn(): Boolean {
        return client.auth.currentSessionOrNull() != null
    }

    // Ottieni l'email dell'utente corrente
    fun getCurrentUSerEmail(): String? {
        return client.auth.currentUserOrNull()?.email
    }

    // Verifica e ripristina la sessione
    suspend fun restoreSession(): Result<Boolean> {
        return try {
            // Supabase Kotlin gestisce automaticamente il refresh del token
            // Basta verificare se esiste una sessione valida
            val session = client.auth.currentSessionOrNull()

            Result.success(session != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSession(): Result<Unit> {
        return try {
            client.auth.refreshCurrentSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun startAutoRefresh(scope: CoroutineScope) {
        stopAutoRefresh()

        refreshJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(50 * 60 * 1000L)

                try {
                    if (isUserLoggedIn()) {
                        client.auth.refreshCurrentSession()
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Clear all the local cache
     */
    private suspend fun clearAllCache() {
        try {
            profileRepository.clearAllCache()
            eventRepository.clearAllEventsCache()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error clearing cache", e)
        }
    }
}

