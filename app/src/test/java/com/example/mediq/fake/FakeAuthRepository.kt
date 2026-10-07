package com.example.mediq.fake

import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred

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

    /**
     * The session `signIn` returns, and how it can fail.
     *
     * The default is a real session so a ViewModel that only checks "did it
     * succeed" is not forced to think about the payload; `currentSessionResult`
     * is null by default because "nobody is signed in" is the honest starting
     * state of a fresh app.
     */
    var signInResult: AuthSession = TestFixtures.authSession()
    var signInError: Throwable? = null

    /** What `currentSession()` returns. Null means nobody is signed in. */
    var currentSessionResult: AuthSession? = null
    var currentSessionError: Throwable? = null

    var signOutError: Throwable? = null

    var requestOtpResult: String = "123456"
    var requestOtpError: Throwable? = null

    var verifyOtpResult: String = "reg_test-registration-id"
    var verifyOtpError: Throwable? = null

    var registerResult: AuthSession = TestFixtures.authSession()
    var registerError: Throwable? = null

    // --- What the ViewModel asked for ---------------------------------------

    val signInRequests = mutableListOf<SignInRequest>()
    val otpRequests = mutableListOf<RequestOtpRequest>()
    val verifyRequests = mutableListOf<VerifyOtpRequest>()
    val registerRequests = mutableListOf<RegisterRequest>()

    /** True when no registration call reached the repository at all. */
    val registerWasCalled: Boolean get() = registerRequests.isNotEmpty()

    /** True when no sign-in reached the repository at all. */
    val signInWasCalled: Boolean get() = signInRequests.isNotEmpty()

    /** True when `signOut` reached the repository at all. */
    val signOutWasCalled: Boolean get() = signOutCount > 0

    var signOutCount = 0
        private set

    /**
     * When set, `signIn` waits on this before answering.
     *
     * `MainDispatcherRule` installs an `UnconfinedTestDispatcher`, which runs a
     * launched coroutine eagerly to completion — so without a gate a fake call
     * never really suspends, `isLoading` is already `false` by the time the next
     * line of the test runs, and a test meant to cover "a second submit while one
     * is in flight" ends up measuring the test dispatcher instead of the
     * ViewModel's guard. Completing this gate is what makes the request genuinely
     * in flight.
     */
    var signInGate: CompletableDeferred<Unit>? = null

    override suspend fun signIn(request: SignInRequest): AuthSession {
        signInRequests += request
        signInGate?.await()
        signInError?.let { throw it }
        return signInResult
    }

    override suspend fun signOut() {
        signOutCount++
        signOutError?.let { throw it }
    }

    override suspend fun currentSession(): AuthSession? {
        currentSessionError?.let { throw it }
        return currentSessionResult
    }

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
