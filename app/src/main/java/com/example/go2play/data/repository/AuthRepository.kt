package com.example.go2play.data.repository

import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class AuthRepository {

    private val client = SupabaseClient.client
    private val profileRepository = ProfileRepository()
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

            startAutoRefresh()

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

            startAutoRefresh()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    suspend fun signOut(): Result<Unit> {
        return try {

            stopAutoRefresh()

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

            if(session != null) {
                startAutoRefresh()
            }

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

    private fun startAutoRefresh() {
        stopAutoRefresh()

        refreshJob = CoroutineScope(Dispatchers.IO).launch {
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
}

