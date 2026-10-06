package com.example.mediq.data.repository

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.domain.repository.AppointmentRepository
import com.example.mediq.domain.repository.AuthRepository
import com.example.mediq.domain.repository.DoctorRepository
import com.example.mediq.domain.repository.NotificationRepository
import com.example.mediq.domain.repository.ProfileRepository
import java.time.LocalDate

/**
 * Raised by the placeholder repositories below.
 *
 * Reads turn this into an empty screen. Writes surface it as an error, because
 * "booked successfully" would be a lie when nothing was booked.
 */
class BackendNotConnectedException : Exception(
    "No backend is wired up yet. This repository returns nothing on purpose."
)

/**
 * Stand-in implementations used until a real backend exists.
 *
 * Reads return empty results, which is what the empty states already draw.
 * Nothing here invents data — that was removed on purpose, because a fake
 * doctor with a plausible licence number is easy to mistake for a real one.
 *
 * When the backend lands, delete this file and register the real repositories
 * in [com.example.mediq.di.AppContainer] instead. The screens and view models
 * should not need to change.
 */

class EmptyDoctorRepository : DoctorRepository {

    override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> =
        Paged(items = emptyList(), nextCursor = null)

    override suspend fun getDoctor(doctorId: String): Doctor = throw BackendNotConnectedException()

    override suspend fun getAvailableDates(doctorId: String, month: LocalDate): List<AvailableDate> =
        emptyList()

    override suspend fun getSlots(doctorId: String, date: LocalDate): List<TimeSlot> =
        emptyList()
}

class EmptyAppointmentRepository : AppointmentRepository {

    override suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment> =
        Paged(items = emptyList(), nextCursor = null)

    override suspend fun getAppointment(appointmentId: String): Appointment =
        throw BackendNotConnectedException()

    override suspend fun book(request: BookingRequest): Appointment =
        throw BackendNotConnectedException()

    override suspend fun cancel(appointmentId: String): Unit = throw BackendNotConnectedException()

    override suspend fun requestReschedule(request: RescheduleRequest): Unit =
        throw BackendNotConnectedException()
}

class EmptyNotificationRepository : NotificationRepository {

    override suspend fun getNotifications(): Paged<Notification> =
        Paged(items = emptyList(), nextCursor = null)

    override suspend fun markRead(notificationId: String): Unit = throw BackendNotConnectedException()
}

class EmptyProfileRepository : ProfileRepository {

    override suspend fun getProfile(): UserProfile = throw BackendNotConnectedException()

    override suspend fun updateProfile(profile: UserProfile): UserProfile =
        throw BackendNotConnectedException()
}

class EmptyAuthRepository : AuthRepository {

    override suspend fun signIn(request: SignInRequest): AuthSession =
        throw BackendNotConnectedException()

    override suspend fun signOut(): Unit = Unit

    /** Nobody is signed in, which is true today. */
    override suspend fun currentSession(): AuthSession? = null

    override suspend fun requestOtp(request: RequestOtpRequest): String =
        throw BackendNotConnectedException()

    override suspend fun verifyOtp(request: VerifyOtpRequest): String =
        throw BackendNotConnectedException()

    override suspend fun register(request: RegisterRequest): AuthSession =
        throw BackendNotConnectedException()
}