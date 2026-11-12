package com.example.mediq.ui.screens.doctordetails

/*
 * The current doctor-details data source: fixed sample content.
 *
 * Replacing this with a repository changes only DoctorDetailsScreen's default
 * argument. The screen has never resolved `doctorId` against this content, and
 * that is unchanged here: the profile is the doctor the list shows for id "1".
 */

object DoctorDetailsSamples {

    val profile = DoctorProfile(
        id = "1",
        initials = "ME",
        name = "Dr. Maria Elena Sandoval",
        specialty = "Internal Medicine",
        location = "Main Building — 2F · Clinic 204",
        experienceYears = 14,
        consultationFee = 700,
        bio = "Adult internal medicine with focus on diabetes, hypertension, " +
                "and preventive check-ups. Consults in Filipino, Cebuano, and English.",
        licenseNumber = "PRC 0112843",
        clinicHours = listOf(
            ClinicHour(day = "Monday", time = "9:00 AM – 5:00 PM"),
            ClinicHour(day = "Wednesday", time = "9:00 AM – 5:00 PM"),
            ClinicHour(day = "Friday", time = "9:00 AM – 5:00 PM")
        )
    )

    /*
     * The strip. The day order is the contract BookingScreens reads: it looks
     * its own date strings up by the index of the day chosen here.
     */
    private val schedule = listOf(
        DateSchedule(
            option = DateOption(day = "Mon", date = 21, fullDate = "2026-09-21"),
            slots = slotsFor("10:00 AM", blocked = listOf("1:00 PM", "3:30 PM", "4:30 PM"))
        ),
        DateSchedule(
            option = DateOption(day = "Wed", date = 23, fullDate = "2026-09-23"),
            slots = slotsFor("9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", blocked = listOf("11:30 AM", "2:00 PM", "4:30 PM"))
        ),
        DateSchedule(
            option = DateOption(day = "Fri", date = 25, fullDate = "2026-09-25"),
            slots = slotsFor("10:00 AM", "10:30 AM", blocked = listOf("11:30 AM", "1:30 PM", "3:30 PM", "4:30 PM"))
        ),
        DateSchedule(
            option = DateOption(day = "Mon", date = 28, fullDate = "2026-09-28"),
            slots = slotsFor("9:30 AM", "11:00 AM", blocked = listOf("11:30 AM", "2:30 PM", "4:00 PM"))
        ),
        DateSchedule(
            option = DateOption(day = "Wed", date = 30, fullDate = "2026-09-30"),
            slots = slotsFor("9:00 AM", "10:00 AM", "11:00 AM", blocked = listOf("11:30 AM", "3:00 PM", "4:30 PM"))
        )
    )

    /** Loaded, with bookable slots. */
    val content = DoctorDetailsUiState.Content(
        doctor = profile,
        schedule = schedule,
        userBookings = emptyList()
    )

    /** Loaded, but every slot on every day is taken or blocked. */
    val noSlots = DoctorDetailsUiState.NoSlots(doctor = profile)

    val loading = DoctorDetailsUiState.Loading

    val error = DoctorDetailsUiState.Error(
        message = "Check your connection and try again."
    )

    /**
     * The same profile and strip with nothing left to book.
     *
     * Used by previews and the "no slots" test rather than hand-building a
     * second schedule.
     */
    fun scheduleWithNothingOpen(): List<DateSchedule> = schedule.map { day ->
        day.copy(
            slots = day.slots.map { it.copy(status = TimeSlotStatus.BLOCKED) }
        )
    }

    private fun slotsFor(
        vararg available: String,
        blocked: List<String> = emptyList()
    ): List<TimeSlotOption> {
        val labels = (available.toList() + blocked).distinct().sortedBy { time12hToMinutes(it) }
        val availableSet = available.toSet()
        val blockedSet = blocked.toSet()
        return labels.map { time ->
            TimeSlotOption(
                time = time,
                status = when {
                    time in availableSet -> TimeSlotStatus.AVAILABLE
                    time in blockedSet -> TimeSlotStatus.BLOCKED
                    else -> TimeSlotStatus.RESERVED
                }
            )
        }
    }
}
