package com.example.mediq.data.api

import com.example.mediq.data.model.*
import retrofit2.http.*

interface MediQApiService {
    // Auth
    @POST("auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): AuthSession

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthSession

    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): Unit

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthSession

    @PATCH("auth/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): UserProfile

    @POST("auth/refresh")
    suspend fun refreshToken(): AuthSession

    // Profile
    @GET("profile")
    suspend fun getProfile(): UserProfile

    @PATCH("profile")
    suspend fun updateProfile(@Body draft: ProfileDraft): UserProfile

    // Doctors
    @GET("doctors")
    suspend fun getDoctors(
        @Query("specializationId") specializationId: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: DoctorStatus? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Paged<Doctor>

    @GET("doctors/{id}")
    suspend fun getDoctor(@Path("id") id: String): Doctor

    @GET("doctors/{id}/schedule")
    suspend fun getDoctorSchedule(@Path("id") id: String): List<ClinicHours>

    @GET("doctors/{id}/availability")
    suspend fun getDoctorAvailability(
        @Path("id") id: String,
        @Query("date") date: String,
    ): List<AvailableDate>

    @GET("doctors/{id}/appointments")
    suspend fun getDoctorAppointments(
        @Path("id") id: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<Appointment>

    // Specializations
    @GET("specializations")
    suspend fun getSpecializations(): List<Specialty>

    // Appointments
    @GET("appointments")
    suspend fun getAppointments(
        @Query("status") status: AppointmentStatus? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Paged<Appointment>

    @POST("appointments")
    suspend fun createAppointment(@Body request: BookingRequest): Appointment

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") id: String): Appointment

    // Messages
    @GET("messages/conversations")
    suspend fun getConversations(): List<MessageThread>

    @GET("messages/conversations/{id}/messages")
    suspend fun getMessages(@Path("id") conversationId: String): List<Message>

    @POST("messages/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: String, @Body body: String): Message

    @PATCH("messages/conversations/{id}/read")
    suspend fun markConversationRead(@Path("id") conversationId: String): Unit

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(@Query("unread") unread: Boolean = false): List<Notification>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Unit

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Unit

    // Admin
    @GET("admin/accounts")
    suspend fun getAdminAccounts(): AdminAccountsResponse

    @POST("admin/accounts")
    suspend fun createStaffAccount(
        @Query("kind") kind: String,
        @Body draft: StaffDraft,
    ): UserProfile

    @PATCH("admin/accounts/{id}/status")
    suspend fun updateAccountStatus(
        @Path("id") id: String,
        @Body status: AccountStatus,
    ): UserProfile

    // Secretary
    @GET("secretary/appointments")
    suspend fun getSecretaryAppointments(): List<Appointment>

    @POST("secretary/appointments/{id}/confirm")
    suspend fun confirmAppointment(@Path("id") id: String): Unit

    @POST("secretary/appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: String): Unit

    @GET("secretary/patients")
    suspend fun getSecretaryPatients(): List<UserProfile>

    @GET("secretary/messages")
    suspend fun getSecretaryMessages(): List<MessageThread>

    // Health
    @GET("health")
    suspend fun healthCheck(): Map<String, String>
}