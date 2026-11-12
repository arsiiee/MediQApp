package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.Appointment
import com.example.mediq.data.model.ClinicHours
import com.example.mediq.data.model.Doctor
import com.example.mediq.data.model.AppointmentStatus
import com.example.mediq.ui.screens.doctordetails.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Loads a doctor's profile and weekly schedule from the API so the
 * doctor-details screen shows real, live data instead of hard-coded samples.
 *
 * When a doctor changes their schedule on the server, the next time this
 * screen opens it fetches the updated clinic hours and renders them.
 */
class DoctorDetailsViewModel : ViewModel() {
    private val api = SupabaseApiService()

    private val _uiState = MutableStateFlow<DoctorDetailsUiState>(DoctorDetailsUiState.Loading)
    /** The current UI state: loading, error, or loaded with profile + schedule. */
    val uiState: StateFlow<DoctorDetailsUiState> = _uiState.asStateFlow()

    /**
     * Loads the doctor's profile and schedule for [doctorId].
     *
     * Safe to call repeatedly; each call resets to Loading while it fetches.
     */
    fun loadDoctor(doctorId: String) {
        viewModelScope.launch {
            _uiState.value = DoctorDetailsUiState.Loading
            try {
                val doctor = api.getDoctor(doctorId)
                val clinicHours = api.getDoctorSchedule(doctorId)
                // Fetch appointments for this doctor to block taken slots, and the
                // signed-in patient's appointments across all doctors to prevent
                // double-booking the same time with a different doctor.
                val thisDoctorAppointments = api.getDoctorAppointments(doctor.userId)
                val patientUserId = api.resolvePatientUserId()
                val patientAppointments = if (patientUserId != null) {
                    api.getAppointments(patientId = patientUserId).items
                } else {
                    emptyList()
                }
                val userBookedWithDoctor = thisDoctorAppointments.filter { it.patientId == patientUserId }
                val otherBookedWithDoctor = thisDoctorAppointments.filter { it.patientId != patientUserId }

                val profile = toDoctorProfile(doctor, clinicHours)
                val schedule = buildDateSchedules(
                    clinicHours = clinicHours,
                    userBooked = userBookedWithDoctor,
                    otherBooked = otherBookedWithDoctor,
                    patientAppointments = patientAppointments
                )

                _uiState.value = DoctorDetailsUiState.Content(
                    doctor = profile,
                    schedule = schedule,
                    userBookings = userBookedWithDoctor
                )
            } catch (e: Exception) {
                _uiState.value = DoctorDetailsUiState.Error(
                    message = e.message ?: "We couldn't load this doctor"
                )
            }
        }
    }

    /**
     * Server day-of-week (0=Sunday..6=Saturday) -> display name.
     */
    private fun dayOfWeekName(serverDayOfWeek: Int): String {
        return when (serverDayOfWeek) {
            0 -> "Sunday"
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            else -> "Unknown"
        }
    }

    /**
     * Maps the server [Doctor] and its [clinicHours] to the UI [DoctorProfile].
     */
    private fun toDoctorProfile(doctor: Doctor, clinicHours: List<ClinicHours>): DoctorProfile {
        val initials = doctor.fullName
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString("") { it.first().uppercase().toString() }
            .take(2)
            .ifEmpty { "DR" }

        return DoctorProfile(
            id = doctor.id,
            initials = initials,
            name = doctor.fullName,
            specialty = doctor.specialty,
            location =
                "${doctor.location.building} — ${doctor.location.floor}F · ${doctor.location.room}",
            experienceYears = doctor.yearsOfExperience,
            consultationFee = (doctor.feeCentavos / 100).toInt(),
            bio = doctor.bio,
            licenseNumber = doctor.licenseNumber,
            clinicHours = clinicHours.map { ch ->
                ClinicHour(
                    day = dayOfWeekName(ch.dayOfWeek),
                    time = "${ch.startTime} – ${ch.endTime}"
                )
            }
        )
    }

    /**
     * Builds the day strip from [clinicHours].
     *
     * Looks ahead 14 days from today. For each date whose day-of-week matches a
     * clinic day (and is not closed), it creates a [DateSchedule] with slots
     * generated from that day's published hours. Slots booked by the current
     * patient (with this doctor or overlapping another doctor), booked by other
     * patients, or already past (today) are not selectable.
     */
    private fun buildDateSchedules(
        clinicHours: List<ClinicHours>,
        userBooked: List<Appointment>,
        otherBooked: List<Appointment>,
        patientAppointments: List<Appointment> = emptyList()
    ): List<DateSchedule> {
        val byDayOfWeek = clinicHours.groupBy { it.dayOfWeek }
        val today = LocalDate.now(ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val schedules = mutableListOf<DateSchedule>()
        val activeUserBooked = userBooked.filter { it.status != AppointmentStatus.CANCELLED }
        val activeOtherBooked = otherBooked.filter { it.status != AppointmentStatus.CANCELLED }

        // Group user's own booked slots with this doctor by date -> set of 12h time strings
        val userBookedByDate = activeUserBooked.groupBy { appt ->
            parseLocalDateTime(appt.startsAt)?.toLocalDate()
        }.filterKeys { it != null }.mapKeys { it.key!! }.mapValues { (_, appts) ->
            appts.mapNotNull { appt ->
                parseLocalDateTime(appt.startsAt)?.let { ldt -> minutesTo12h(ldt.hour * 60 + ldt.minute) }
            }.toSet()
        }

        // Group other patients' booked slots by date -> set of 12h time strings
        val otherBookedByDate = activeOtherBooked.groupBy { appt ->
            parseLocalDateTime(appt.startsAt)?.toLocalDate()
        }.filterKeys { it != null }.mapKeys { it.key!! }.mapValues { (_, appts) ->
            appts.mapNotNull { appt ->
                parseLocalDateTime(appt.startsAt)?.let { ldt -> minutesTo12h(ldt.hour * 60 + ldt.minute) }
            }.toSet()
        }

        for (i in 0..13) {
            val date = today.plusDays(i.toLong())
            val isToday = i == 0
            val javaDow = date.dayOfWeek // MONDAY=1 .. SUNDAY=7
            val serverDayOfWeek = (javaDow.value % 7) // 0=Sunday .. 6=Saturday
            val hoursForDay = byDayOfWeek[serverDayOfWeek]?.filter { !it.isClosed } ?: continue

            val userBookedTimes = userBookedByDate[date] ?: emptySet()
            val otherBookedTimes = otherBookedByDate[date] ?: emptySet()
            val slots = buildSlotOptionsForDay(
                clinicHoursForDay = hoursForDay,
                isToday = isToday,
                nowMinutes = now.hour * 60 + now.minute,
                userBookedTimes = userBookedTimes,
                otherBookedTimes = otherBookedTimes,
                date = date,
                patientAppointments = patientAppointments
            )
            if (slots.isEmpty()) continue

            val option = DateOption(
                day = javaDow.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                date = date.dayOfMonth,
                fullDate = date.toString()
            )
            schedules.add(DateSchedule(option = option, slots = slots))
        }

        return schedules
    }

    private fun parseLocalDateTime(value: String): LocalDateTime? {
        return try {
            LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value))
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value)
            } catch (_: Exception) {
                null
            }
        }
    }
}