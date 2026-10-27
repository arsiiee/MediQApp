package com.example.mediq.service

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

class AdminService(
    private val adminRepository: AdminRepository,
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository,
) {
    suspend fun getAllAccounts(): LoadState<AdminAccountsResponse> = adminRepository.getAllAccounts()

    suspend fun createStaffAccount(kind: String, draft: StaffDraft): LoadState<UserProfile> {
        val resolvedKind = kind.ifBlank { draft.role ?: "doctor" }
        val password = draft.password
            ?.takeIf { it.isNotBlank() && it.length >= 8 }
            ?: return LoadState.Error(ApiFailure.validation("Password is required and must be at least 8 characters"))
        val name = draft.fullName ?: draft.name ?: ""
        val role = when (resolvedKind) {
            "doctor" -> UserRole.DOCTOR
            "secretary" -> UserRole.SECRETARY
            else -> throw IllegalArgumentException("Invalid staff kind: $resolvedKind")
        }
        val registrationDraft = RegistrationDraft(
            username = draft.username,
            password = password,
            fullName = name,
            email = draft.email,
            mobileNumber = draft.mobileNumber ?: "+639000000000",
            dateOfBirth = kotlinx.datetime.LocalDate.parse(draft.dateOfBirth ?: "1990-01-01"),
            role = role,
            practice = if (resolvedKind == "doctor") draft.practice?.let {
                DoctorPractice(
                    specializationId = it.specializationId,
                    licenseNumber = it.licenseNumber,
                    yearsExperience = it.yearsExperience,
                    feeCentavos = it.feeCentavos,
                    bio = it.bio,
                    building = it.building,
                    floor = it.floor,
                    room = it.room,
                    languages = it.languages.map { lang -> com.example.mediq.domain.model.ConsultationLanguage(lang, lang) },
                )
            } else null,
            assignedDoctorId = if (resolvedKind == "secretary") draft.assignedDoctorId else null,
        )
        return adminRepository.createStaffAccount(registrationDraft)
    }

    suspend fun updateAccountStatus(userId: String, status: AccountStatus): LoadState<UserProfile> = adminRepository.updateAccountStatus(userId, status)

    suspend fun reassignSecretaryDoctor(userId: String, assignedDoctorId: String?): LoadState<UserProfile> = adminRepository.reassignSecretaryDoctor(userId, assignedDoctorId)

    suspend fun getDoctorsForAdmin(): LoadState<List<Doctor>> = adminRepository.getDoctorsForAdmin()
}

@Serializable
data class StaffDraft(
    val username: String,
    val name: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null,
    val dateOfBirth: String? = null,
    val temporaryPassword: String? = null,
    val password: String? = null,
    val role: String? = null,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val practice: DoctorPracticeDraft? = null,
    val assignedDoctorId: String? = null,
)

@Serializable
data class DoctorPracticeDraft(
    val specializationId: String,
    val licenseNumber: String,
    val yearsExperience: Int,
    val feeCentavos: Long,
    val bio: String,
    val building: String,
    val floor: String,
    val room: String,
    val languages: List<String>,
)