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

    const val BASE_URL = "http://10.0.2.2:8099/"

    fun create(
        tokenStore: TokenStore,
        sessionSignal: com.example.mediq.domain.model.SessionSignal,
    ): MediQApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(SessionAuthenticator(tokenStore, sessionSignal))
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

/**
 * OkHttp [okhttp3.Authenticator] that handles HTTP 401 responses.
 *
 * Responsibilities in order:
 * 1. Clears [TokenStore] on every 401 response handled so a dead token is not re-sent.
 * 2. Signals the application via [com.example.mediq.domain.model.SessionSignal.markExpired].
 *    Because the signal is idempotent, concurrent 401s produce only one state transition.
 * 3. Caps retries per request: if the request has already attempted a 401 retry, returns null.
 * 4. Refuses to re-issue any non-idempotent write request (such as POST, PUT, DELETE, PATCH).
 *    In the past, auto-retrying non-idempotent writes without an idempotency key created
 *    a double-booking hazard where a patient could get booked twice if the first request
 *    succeeded on the server but the response was interrupted. Only safe read methods
 *    (GET, HEAD) are allowed to retry.
 */
class SessionAuthenticator(
    private val tokenStore: TokenStore,
    private val sessionSignal: com.example.mediq.domain.model.SessionSignal,
) : okhttp3.Authenticator {

    override fun authenticate(route: okhttp3.Route?, response: Response): okhttp3.Request? {
        // Guard against retry loops: a response carrying more than one prior 401 attempt returns null
        if (responseCount(response) >= 2) {
            tokenStore.clearSession()
            sessionSignal.markExpired()
            return null
        }

        // Only retry safe/idempotent read methods. Non-idempotent writes are NEVER auto-retried.
        val method = response.request.method
        if (method != "GET" && method != "HEAD") {
            tokenStore.clearSession()
            sessionSignal.markExpired()
            return null
        }

        // If the request already carried the newest token (or no new token has been supplied),
        // clear the dead token, signal expiry, and do not loop.
        val currentToken = tokenStore.getAccessToken()
        val requestAuth = response.request.header("Authorization")
        if (currentToken == null || requestAuth == "Bearer $currentToken") {
            tokenStore.clearSession()
            sessionSignal.markExpired()
            return null
        }

        // A new token is available in tokenStore (e.g. refreshed/re-authenticated)
        return response.request.newBuilder()
            .header("Authorization", "Bearer $currentToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
