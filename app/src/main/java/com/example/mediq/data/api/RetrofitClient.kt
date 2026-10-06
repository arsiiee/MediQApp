package com.example.mediq.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds the OkHttp + Retrofit stack used by all Retrofit repositories.
 *
 * BASE_URL points at the PC running the server. Currently set to this PC's LAN
 * address, because the LAN address is the only one that works on *every* target
 * here — emulator and physical phone alike. Change it to match your test setup:
 *   - Emulator or physical device on this Wi-Fi : http://192.168.100.14:8099/  <- current
 *   - Emulator only                             : http://10.0.2.2:8099/
 *   - Physical device over ADB, no Wi-Fi        : http://127.0.0.1:8099/
 *     ...which additionally requires one command:
 *       adb reverse tcp:8099 tcp:8099
 *
 * Why the LAN address rather than 10.0.2.2: 10.0.2.2 is an alias that exists
 * only inside the emulator's virtual network. On a physical phone it is
 * unroutable, so every call times out and `call {}` reports "Couldn't reach the
 * clinic" — which reads like the server is down while the server is answering
 * 127.0.0.1 perfectly well. Check what `adb devices` actually lists before
 * trusting a network error: an emulator and a phone are both just "a device",
 * and moving between them changes nothing else. This exact mismatch is what
 * made sign-in fail on 2026-10-06 with the server up and healthy.
 *
 * Only 10.0.2.2, 127.0.0.1 and this LAN address are permitted for cleartext;
 * see res/xml/network_security_config.xml. This PC's address is DHCP-assigned
 * and changes when it joins a different network — `ipconfig` reads the current
 * one, and the config file must be updated in step, or cleartext is refused and
 * the app fails with the same misleading message.
 *
 * HTTP logging is injected via [buildLoggingInterceptor], which is defined
 * in two source sets: the debug variant returns a full BODY-level OkHttp
 * logger; the release variant returns a no-op pass-through. This keeps
 * okhttp-logging as debugImplementation with no classpath leak into release.
 */
object RetrofitClient {

    const val BASE_URL = "http://192.168.100.14:8099/"

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
