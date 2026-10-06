package com.example.mediq.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Retrofit interface for all MediQ server endpoints.
 *
 * Authentication is handled by [RetrofitClient.AuthInterceptor], which
 * automatically attaches `Authorization: Bearer <token>` on every request
 * whenever a token is present in [TokenStore]. No @Header parameter is needed
 * on individual methods — the interceptor adds it transparently.
 *
 * Endpoint reference: server/src/main/kotlin/…/server/http/Routes.kt
 */
interface MediQApiService {

    // ── Auth (unauthenticated) ─────────────────────────────────────────────

    @POST("auth/sign-in")
    suspend fun signIn(@Body body: SignInRequestDto): AuthSessionDto

    @POST("auth/sign-out")
    suspend fun signOut(): AckDto

    @POST("auth/otp/request")
    suspend fun requestOtp(@Body body: RequestOtpRequestDto): OtpRequestedDto

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body body: VerifyOtpRequestDto): Map<String, String>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthSessionDto

    // ── Doctors (public, no auth required) ────────────────────────────────

    @GET("doctors")
    suspend fun getDoctors(@QueryMap params: Map<String, String>): PagedDto<DoctorDto>

    @GET("doctors/{id}")
    suspend fun getDoctor(@Path("id") id: String): DoctorDto

    @GET("doctors/{id}/availability")
    suspend fun getAvailability(
        @Path("id") id: String,
        @Query("month") month: String,   // "YYYY-MM"
    ): PagedDto<AvailableDateDto>

    @GET("doctors/{id}/slots")
    suspend fun getSlots(
        @Path("id") id: String,
        @Query("date") date: String,     // "YYYY-MM-DD"
    ): PagedDto<TimeSlotDto>

    // ── Appointments (Bearer token required — added by AuthInterceptor) ───

    @GET("appointments")
    suspend fun getAppointments(
        @Query("filter") filter: String,
    ): PagedDto<AppointmentDto>

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") id: String): AppointmentDto

    @POST("appointments")
    suspend fun book(@Body body: BookingRequestDto): AppointmentDto

    @DELETE("appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: String): AckDto

    @POST("appointments/{id}/reschedule-request")
    suspend fun requestReschedule(
        @Path("id") id: String,
        @Body body: RescheduleRequestDto,
    ): AckDto

    // ── Notifications (Bearer required) ───────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): PagedDto<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): AckDto

    // ── Profile (Bearer required) ─────────────────────────────────────────

    @GET("profile")
    suspend fun getProfile(): UserProfileDto

    @PUT("profile")
    suspend fun updateProfile(@Body body: UpdateProfileDto): UserProfileDto
}
