package com.example.mediq.data.api

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
     * The device's own loopback, forwarded to the developer machine by
     * `adb reverse tcp:8099 tcp:8099`.
     *
     * The emulator's 10.0.2.2 alias would also reach the machine, but the
     * host firewall blocks inbound connections to a server that has no
     * allow-rule for it, and that path is closed. The adb forward travels
     * over the adb connection the device already has, and works the same on
     * an emulator and on a physical handset.
     */
    private const val BASE_URL = "http://127.0.0.1:8099/"

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