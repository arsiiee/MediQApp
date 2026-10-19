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

    // --- Messages ----------------------------------------------------------
    //
    // Settable per method rather than one shared switch: the interesting tests
    // need one call to fail while the rest behave, and a single `throwOn` flag
    // cannot express that.

    var messageThreadsResponse: PagedDto<MessageThreadDto> = PagedDto(emptyList())
    var openThreadResponse: MessageThreadDto? = null
    var messageThreadResponse: MessageThreadDto? = null
    var messagesResponse: PagedDto<MessageDto> = PagedDto(emptyList())
    var sendMessageResponse: MessageDto? = null
    var markThreadReadResponse: AckDto = AckDto(true)

    var messageThreadsError: Throwable? = null
    var openThreadError: Throwable? = null
    var messageThreadError: Throwable? = null
    var messagesError: Throwable? = null
    var sendMessageError: Throwable? = null
    var markThreadReadError: Throwable? = null

    var messageThreadsCalled: Boolean = false
    val openThreadRequests = mutableListOf<OpenThreadRequestDto>()
    val sendRequests = mutableListOf<Pair<String, SendMessageRequestDto>>()
    val markedReadThreadIds = mutableListOf<String>()
    val requestedMessageCursors = mutableListOf<String?>()

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

    override suspend fun getMessageThreads(): PagedDto<MessageThreadDto> {
        messageThreadsCalled = true
        messageThreadsError?.let { throw it }
        return messageThreadsResponse
    }

    override suspend fun openMessageThread(body: OpenThreadRequestDto): MessageThreadDto {
        openThreadRequests += body
        openThreadError?.let { throw it }
        return openThreadResponse ?: error("openThreadResponse not set")
    }

    override suspend fun getMessageThread(id: String): MessageThreadDto {
        messageThreadError?.let { throw it }
        return messageThreadResponse ?: error("messageThreadResponse not set")
    }

    override suspend fun getMessages(threadId: String, cursor: String?): PagedDto<MessageDto> {
        requestedMessageCursors += cursor
        messagesError?.let { throw it }
        return messagesResponse
    }

    override suspend fun sendMessage(threadId: String, body: SendMessageRequestDto): MessageDto {
        sendRequests += threadId to body
        sendMessageError?.let { throw it }
        return sendMessageResponse ?: error("sendMessageResponse not set")
    }

    override suspend fun markThreadRead(id: String): AckDto {
        markedReadThreadIds += id
        markThreadReadError?.let { throw it }
        return markThreadReadResponse
    }
}
