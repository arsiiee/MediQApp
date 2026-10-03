package com.example.mediq.server.db

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.CLINIC_ZONE
import com.example.mediq.domain.model.clinicDateTime
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import java.sql.Connection
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.YearMonth
import java.util.UUID

/**
 * Doctors, their recurring hours, and the concrete slots generated from those
 * hours.
 *
 * Slots are materialised as rows rather than computed on read. That is what
 * makes the booking transaction possible — the unique constraint in
 * `slot_claims` can only protect a row that already exists.
 */
class DoctorStore(
    private val db: Database,
    private val slotDurationMinutes: Int,
) {

    fun list(query: DoctorQuery, limit: Int, offset: Int): Pair<List<Doctor>, Boolean> =
        db.read { c ->
            ensureSpecialties(c)
            val where = StringBuilder(" WHERE 1=1")
            val params = mutableListOf<Any?>()
            if (query.searchText != null) {
                where.append(" AND LOWER(d.full_name) LIKE ?")
                params += "%${query.searchText.lowercase()}%"
            }
            if (query.specialty != null) {
                where.append(" AND d.specialty_id = ?")
                params += query.specialty.wireValue
            }
            if (query.building != null) {
                where.append(" AND LOWER(d.building) LIKE ?")
                params += "%${query.building.lowercase()}%"
            }

            val total = c.prepareStatement(
                "SELECT COUNT(*) FROM doctors d$where"
            ).use { st -> bind(st, params); st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 } }

            // limit + 1 to learn whether another page exists without a count.
            val doctors = c.prepareStatement(
                "$SELECT_DOCTOR$where ORDER BY d.full_name LIMIT ? OFFSET ?"
            ).use { st ->
                bind(st, params)
                st.setInt(params.size + 1, limit + 1)
                st.setInt(params.size + 2, offset)
                st.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.readDoctor(c) else null }.toList()
                }
            }
            val hasMore = doctors.size > limit
            doctors.take(limit) to hasMore
        }

    fun get(doctorId: String): Doctor = db.read { c ->
        ensureSpecialties(c)
        c.prepareStatement("$SELECT_DOCTOR WHERE d.id = ?").use { st ->
            st.setString(1, doctorId)
            st.executeQuery().use { rs ->
                if (rs.next()) rs.readDoctor(c) else throw ApiError.notFound("That doctor was not found.")
            }
        }
    }

    /**
     * Dates in [month] with at least one slot still open.
     *
     * Only checks days that have clinic hours at all, then counts slots that
     * are neither blocked nor claimed. Slots are generated lazily rather than
     * for the whole month up front.
     */
    fun availableDates(doctorId: String, month: LocalDate): List<AvailableDate> {
        val yearMonth = YearMonth.from(month)
        // Do not offer dates that have already passed.
        val today = LocalDate.now(CLINIC_ZONE)
        val firstDay = maxOf(yearMonth.atDay(1), today)

        return db.read { c ->
            val days = mutableListOf<AvailableDate>()
            var date = firstDay
            while (!date.isAfter(yearMonth.atEndOfMonth())) {
                // Materialise the day before counting it. Slots are generated
                // lazily, so counting without generating first reports every
                // date as having nothing open — the date picker would come up
                // permanently empty.
                ensureSlots(c, doctorId, date)

                val open = countOpenSlots(c, doctorId, date)
                if (open > 0) days += AvailableDate(date, open)
                date = date.plusDays(1)
            }
            days
        }
    }

    /**
     * Every slot on [date], taken ones included.
     *
     * The clinic owns availability, so the app is told what is really open
     * rather than deciding from a cached copy — a slot that was free a minute
     * ago may be gone by the time the user taps it.
     */
    fun slots(doctorId: String, date: LocalDate): List<TimeSlot> = db.read { c ->
        ensureSlots(c, doctorId, date)
        c.prepareStatement("$SELECT_SLOTS WHERE s.doctor_id = ? AND s.starts_at >= ? AND s.starts_at < ? ORDER BY s.starts_at")
            .use { st ->
                val start = date.atStartOfDay(CLINIC_ZONE).toInstant()
                val end = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant()
                st.setString(1, doctorId)
                st.setObject(2, start.atOffset(ZoneOffset.UTC))
                st.setObject(3, end.atOffset(ZoneOffset.UTC))
                st.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.readSlot() else null }.toList()
                }
            }
    }

    /** Slot ids that a request refers to must belong to this doctor and be bookable. */
    fun requireBookableSlot(c: Connection, slotId: String): TimeSlot {
        val slot = c.prepareStatement("$SELECT_SLOTS WHERE s.id = ?").use { st ->
            st.setString(1, slotId)
            st.executeQuery().use { rs -> if (rs.next()) rs.readSlot() else null }
        } ?: throw ApiError.badRequest("That time slot does not exist.")

        if (slot.status == SlotStatus.BLOCKED) {
            throw ApiError.conflict("The clinic has blocked that time.")
        }
        if (slot.status == SlotStatus.RESERVED) {
            throw ApiError.conflict("That time was just taken. Please pick another.")
        }
        if (slot.startsAt.isBefore(Instant.now())) {
            throw ApiError.badRequest("That time is already in the past.")
        }
        return slot
    }

    /** Creates the rows for [date] if they are not there yet. */
    fun ensureSlots(c: Connection, doctorId: String, date: LocalDate) {
        val alreadyGenerated = c.prepareStatement(
            "SELECT COUNT(*) FROM slots WHERE doctor_id = ? AND starts_at >= ? AND starts_at < ?"
        ).use { st ->
            val start = date.atStartOfDay(CLINIC_ZONE).toInstant()
            val end = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant()
            st.setString(1, doctorId)
            st.setObject(2, start.atOffset(ZoneOffset.UTC))
            st.setObject(3, end.atOffset(ZoneOffset.UTC))
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
        if (alreadyGenerated > 0) return

        val hours = clinicHours(c, doctorId, date.dayOfWeek)
        val step = slotDurationMinutes
        val now = Instant.now()

        c.prepareStatement(
            "MERGE INTO slots (id, doctor_id, starts_at, ends_at, status) KEY (id) VALUES (?, ?, ?, ?, 'available')"
        ).use { st ->
            for (window in hours) {
                var start = window.opensAt
                while (!start.plusMinutes(step.toLong()).isAfter(window.closesAt)) {
                    val end = start.plusMinutes(step.toLong())
                    val startsAt: Instant = clinicDateTime(date, start)
                    // Never materialise slots that are already behind us.
                    if (startsAt.isAfter(now)) {
                        st.setString(1, deterministicSlotId(doctorId, startsAt))
                        st.setString(2, doctorId)
                        st.setObject(3, startsAt.atOffset(ZoneOffset.UTC))
                        st.setObject(4, clinicDateTime(date, end).atOffset(ZoneOffset.UTC))
                        st.addBatch()
                    }
                    start = end
                }
            }
            st.executeBatch()
        }
    }

    private fun clinicHours(c: Connection, doctorId: String, day: DayOfWeek): List<ClinicHours> =
        c.prepareStatement(
            "SELECT day_of_week, opens_at, closes_at FROM clinic_hours WHERE doctor_id = ? AND day_of_week = ?"
        ).use { st ->
            st.setString(1, doctorId)
            st.setInt(2, day.value)
            st.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) {
                        ClinicHours(
                            dayOfWeek = day,
                            opensAt = rs.getObject("opens_at", LocalTime::class.java),
                            closesAt = rs.getObject("closes_at", LocalTime::class.java),
                        )
                    } else {
                        null
                    }
                }.toList()
            }
        }

    private fun countOpenSlots(c: Connection, doctorId: String, date: LocalDate): Int =
        c.prepareStatement(
            """
            SELECT COUNT(*) FROM slots s
            WHERE s.doctor_id = ? AND s.status = 'available'
              AND s.starts_at >= ? AND s.starts_at < ?
              AND NOT EXISTS (SELECT 1 FROM slot_claims sc WHERE sc.slot_id = s.id)
            """.trimIndent()
        ).use { st ->
            st.setString(1, doctorId)
            st.setObject(2, date.atStartOfDay(CLINIC_ZONE).toInstant().atOffset(ZoneOffset.UTC))
            st.setObject(3, date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant().atOffset(ZoneOffset.UTC))
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

    companion object {
        /**
         * Derived from the doctor and start time, so regenerating a date
         * produces the same ids and `MERGE` stays idempotent under concurrent
         * requests. A random UUID here would create duplicate slots.
         */
        fun deterministicSlotId(doctorId: String, startsAt: Instant): String =
            UUID.nameUUIDFromBytes("$doctorId|$startsAt".toByteArray()).toString()

        private fun bind(st: java.sql.PreparedStatement, params: List<Any?>) {
            params.forEachIndexed { i, p -> st.setObject(i + 1, p) }
        }

        private fun ensureSpecialties(c: Connection) {
            c.prepareStatement(
                "MERGE INTO specialties (id, display_name) KEY (id) VALUES (?, ?)"
            ).use { st ->
                Specialty.entries.forEach { s ->
                    st.setString(1, s.wireValue)
                    st.setString(2, s.displayName)
                    st.addBatch()
                }
                st.executeBatch()
            }
        }

        private val SELECT_DOCTOR = """
            SELECT d.id, d.full_name, d.specialty_id, d.years_experience, d.fee_centavos,
                   d.license_number, d.bio, d.building, d.floor, d.room, d.languages
            FROM doctors d
        """

        // The claim join is what turns a stored 'available' slot into a wire
        // status of 'reserved'. The slot table itself is not mutated on booking,
        // so releasing a cancelled slot is a single delete.
        private val SELECT_SLOTS = """
            SELECT s.id, s.doctor_id, s.starts_at, s.ends_at, s.status,
                   (SELECT COUNT(*) FROM slot_claims sc WHERE sc.slot_id = s.id) AS claim_count
            FROM slots s
        """
    }
}

private fun java.sql.ResultSet.readSlot() = TimeSlot(
    id = getString("id"),
    doctorId = getString("doctor_id"),
    startsAt = getObject("starts_at", OffsetDateTime::class.java).toInstant(),
    endsAt = getObject("ends_at", OffsetDateTime::class.java).toInstant(),
    status = when {
        getString("status") == "blocked" -> SlotStatus.BLOCKED
        getInt("claim_count") > 0 -> SlotStatus.RESERVED
        else -> SlotStatus.AVAILABLE
    },
)

private fun java.sql.ResultSet.readDoctor(c: Connection): Doctor {
    val id = getString("id")
    val hours = c.prepareStatement(
        "SELECT day_of_week, opens_at, closes_at FROM clinic_hours WHERE doctor_id = ? ORDER BY day_of_week"
    ).use { st ->
        st.setString(1, id)
        st.executeQuery().use { rs ->
            generateSequence {
                if (rs.next()) {
                    ClinicHours(
                        dayOfWeek = DayOfWeek.of(rs.getInt("day_of_week")),
                        opensAt = rs.getObject("opens_at", LocalTime::class.java),
                        closesAt = rs.getObject("closes_at", LocalTime::class.java),
                    )
                } else {
                    null
                }
            }.toList()
        }
    }
    val languages = getString("languages")
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { wire -> com.example.mediq.domain.model.ConsultationLanguage.entries.firstOrNull { it.wireValue == wire } }

    return Doctor(
        id = id,
        fullName = getString("full_name"),
        specialty = Specialty.entries.first { it.wireValue == getString("specialty_id") },
        yearsOfExperience = getInt("years_experience"),
        consultationFee = Money(getLong("fee_centavos")),
        location = com.example.mediq.domain.model.ClinicLocation(
            building = getString("building"),
            floor = getString("floor"),
            room = getString("room"),
        ),
        licenseNumber = getString("license_number"),
        bio = getString("bio"),
        languages = languages,
        clinicHours = hours,
    )
}