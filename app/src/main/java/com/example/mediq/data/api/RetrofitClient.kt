package com.example.mediq.data.api

import com.example.mediq.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /*
     * Debug builds: local Ktor server. Change DEBUG_BASE_URL to match your
     * dev machine (emulator uses 10.0.2.2; adb reverse forwards 8099).
     *
     * Release builds: https://mediq-ktor-server.onrender.com/
     *
     * Override with -Dmediq.api.baseUrl=... if needed.
     */
    private const val DEBUG_BASE_URL = "http://10.0.2.2:8099/"
    private val BASE_URL: String = System.getProperty("mediq.api.baseUrl") ?: if (BuildConfig.DEBUG) {
        DEBUG_BASE_URL
    } else {
        "https://mediq-ktor-server.onrender.com/"
    }

    /**
     * The bearer token from the last sign-in.
     *
     * Held here rather than in a ViewModel because the OkHttp client is the
     * one that has to put it on every request.
     */
    @Volatile
    var accessToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = accessToken
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: MediQApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MediQApiService::class.java)
    }
}