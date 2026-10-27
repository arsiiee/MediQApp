package com.example.mediq.server

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.util.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class InMemoryAdminRepository(
    private val authRepository: InMemoryAuthRepository,
    private val doctorRepository: InMemoryDoctorRepository,
) : AdminRepository {
    override suspend fun getAllAccounts(): LoadState<AdminAccountsResponse> {
        val allUsers = authRepository.users.value.values
        return LoadState.Success(AdminAccountsResponse(
            patients = allUsers.filter { it.role == UserRole.PATIENT }.toList(),
            doctors = allUsers.filter { it.role == UserRole.DOCTOR }.toList(),
            secretaries = allUsers.filter { it.role == UserRole.SECRETARY }.toList(),
        ))
    }

    override suspend fun createStaffAccount(draft: RegistrationDraft): LoadState<UserProfile> {
        // This creates a user profile and potentially a doctor record
        val passwordHash = authRepository.passwordHash.hash(draft.password)
        val profile = UserProfile(
            id = java.util.UUID.randomUUID().toString(),
            username = draft.username,
            fullName = draft.fullName,
            email = draft.email,
            mobileNumber = draft.mobileNumber,
            dateOfBirth = draft.dateOfBirth,
            role = draft.role,
            status = AccountStatus.ACTIVE,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        )
        authRepository.users.update { it + (profile.id to profile) }
        authRepository.passwordHashes.update { it + (profile.id to passwordHash) }

        if (draft.role == UserRole.DOCTOR && draft.practice != null) {
            val practice = draft.practice
            val doctor = Doctor(
                id = java.util.UUID.randomUUID().toString(),
                userId = profile.id,
                fullName = draft.fullName,
                specialty = "TBD",
                specializationId = practice.specializationId,
                licenseNumber = practice.licenseNumber,
                yearsOfExperience = practice.yearsExperience,
                feeCentavos = practice.feeCentavos,
                bio = practice.bio,
                location = ClinicLocation(practice.building, practice.floor, practice.room),
                languages = practice.languages,
                status = DoctorStatus.ACTIVE,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            doctorRepository.doctors.update { it + (doctor.id to doctor) }
            // Update profile with doctorId
            val updatedProfile = profile.copy(doctorId = doctor.id)
            authRepository.users.update { it + (profile.id to updatedProfile) }
            return LoadState.Success(updatedProfile)
        } else if (draft.role == UserRole.SECRETARY) {
            val updatedProfile = profile.copy(assignedDoctorId = draft.assignedDoctorId)
            authRepository.users.update { it + (profile.id to updatedProfile) }
            return LoadState.Success(updatedProfile)
        }

        return LoadState.Success(profile)
    }

    override suspend fun updateAccountStatus(userId: String, status: AccountStatus): LoadState<UserProfile> {
        val profile = authRepository.users.value[userId]
            ?: return LoadState.Error(ApiFailure.notFound("User not found"))
        val updated = profile.copy(
            status = status,
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        )
        authRepository.users.update { it + (userId to updated) }
        return LoadState.Success(updated)
    }

    override suspend fun reassignSecretaryDoctor(userId: String, assignedDoctorId: String?): LoadState<UserProfile> {
        val profile = authRepository.users.value[userId]
            ?: return LoadState.Error(ApiFailure.notFound("User not found"))
        if (profile.role != UserRole.SECRETARY) {
            return LoadState.Error(ApiFailure.validation("Only secretaries can be reassigned to a doctor"))
        }
        if (assignedDoctorId != null) {
            val doctor = doctorRepository.findByUserId(assignedDoctorId).getOrNull()
                ?: return LoadState.Error(ApiFailure.validation("Doctor not found or not active"))
            if (doctor.status != DoctorStatus.ACTIVE) {
                return LoadState.Error(ApiFailure.validation("Doctor not found or not active"))
            }
        }
        val updated = profile.copy(
            assignedDoctorId = assignedDoctorId,
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        )
        authRepository.users.update { it + (userId to updated) }
        return LoadState.Success(updated)
    }

    override suspend fun getDoctorsForAdmin(): LoadState<List<Doctor>> {
        return doctorRepository.findAll(DoctorQuery(limit = 1000)).fold(
            onSuccess = { LoadState.Success(it.items) },
            onError = { LoadState.Error(it) },
        )
    }
}
