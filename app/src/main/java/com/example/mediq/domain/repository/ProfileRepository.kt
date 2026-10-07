package com.example.mediq.domain.repository

import com.example.mediq.domain.model.UserProfile

interface ProfileRepository {

    suspend fun getProfile(): UserProfile

    suspend fun updateProfile(profile: UserProfile): UserProfile
}