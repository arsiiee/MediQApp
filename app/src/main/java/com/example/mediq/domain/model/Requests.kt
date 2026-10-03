package com.example.mediq.domain.model

import java.time.LocalDate

/**
 * Filters for the doctor list. Both the search field and the two filter chips
 * on the Doctors screen map onto this one object, so "filter by specialty" and
 * "filter by clinic location" become a single query rather than two screens'
 * worth of state.
 */
data class DoctorQuery(
    val searchText: String? = null,
    val specialty: Specialty? = null,
    val building: String? = null,
)

/** Which appointments to fetch. */
enum class AppointmentFilter(val wireValue: String) {
    UPCOMING("upcoming"),
    HISTORY("history"),
    ALL("all"),
}

/**
 * A booking request. The slot is identified by its own id rather than by
 * doctor, date, and time, so the server re-checks availability against a single
 * value and can't be sent a date and time that don't match a real slot.
 */
data class BookingRequest(
    val slotId: String,
    val reasonForVisit: String?,
    val confirmedByPatient: Boolean,
)

// --- Auth -----------------------------------------------------------------

data class SignInRequest(
    val username: String,
    val password: String,
)

/** Starts registration by sending an OTP to the phone number. */
data class RequestOtpRequest(
    val mobileNumber: String,
)

data class VerifyOtpRequest(
    val mobileNumber: String,
    val otp: String,
)

/**
 * Finishes registration. [registrationId] ties this back to the verified OTP
 * so the server knows the number was already proven in the earlier step.
 */
data class RegisterRequest(
    val registrationId: String,
    val fullName: String,
    val username: String,
    val password: String,
    val dateOfBirth: LocalDate?,
    val sex: Sex?,
    val email: String?,
)

// --- Paging ---------------------------------------------------------------

/** A page of results. Cursors avoid the offset drift of page numbers. */
data class Paged<T>(
    val items: List<T>,
    val nextCursor: String? = null,
) {
    val hasMore: Boolean get() = nextCursor != null
}