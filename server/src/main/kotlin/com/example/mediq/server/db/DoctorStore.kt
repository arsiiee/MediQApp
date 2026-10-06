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

            // limit + 1 to learn whether another page exists without a second COUNT query.
            val rows = c.prepareStatement(
                "$SELECT_DOCTOR$where ORDER BY d.full_name LIMIT ? OFFSET ?"
            ).use { st ->
                bind(st, params)
                st.setInt(params.size + 1, limit + 1)
                st.setInt(params.size + 2, offset)
                st.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.readDoctorRow() else null }.toList()
                }
            }

            val page = rows.take(limit)
            val hasMore = rows.size > limit

            // Batch-fetch clinic_hours for all doctors in the page in one query
            // instead of one query per doctor (N+1 → 2 queries total).
            val hoursMap: Map<String, List<ClinicHours>> = if (page.isEmpty()) {
                emptyMap()
            } else {
                val placeholders = page.joinToString(",") { "?" }
                c.prepareStatement(
                    "SELECT doctor_id, day_of_week, opens_at, closes_at FROM clinic_hours WHERE doctor_id IN ($placeholders) ORDER BY doctor_id, day_of_week"
                ).use { st ->
                    page.forEachIndexed { i, row -> st.setString(i + 1, row.id) }
                    st.executeQuery().use { rs ->
                        val map = mutableMapOf<String, MutableList<ClinicHours>>()
                        while (rs.next()) {
                            val doctorId = rs.getString("doctor_id")
                            map.getOrPut(doctorId) { mutableListOf() } += ClinicHours(
                                dayOfWeek = DayOfWeek.of(rs.getInt("day_of_week")),
                                opensAt = rs.getObject("opens_at", LocalTime::class.java),
                                closesAt = rs.getObject("closes_at", LocalTime::class.java),
                            )
                        }
                        map
                    }
                }
            }

            page.map { it.toDoctor(hoursMap[it.id].orEmpty()) } to hasMore
        }

    fun get(doctorId: String): Doctor = db.read { c ->
        ensureSpecialties(c)
        val row = c.prepareStatement("$SELECT_DOCTOR WHERE d.id = ?").use { st ->
            st.setString(1, doctorId)
            st.executeQuery().use { rs ->
                if (rs.next()) rs.readDoctorRow() else throw ApiError.notFound("That doctor was not found.")
            }
        }
        val hours = c.prepareStatement(
            "SELECT day_of_week, opens_at, closes_at FROM clinic_hours WHERE doctor_id = ? ORDER BY day_of_week"
        ).use { st ->
            st.setString(1, doctorId)
            st.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) ClinicHours(
                        dayOfWeek = DayOfWeek.of(rs.getInt("day_of_week")),
                        opensAt = rs.getObject("opens_at", LocalTime::class.java),
                        closesAt = rs.getObject("closes_at", LocalTime::class.java),
                    ) else null
                }.toList()
            }
        }
        row.toDoctor(hours)
    }

    /**
     * Dates in [month] with at least one slot still open.
     *
     * Slots for the entire date range are materialised in one pass, then a
     * single GROUP BY query counts open slots per day — 2 queries total instead
     * of 2 per day (was 62 for a full month).
     */
    fun availableDates(doctorId: String, month: LocalDate): List<AvailableDate> {
        val yearMonth = YearMonth.from(month)
        // Do not offer dates that have already passed.
        val today = LocalDate.now(CLINIC_ZONE)
        val firstDay = maxOf(yearMonth.atDay(1), today)
        val lastDay = yearMonth.atEndOfMonth()

        if (firstDay.isAfter(lastDay)) return emptyList()

        return db.read { c ->
            // Materialise slots for every day in the range in one pass.
            var date = firstDay
            while (!date.isAfter(lastDay)) {
                ensureSlots(c, doctorId, date)
                date = date.plusDays(1)
            }

            // Count open slots grouped by date in a single query.
            val rangeStart = firstDay.atStartOfDay(CLINIC_ZONE).toInstant()
            val rangeEnd = lastDay.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant()
            // FORMATDATETIME is given the clinic zone explicitly instead of
            // inheriting the session zone. `CAST(starts_at AS DATE)` looks
            // equivalent and is not: H2 resolves it through the session zone, so
            // a host booting in UTC reports an 01:00 Manila clinic under the
            // *previous* day. `AT TIME ZONE` does not help — H2 ignores it for
            // this conversion. AvailableDatesTest runs this query under three
            // session zones so the guarantee cannot silently regress.
            // The zone is a literal rather than a bound parameter: H2 rejects a
            // parameter inside an aggregate's expression, so it has to be
            // rendered into the text. Safe to interpolate because it is
            // CLINIC_ZONE, a compile-time constant, not request input. The
            // per-row filter values stay bound.
            val zone = CLINIC_ZONE.id.replace("'", "''")
            c.prepareStatement(
                """
                SELECT slot_date, COUNT(*) AS open_count
                FROM (
                    SELECT FORMATDATETIME(s.starts_at, 'yyyy-MM-dd', 'en', '$zone') AS slot_date
                    FROM slots s
                    WHERE s.doctor_id = ? AND s.status = 'available'
                      AND s.starts_at >= ? AND s.starts_at < ?
                      AND NOT EXISTS (SELECT 1 FROM slot_claims sc WHERE sc.slot_id = s.id)
                )
                GROUP BY slot_date
                ORDER BY slot_date
                """.trimIndent()
            ).use { st ->
                st.setString(1, doctorId)
                st.setObject(2, rangeStart.atOffset(ZoneOffset.UTC))
                st.setObject(3, rangeEnd.atOffset(ZoneOffset.UTC))
                st.executeQuery().use { rs ->
                    generateSequence {
                        if (rs.next()) {
                            // FORMATDATETIME returns the formatted string, not
                            // a SQL DATE, so it is parsed rather than read as
                            // a date object.
                            val localDate = LocalDate.parse(rs.getString("slot_date"))
                            AvailableDate(localDate, rs.getInt("open_count"))
                        } else null
                    }.toList()
                }
            }
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

/**
 * Flat projection of the doctors table columns — no associated collections.
 * Callers fetch clinic_hours separately (in bulk) and assemble via [toDoctor].
 */
private data class DoctorRow(
    val id: String,
    val fullName: String,
    val specialtyWire: String,
    val yearsOfExperience: Int,
    val feeCentavos: Long,
    val licenseNumber: String,
    val bio: String,
    val building: String,
    val floor: String,
    val room: String,
    val languagesRaw: String,
)

private fun java.sql.ResultSet.readDoctorRow() = DoctorRow(
    id = getString("id"),
    fullName = getString("full_name"),
    specialtyWire = getString("specialty_id"),
    yearsOfExperience = getInt("years_experience"),
    feeCentavos = getLong("fee_centavos"),
    licenseNumber = getString("license_number"),
    bio = getString("bio"),
    building = getString("building"),
    floor = getString("floor"),
    room = getString("room"),
    languagesRaw = getString("languages"),
)

private fun DoctorRow.toDoctor(hours: List<ClinicHours>): Doctor {
    val languages = languagesRaw
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { wire -> com.example.mediq.domain.model.ConsultationLanguage.entries.firstOrNull { it.wireValue == wire } }

    return Doctor(
        id = id,
        fullName = fullName,
        specialty = Specialty.entries.first { it.wireValue == specialtyWire },
        yearsOfExperience = yearsOfExperience,
        consultationFee = Money(feeCentavos),
        location = com.example.mediq.domain.model.ClinicLocation(
            building = building,
            floor = floor,
            room = room,
        ),
        licenseNumber = licenseNumber,
        bio = bio,
        languages = languages,
        clinicHours = hours,
    )
}