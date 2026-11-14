package com.example.mediq.data.api

import com.example.mediq.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object SupabaseClient {
    /*
     * Supabase project connection details.
     *
     * Values come from BuildConfig (injected at build time) or JVM system properties
     * (-Dmediq.supabase.url / -Dmediq.supabase.anonKey). Do not hardcode keys.
     */
    private val SUPABASE_URL: String =
        System.getProperty("mediq.supabase.url")
            ?: BuildConfig.SUPABASE_URL

    private val SUPABASE_ANON_KEY: String =
        System.getProperty("mediq.supabase.anonKey")
            ?: BuildConfig.SUPABASE_ANON_KEY

    /** The OkHttp client configured for Supabase REST API calls. */
    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Base REST path for Supabase. */
    val restUrl: String = "$SUPABASE_URL/rest/v1"

    /** Auth base path. */
    val authUrl: String = "$SUPABASE_URL/auth/v1"

    /** The anon key used for unauthenticated requests. */
    val anonKey: String = SUPABASE_ANON_KEY

    /** The bearer token from the last sign-in. */
    @Volatile
    var accessToken: String? = null

    /** The refresh token from the last sign-in. */
    @Volatile
    var refreshToken: String? = null

    /** The auth user ID from the last sign-in, used to look up the profile without relying on RLS. */
    @Volatile
    var authUserId: String? = null
}