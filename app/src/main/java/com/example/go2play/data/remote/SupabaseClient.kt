package com.example.go2play.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClient {

    private const val SUPABASE_URL = "https://mhxxoaotwqsguotcpvyp.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1oeHhvYW90d3FzZ3VvdGNwdnlwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI5ODU2NDgsImV4cCI6MjA3ODU2MTY0OH0.Un_whSuNXBlJkjzP741qcaQ_BvQhcM-Ruu0dZGH4Hfs"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth.Companion)
        install(Postgrest.Companion)
        install(Storage.Companion)

        // AGGIUNGI QUESTA CONFIGURAZIONE
        defaultSerializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true  // <-- QUESTO È IMPORTANTE
                isLenient = true
                coerceInputValues = true
            }
        )
    }

}