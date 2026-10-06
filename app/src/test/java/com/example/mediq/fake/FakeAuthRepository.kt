package com.example.mediq.fake

import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.domain.repository.AuthRepository

/**
 * A hand-written [AuthRepository] for ViewModel tests.
 *
 * No mocking framework, matching the convention the `data/api` tests already
 * set: the thing being tested is a state machine, and a fake that records what
 * it was asked for says more about that state machine than a verify-count would.
 *
 * Each wizard call has a settable result *and* a settable error. The error is
 * what the interesting tests need — `ApiFailure` carries the sentence the server
 * wrote for a patient, and whether a ViewModel surfaces it or swallows it is
 * exactly what `AGENTS.md`'s error contract is about.
 */
class FakeAuthRepository : AuthRepository {

    // --- Settable outcomes ---------------------------------------------------

    var requestOtpResult: String = "123456"
    var requestOtpError: Throwable? = null

    var verifyOtpResult: String = "reg_test-registration-id"
    var verifyOtpError: Throwable? = null

    var registerResult: AuthSession = TestFixtures.authSession()
    var registerError: Throwable? = null

    // --- What the ViewModel asked for ---------------------------------------

    val otpRequests = mutableListOf<RequestOtpRequest>()
    val verifyRequests = mutableListOf<VerifyOtpRequest>()
    val registerRequests = mutableListOf<RegisterRequest>()

    /** True when no registration call reached the repository at all. */
    val registerWasCalled: Boolean get() = registerRequests.isNotEmpty()

    override suspend fun signIn(request: SignInRequest): AuthSession =
        TestFixtures.authSession()

    override suspend fun signOut() = Unit

    override suspend fun currentSession(): AuthSession? = null

    override suspend fun requestOtp(request: RequestOtpRequest): String {
        otpRequests += request
        requestOtpError?.let { throw it }
        return requestOtpResult
    }

    override suspend fun verifyOtp(request: VerifyOtpRequest): String {
        verifyRequests += request
        verifyOtpError?.let { throw it }
        return verifyOtpResult
    }

    override suspend fun register(request: RegisterRequest): AuthSession {
        registerRequests += request
        registerError?.let { throw it }
        return registerResult
    }
}
