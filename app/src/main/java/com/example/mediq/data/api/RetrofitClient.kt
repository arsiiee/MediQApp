package com.example.mediq.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds the OkHttp + Retrofit stack used by all Retrofit repositories.
 *
 * BASE_URL points at the PC running Android Studio. Currently set for the
 * Android emulator, which reaches the host through 10.0.2.2 and needs no extra
 * setup. Change it to match your test setup:
 *   - Emulator                     : http://10.0.2.2:8099/   <- current
 *   - Physical device (ADB reverse): http://127.0.0.1:8099/
 *     ...which additionally requires one command:
 *       adb reverse tcp:8099 tcp:8099
 *   - Physical device on LAN       : http://<host-IP>:8099/
 *
 * Only 10.0.2.2, 127.0.0.1 and the LAN host address are permitted for
 * cleartext; see res/xml/network_security_config.xml. A LAN host address
 * needs adding there too, and the phone must be able to reach this PC on
 * the network (Windows Firewall blocks inbound on port 8099 by default).
 *
 * HTTP logging is injected via [buildLoggingInterceptor], which is defined
 * in two source sets: the debug variant returns a full BODY-level OkHttp
 * logger; the release variant returns a no-op pass-through. This keeps
 * okhttp-logging as debugImplementation with no classpath leak into release.
 */
object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:8099/"

    fun create(tokenStore: TokenStore): MediQApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(buildLoggingInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MediQApiService::class.java)
    }
}

/**
 * OkHttp interceptor that injects the Bearer token stored in [TokenStore]
 * into every outgoing request. Requests made before sign-in (token == null)
 * are forwarded without an Authorization header — the server accepts them on
 * public routes and rejects them on protected routes.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
