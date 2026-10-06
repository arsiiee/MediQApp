package com.example.mediq.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Release-only implementation: returns a pass-through interceptor that adds
 * no overhead and has no reference to the okhttp-logging artifact, which is
 * declared as debugImplementation and is absent from release classpaths.
 */
internal fun buildLoggingInterceptor(): Interceptor =
    Interceptor { chain: Interceptor.Chain -> chain.proceed(chain.request()) }
