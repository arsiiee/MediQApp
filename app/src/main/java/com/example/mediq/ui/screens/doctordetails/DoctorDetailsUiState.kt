package com.example.mediq.ui.screens.doctordetails

/** One weekday the doctor holds clinic, as listed under the day strip. */
data class ClinicHour(
    val day: String,
    val time: String
)

/** A selectable day in the strip. [date] is the day of the month. */
data class DateOption(
    val day: String,
    val date: Int
)

/** Whether a slot can still be booked. */
enum class TimeSlotStatus {
    AVAILABLE,
    RESERVED,
    BLOCKED
}

/** One day on the strip plus the status of every slot on it. */
data class DateSchedule(
    val option: DateOption,
    val statuses: Map<String, TimeSlotStatus>
) {
    /** Slots on this day that can still be booked. Derived, never hand-written. */
    val openSlots: Int
        get() = statuses.count { it.value == TimeSlotStatus.AVAILABLE }
}

/** The doctor as the detail screen renders them. */
data class DoctorProfile(
    val id: String,
    val initials: String,
    val name: String,
    val specialty: String,
    val location: String,
    val experienceYears: Int,
    val consultationFee: Int,
    val bio: String,
    val licenseNumber: String,
    val clinicHours: List<ClinicHour>
)

/**
 * Everything the screen can show.
 *
 * "No slots" is its own state rather than an empty [Content], because it says
 * something different to the patient: the profile loaded fine, the clinic just
 * has nothing to book.
 */
sealed interface DoctorDetailsUiState {

    /** Skeleton that mirrors the loaded layout. */
    data object Loading : DoctorDetailsUiState

    /** The profile or its schedule could not load. */
    data class Error(val message: String) : DoctorDetailsUiState

    /** The profile loaded, but no day on the strip has a bookable slot. */
    data class NoSlots(val doctor: DoctorProfile) : DoctorDetailsUiState

    /** Profile and schedule loaded. */
    data class Content(
        val doctor: DoctorProfile,
        val schedule: List<DateSchedule>
    ) : DoctorDetailsUiState {

        /** True while at least one day on the strip has a bookable slot. */
        val hasOpenSlots: Boolean
            get() = schedule.any { it.openSlots > 0 }
    }
}

/**
 * Callbacks the screen raises.
 *
 * [onContinue] receives the day's **index on the strip**, which is the value
 * `BookingFlow` reads as its `dateIndex` argument.
 */
data class DoctorDetailsActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onContinue: (dateIndex: Int, time: String) -> Unit = { _, _ -> }
)
