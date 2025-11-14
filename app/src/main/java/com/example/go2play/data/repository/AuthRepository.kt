package com.example.go2play.data.repository

import com.example.go2play.data.remote.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email


class AuthRepository {

    private val client = SupabaseClient.client

    // Registrazione con email e password
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
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
}

