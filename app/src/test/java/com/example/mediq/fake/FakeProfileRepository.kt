package com.example.mediq.fake

import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.repository.ProfileRepository

/**
 * A hand-written [ProfileRepository] for ViewModel tests.
 *
 * Records the profile it was asked to save, which is the assertion that matters
 * for an update: a ViewModel that showed the edited name on screen while
 * submitting the original one would look correct in every state-only check.
 */
class FakeProfileRepository : ProfileRepository {

    // --- Settable outcomes ---------------------------------------------------

    var profile: UserProfile = TestFixtures.userProfile()
    var getProfileError: Throwable? = null
    var updateProfileError: Throwable? = null

    /** What `updateProfile` returns, so a server-side normalisation can be simulated. */
    var updateResult: UserProfile = profile

    // --- What the ViewModel asked for ---------------------------------------

    val updateRequests = mutableListOf<UserProfile>()

    /** True when no read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = readRequested || updateRequests.isNotEmpty()

    /** True when no mutation reached the repository at all. */
    val wasMutated: Boolean get() = updateRequests.isNotEmpty()

    var readRequested = false
        private set

    override suspend fun getProfile(): UserProfile {
        readRequested = true
        getProfileError?.let { throw it }
        return profile
    }

    override suspend fun updateProfile(profile: UserProfile): UserProfile {
        updateRequests += profile
        updateProfileError?.let { throw it }
        return updateResult
    }
}
