package com.example.go2play.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SupabaseClient {

    private const val SUPABASE_URL = "https://mhxxoaotwqsguotcpvyp.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1oeHhvYW90d3FzZ3VvdGNwdnlwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI5ODU2NDgsImV4cCI6MjA3ODU2MTY0OH0.Un_whSuNXBlJkjzP741qcaQ_BvQhcM-Ruu0dZGH4Hfs"

    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun initialize(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "supabase_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth) {
                flowType = FlowType.PKCE

                sessionManager = object : SessionManager {
                    override suspend fun saveSession(session: UserSession) {
                        val sessionJson = Json.encodeToString(session)
                        sharedPreferences.edit().putString("session", sessionJson).apply()
                    }

                    override suspend fun loadSession(): UserSession? {
                        val sessionJson = sharedPreferences.getString("session", null) ?: return null
                        return try {
                            Json.decodeFromString<UserSession>(sessionJson)
                        } catch (e: Exception) {
                            // Se il file è corrotto o vecchio, ritorniamo null così l'utente deve rifare il login
                            null
                        }
                    }

                    override suspend fun deleteSession() {
                        sharedPreferences.edit().remove("session").apply()
                    }
                }
            }

            install(Postgrest)
            install(Storage)

            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
            )
        }
    }
}