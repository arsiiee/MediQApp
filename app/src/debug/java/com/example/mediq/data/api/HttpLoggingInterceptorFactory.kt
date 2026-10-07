package com.example.mediq.data.api

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Debug-only implementation: returns a BODY-level logging interceptor.
 *
 * The release source set provides a no-op version of this function so the
 * OkHttpClient builder code in RetrofitClient stays identical across variants.
 * The okhttp-logging artifact is declared as debugImplementation, so this file
 * only compiles in debug builds where the class is on the classpath.
 */
internal fun buildLoggingInterceptor(): Interceptor =
    HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
