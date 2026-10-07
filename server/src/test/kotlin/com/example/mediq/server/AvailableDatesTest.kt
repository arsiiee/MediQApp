package com.example.mediq.server

import com.example.mediq.domain.model.CLINIC_ZONE
import com.example.mediq.server.db.DoctorStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the date picker: which calendar dates come back, and how many open
 * slots each one has.
 *
 * The rule this class exists to protect is that a date is a **clinic** date.
 * `CLINIC_ZONE` is Asia/Manila, so a clinic open at 01:00 is open on that
 * calendar day, even though 01:00 Manila is the *previous* day in UTC. A query
 * that groups slots by `CAST(starts_at AS DATE)` gets that wrong on any host
 * whose session zone is not Manila, and no other test in the suite notices:
 * every other fixture opens at 09:00 or later, and `smoke.ps1` only asserts
 * `Status 200` on this endpoint.
 *
 * Each test builds its own database, and several deliberately set a session
 * zone the production default never uses — that is the whole point. A test that
 * passes only because the developer happens to be in UTC+8 proves nothing.
 */
class AvailableDatesTest {

    /**
     * A whole month far enough out that no slot is ever "in the past" during a
     * run. Starts in July so a run that straddles new year cannot produce a
     * month that has already begun.
     */
    private fun futureMonth(daysAhead: Long = 60L): YearMonth =
        YearMonth.from(LocalDate.now(CLINIC_ZONE).plusDays(daysAhead)).let { ym ->
            if (ym.monthValue >= 7) ym else ym.plusMonths((7 - ym.monthValue).toLong())
        }

    private fun database(sessionTimeZone: String): Database = Database(
        ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:avail_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            jwtSecret = "test-secret-not-used-for-anything-real-123456",
            jwtIssuer = "mediq-test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
            sessionTimeZone = sessionTimeZone,
        )
    )

    /**
     * Opens every ISO weekday so any target date has slots, which keeps these
     * tests about the calendar rather than about day-of-week maths.
     */
    private fun insertDoctor(db: Database, opens: LocalTime, closes: LocalTime): String {
        val doctorId = UUID.randomUUID().toString()
        db.tx { c ->
            c.prepareStatement(
                "MERGE INTO specialties (id, display_name) KEY (id) VALUES ('general', 'General')"
            ).use { it.executeUpdate() }

            c.prepareStatement(
                """
                INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                     license_number, bio, building, floor, room, languages, created_at)
                VALUES (?, 'Calendar Doctor', 'general', 5, 70000, 'TEST-CAL', 'bio', 'Main', '1F', '101', 'en', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, doctorId)
                st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }

            (1..7).forEach { dayOfWeek ->
                c.prepareStatement(
                    """
                    INSERT INTO clinic_hours (id, doctor_id, day_of_week, opens_at, closes_at)
                    VALUES (?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, UUID.randomUUID().toString())
                    st.setString(2, doctorId)
                    st.setInt(3, dayOfWeek)
                    st.setObject(4, opens)
                    st.setObject(5, closes)
                    st.executeUpdate()
                }
            }
        }
        return doctorId
    }

    // -----------------------------------------------------------------------
    // The regression this file was written for.
    // -----------------------------------------------------------------------

    @Test
    fun `a clinic open after midnight is reported on its Manila date, not the UTC one`() {
        // 01:00-04:00 Manila is 17:00-20:00 UTC the *previous* day. Grouping by
        // UTC pushes every one of those slots onto yesterday, which both
        // miscounts each day and hides the final day of the month entirely.
        for (sessionZone in listOf("UTC", "America/New_York", "Asia/Manila")) {
            val db = database(sessionZone)
            try {
                val doctors = DoctorStore(db, slotDurationMinutes = 30)
                val doctorId = insertDoctor(db, LocalTime.of(1, 0), LocalTime.of(4, 0))

                val month = futureMonth()
                val dates = doctors.availableDates(doctorId, month.atDay(1))

                val reported = dates.map { it.date }
                assertTrue(
                    reported.isNotEmpty(),
                    "no dates returned for session zone $sessionZone",
                )
                assertTrue(
                    reported.all { YearMonth.from(it) == month },
                    "session zone $sessionZone reported dates outside the requested month: " +
                        reported.filterNot { YearMonth.from(it) == month },
                )
                // The last day of the month is the one that disappears when the
                // slots shift backwards past the range end.
                assertTrue(
                    reported.contains(month.atEndOfMonth()),
                    "session zone $sessionZone dropped the last day of the month " +
                        "(${month.atEndOfMonth()}); got ${reported.lastOrNull()}",
                )
                assertEquals(
                    month.atDay(1),
                    reported.first(),
                    "session zone $sessionZone did not start the month on the 1st",
                )
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun `open slot counts match the slots that were generated`() {
        // 09:00-10:30 at 30 minutes is three slots. Asserting the count catches
        // a GROUP BY that both mislabels and double-counts.
        val db = database("UTC")
        try {
            val doctors = DoctorStore(db, slotDurationMinutes = 30)
            val doctorId = insertDoctor(db, LocalTime.of(9, 0), LocalTime.of(10, 30))

            val month = futureMonth(daysAhead = 90)
            val dates = doctors.availableDates(doctorId, month.atDay(1))

            assertTrue(dates.isNotEmpty(), "expected dates for a 09:00-10:30 clinic")
            assertTrue(
                dates.all { it.openSlotCount == 3 },
                "expected 3 open slots per day, got ${dates.map { it.date to it.openSlotCount }}",
            )
            assertEquals(
                dates.map { it.date }.sorted(),
                dates.map { it.date },
                "dates must be returned in ascending order",
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `a month entirely in the past returns nothing`() {
        val db = database("UTC")
        try {
            val doctors = DoctorStore(db, slotDurationMinutes = 30)
            val doctorId = insertDoctor(db, LocalTime.of(9, 0), LocalTime.of(17, 0))

            val past = LocalDate.now(CLINIC_ZONE).minusMonths(2)
            assertEquals(
                emptyList(),
                doctors.availableDates(doctorId, past),
                "a fully-past month must not offer bookable dates",
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `a doctor with no clinic hours offers no dates`() {
        val db = database("UTC")
        try {
            val doctors = DoctorStore(db, slotDurationMinutes = 30)
            val doctorId = UUID.randomUUID().toString()
            db.tx { c ->
                c.prepareStatement(
                    "MERGE INTO specialties (id, display_name) KEY (id) VALUES ('general', 'General')"
                ).use { it.executeUpdate() }
                c.prepareStatement(
                    """
                    INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                         license_number, bio, building, floor, room, languages, created_at)
                    VALUES (?, 'No Hours', 'general', 1, 1000, 'TEST-NOH', 'bio', 'Main', '1F', '1', 'en', ?)
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, doctorId)
                    st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
                    st.executeUpdate()
                }
            }

            assertEquals(
                emptyList(),
                doctors.availableDates(doctorId, futureMonth().atDay(1)),
                "a doctor with no clinic hours must not generate slots",
            )
        } finally {
            db.close()
        }
    }
}