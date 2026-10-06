package com.example.mediq.data.repository

import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.UpdateProfileDto
import com.example.mediq.data.api.call
import com.example.mediq.data.api.toDomain
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.repository.ProfileRepository

/**
 * Live implementation of [ProfileRepository] backed by the Ktor server.
 *
 * Both operations require an active Bearer token (injected by
 * [com.example.mediq.data.api.AuthInterceptor]) and propagate [ApiFailure] to
 * the caller — a profile that can't load should show an error, not silently
 * disappear.
 */
class RetrofitProfileRepository(
    private val api: MediQApiService,
) : ProfileRepository {

    override suspend fun getProfile(): UserProfile =
        call { api.getProfile() }.toDomain()

    override suspend fun updateProfile(profile: UserProfile): UserProfile =
        call {
            api.updateProfile(
                UpdateProfileDto(
                    fullName = profile.fullName,
                    email = profile.email.ifBlank { null },
                    mobileNumber = profile.mobileNumber,
                    dateOfBirth = profile.dateOfBirth.toString(),
                    sex = profile.sex?.wireValue,
                    address = profile.address,
                )
            )
        }.toDomain()
}
