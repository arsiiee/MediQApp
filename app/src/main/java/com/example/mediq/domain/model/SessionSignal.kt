package com.example.mediq.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A central signal tracking whether the current session has expired.
 *
 * Before this existed, five screens each rendered an unactionable
 * "Please sign in again." message with no way to act on it from that screen,
 * forcing the patient to find the Profile screen to sign in again.
 *
 * This signal provides a clean seam between `data/` (which detects 401s via
 * OkHttp Authenticator) and `ui/` (which observes it at the nav host level
 * to prompt for re-authentication), without violating architecture boundaries.
 *
 * It starts `false`; a fresh install and a signed-out patient are indistinguishable
 * to the nav host until an active session receives a 401.
 */
class SessionSignal {

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    /**
     * Flags the current session as expired. Idempotent: repeated calls
     * across concurrent 401 responses maintain the true state without
     * emitting extra transitions.
     */
    fun markExpired() {
        _isExpired.value = true
    }

    /**
     * Clears the expired state, resetting the signal back to false.
     * Called when the re-authentication modal resolves or on sign-out.
     */
    fun clear() {
        _isExpired.value = false
    }
}
