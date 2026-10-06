package com.example.mediq.data.repository

import com.example.mediq.data.api.AuthSessionDto
import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.RegisterRequestDto
import com.example.mediq.data.api.RequestOtpRequestDto
import com.example.mediq.data.api.SignInRequestDto
import com.example.mediq.data.api.TokenStore
import com.example.mediq.data.api.VerifyOtpRequestDto
import com.example.mediq.data.api.call
import com.example.mediq.data.api.toDomain
import com.example.mediq.data.api.toDto
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.domain.repository.AuthRepository
import java.time.Instant

/**
 * Live implementation of [AuthRepository] backed by the Ktor server.
 *
 * After a successful sign-in or registration the full [AuthSessionDto] is
 * persisted in [TokenStore] (SharedPreferences). [currentSession] reads that
 * cached value without a network call, so the app stays signed in across
 * restarts until the token expires or the user signs out.
 *
 * Sign-out is best-effort: the server session is revoked if reachable, but
 * the local token is always cleared so the user can never be stuck on a
 * signed-in screen they can't leave.
 *
 * Failures leave as [ApiError] carrying the server's own message — "Those
 * details don't match an account.", not "HTTP 401 ".
 */
class RetrofitAuthRepository(
    private val api: MediQApiService,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override suspend fun signIn(request: SignInRequest): AuthSession {
        val dto = call { api.signIn(SignInRequestDto(request.username, request.password)) }
        tokenStore.saveSession(dto)
        return dto.toDomain()
    }

    override suspend fun signOut() {
        runCatching { call { api.signOut() } }
        tokenStore.clearSession()
    }

    override suspend fun currentSession(): AuthSession? {
        val dto = tokenStore.getSession() ?: return null
        val session = dto.toDomain()
        return if (session.isExpired(Instant.now())) {
            tokenStore.clearSession()
            null
        } else {
            session
        }
    }

    override suspend fun requestOtp(request: RequestOtpRequest): String {
        val dto = call { api.requestOtp(RequestOtpRequestDto(request.mobileNumber)) }
        // In dev mode the server returns the OTP code in the response body
        // (returnCodeToCaller = true). In production this will be null.
        return dto.code ?: ""
    }

    override suspend fun verifyOtp(request: VerifyOtpRequest): String {
        val map = call { api.verifyOtp(VerifyOtpRequestDto(request.mobileNumber, request.otp)) }
        return map["registrationId"]
            ?: throw ApiFailure(
                status = 200,
                code = ApiFailure.CODE_MALFORMED,
                message = "Verification didn't complete. Request a new code.",
            )
    }

    override suspend fun register(request: RegisterRequest): AuthSession {
        val dto = call { api.register(request.toDto()) }
        tokenStore.saveSession(dto)
        return dto.toDomain()
    }
}
