package com.example.mediq.fake

import com.example.mediq.data.api.AckDto
import com.example.mediq.data.api.AppointmentDto
import com.example.mediq.data.api.AuthSessionDto
import com.example.mediq.data.api.AvailableDateDto
import com.example.mediq.data.api.BookingRequestDto
import com.example.mediq.data.api.DoctorDto
import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.NotificationDto
import com.example.mediq.data.api.OtpRequestedDto
import com.example.mediq.data.api.PagedDto
import com.example.mediq.data.api.RegisterRequestDto
import com.example.mediq.data.api.RequestOtpRequestDto
import com.example.mediq.data.api.RescheduleRequestDto
import com.example.mediq.data.api.SignInRequestDto
import com.example.mediq.data.api.TimeSlotDto
import com.example.mediq.data.api.UpdateProfileDto
import com.example.mediq.data.api.UserProfileDto
import com.example.mediq.data.api.VerifyOtpRequestDto

/**
 * A fake [MediQApiService] that records calls and returns configurable responses.
 */
class FakeMediQApiService : MediQApiService {

    var signInResponse: AuthSessionDto? = null
    var signOutResponse: AckDto = AckDto(true)

    var signOutCalled: Boolean = false

    override suspend fun signIn(body: SignInRequestDto): AuthSessionDto {
        return signInResponse ?: error("signInResponse not set")
    }

    override suspend fun signOut(): AckDto {
        signOutCalled = true
        return signOutResponse
    }

    override suspend fun requestOtp(body: RequestOtpRequestDto): OtpRequestedDto = TODO()
    override suspend fun verifyOtp(body: VerifyOtpRequestDto): Map<String, String> = TODO()
    override suspend fun register(body: RegisterRequestDto): AuthSessionDto = TODO()
    override suspend fun getDoctors(params: Map<String, String>): PagedDto<DoctorDto> = TODO()
    override suspend fun getDoctor(id: String): DoctorDto = TODO()
    override suspend fun getAvailability(id: String, month: String): PagedDto<AvailableDateDto> = TODO()
    override suspend fun getSlots(id: String, date: String): PagedDto<TimeSlotDto> = TODO()
    override suspend fun getAppointments(filter: String): PagedDto<AppointmentDto> = TODO()
    override suspend fun getAppointment(id: String): AppointmentDto = TODO()
    override suspend fun book(body: BookingRequestDto): AppointmentDto = TODO()
    override suspend fun cancelAppointment(id: String): AckDto = TODO()
    override suspend fun requestReschedule(id: String, body: RescheduleRequestDto): AckDto = TODO()
    override suspend fun getNotifications(): PagedDto<NotificationDto> = TODO()
    override suspend fun markNotificationRead(id: String): AckDto = TODO()
    override suspend fun getProfile(): UserProfileDto = TODO()
    override suspend fun updateProfile(body: UpdateProfileDto): UserProfileDto = TODO()
}
