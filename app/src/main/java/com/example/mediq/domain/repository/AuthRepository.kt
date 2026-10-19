package com.example.mediq.domain.repository

import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.VerifyOtpRequest

/**
 * Sign-in, sign-out, and registration.
 *
 * Registration is three steps because the phone number is verified before the
 * account exists: [requestOtp] sends a code, [verifyOtp] checks it and returns
 * a registration id, and [register] turns that id into a session.
 */
interface AuthRepository {

    suspend fun signIn(request: SignInRequest): AuthSession

    suspend fun signOut()

    /** The stored session, or null if nobody is signed in. */
    suspend fun currentSession(): AuthSession?

    suspend fun requestOtp(request: RequestOtpRequest): String

    suspend fun verifyOtp(request: VerifyOtpRequest): String

    suspend fun register(request: RegisterRequest): AuthSession

    /**
     * Rewrites the cached session's profile after a successful edit.
     *
     * [currentSession] reads the local store rather than the server, so without
     * this the profile screen would keep showing the pre-edit values until the
     * next sign-in even though the save succeeded. The token and its expiry are
     * left exactly as they were — an edit is not a re-authentication, and
     * extending a session here would outlive what the server believes.
     *
     * Implementations with no stored session do nothing: there is no token to
     * attach a profile to, and inventing one would hand the app a session the
     * server never issued.
     */
    suspend fun updateCachedProfile(profile: UserProfile)
}