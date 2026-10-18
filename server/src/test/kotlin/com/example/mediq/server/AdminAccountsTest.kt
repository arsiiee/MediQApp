package com.example.mediq.server

import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.UserRow
import com.example.mediq.server.db.UserStore
import com.example.mediq.server.http.toAdminDto
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The reads the admin area's directories are built on.
 *
 * Two things were missing until this: `users` had no way to be listed at all,
 * and the two columns the directories already displayed — when an account was
 * created, and which doctor a staff account sits behind — were in the table but
 * never selected, so nothing could report them. These pin both, plus the
 * `GROUP BY` behind the specialization counts.
 *
 * Every test gets its own database. They create accounts of overlapping roles
 * and assert on whole lists, so a shared one would make each assertion depend on
 * which test ran first.
 */
class AdminAccountsTest {

    @Test
    fun `listByRole returns the role asked for and nothing else`() {
        val f = Fixture()
        f.create(UserRole.DOCTOR, "Zoe Doctor")
        f.create(UserRole.DOCTOR, "Amy Doctor")
        f.create(UserRole.SECRETARY, "Sam Secretary")
        f.create(UserRole.PATIENT, "Pat Patient")

        // Ordered here rather than by the screens, so two screens cannot
        // disagree about the order of the same list.
        assertEquals(
            listOf("Amy Doctor", "Zoe Doctor"),
            f.users.listByRole(UserRole.DOCTOR).map { it.fullName },
        )
        assertEquals(listOf("Sam Secretary"), f.users.listByRole(UserRole.SECRETARY).map { it.fullName })
        assertEquals(listOf("Pat Patient"), f.users.listByRole(UserRole.PATIENT).map { it.fullName })

        // The load-bearing one: a role nobody holds must come back empty rather
        // than come back as the whole table, which is what a filter that was
        // dropped from the SQL would do while every other assertion still passed.
        assertTrue(f.users.listByRole(UserRole.ADMIN).isEmpty(), "an empty directory must be empty")
    }

    @Test
    fun `an account carries its creation time and its linked doctor`() {
        val f = Fixture()
        val doctorRowId = f.insertDoctor(Specialty.CARDIOLOGY)
        val linked = f.create(UserRole.SECRETARY, "Linked Secretary")
        val unlinked = f.create(UserRole.ADMIN, "Unlinked Admin")

        // Written as a column rather than through `create`: until now only the
        // demo seed ever set `doctor_id`, so this is the write every reader of
        // the field depends on being visible.
        f.run("UPDATE users SET doctor_id = ? WHERE id = ?", doctorRowId, linked.id)

        assertEquals(doctorRowId, f.users.findById(linked.id)?.doctorId)
        assertNull(f.users.findById(unlinked.id)?.doctorId, "staff with no doctor must read as null")
    }

    @Test
    fun `creation time is the real one and not a default`() {
        val f = Fixture()
        val before = Instant.now()
        val created = f.create(UserRole.PATIENT, "Dated Patient")

        val row = f.users.findById(created.id)!!
        // A row that never selected the column would read as the epoch, and a
        // row that selected it but dropped it in mapping would read as null.
        assertTrue(
            !row.createdAt.isBefore(before.minusSeconds(60)) && !row.createdAt.isAfter(Instant.now()),
            "createdAt must be the account's creation time, was ${row.createdAt}",
        )
    }

    @Test
    fun `the admin DTO carries every field the directories read`() {
        val f = Fixture()
        val created = f.create(UserRole.PATIENT, "Dto Patient", email = "dto@example.invalid")

        val dto = created.toAdminDto()

        // Asserted on the DTO and not only on the row: the JSON config sets
        // `explicitNulls = false`, so a field that is null — or renamed to
        // something the client does not read — leaves the response silently
        // rather than failing anything.
        assertEquals(created.id, dto.id)
        assertEquals(created.username, dto.username)
        assertEquals("Dto Patient", dto.fullName)
        assertEquals("dto@example.invalid", dto.email)
        assertEquals(created.mobileNumber, dto.mobileNumber)
        assertEquals(created.dateOfBirth.toString(), dto.dateOfBirth)
        assertEquals("patient", dto.role)
        assertEquals(created.createdAt.toString(), dto.createdAt)
        assertNull(dto.doctorId, "a patient has no doctor behind it")
    }

    @Test
    fun `specialty counts count the doctors under each specialty`() {
        val f = Fixture()
        f.insertDoctor(Specialty.CARDIOLOGY)
        f.insertDoctor(Specialty.CARDIOLOGY)
        f.insertDoctor(Specialty.PEDIATRICS)

        val counts = f.doctors.specialtyCounts()

        assertEquals(2, counts[Specialty.CARDIOLOGY.wireValue])
        assertEquals(1, counts[Specialty.PEDIATRICS.wireValue])
        // Absent, not zero: the route fills the gap from the enum so a specialty
        // nobody practises still shows. A store that invented zeroes here would
        // hide which specialties it never asked about.
        assertNull(
            counts[Specialty.ORTHOPEDICS.wireValue],
            "a specialty with no doctors must be absent from the counts, not present as zero",
        )
    }

    /** Its own database, so one test's accounts cannot reach another's assertions. */
    private class Fixture {
        private val config = ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:admin_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            jwtSecret = "test-secret-not-used-for-anything-real-123456",
            jwtIssuer = "mediq-test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
        )

        private val db = Database(config)
        val users = UserStore(db)
        val doctors = DoctorStore(db, config.slotDurationMinutes)

        fun create(role: UserRole, name: String, email: String? = null): UserRow = users.create(
            username = "acct_${UUID.randomUUID().toString().take(8)}",
            password = "placeholder-never-used",
            fullName = name,
            email = email,
            mobileNumber = mobile(),
            dateOfBirth = LocalDate.of(1990, 1, 1),
            sex = Sex.PREFER_NOT_TO_SAY,
            role = role,
        )

        /** A `doctors` row, so the specialty counts have something to count. */
        fun insertDoctor(specialty: Specialty): String {
            val id = UUID.randomUUID().toString()
            db.tx { c ->
                c.prepareStatement("MERGE INTO specialties (id, display_name) KEY (id) VALUES (?, ?)")
                    .use { st ->
                        st.setString(1, specialty.wireValue)
                        st.setString(2, specialty.displayName)
                        st.executeUpdate()
                    }
                c.prepareStatement(
                    """
                    INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                         license_number, bio, building, floor, room, languages, created_at)
                    VALUES (?, 'Admin Test Doctor', ?, 5, 70000, 'TEST-LICENCE', 'Test bio',
                            'Main', '1F', '101', 'en', ?)
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, id)
                    st.setString(2, specialty.wireValue)
                    st.setObject(3, OffsetDateTime.now(ZoneOffset.UTC))
                    st.executeUpdate()
                }
            }
            return id
        }

        fun run(sql: String, vararg params: String) = db.tx { c ->
            c.prepareStatement(sql).use { st ->
                params.forEachIndexed { i, p -> st.setString(i + 1, p) }
                st.executeUpdate()
            }
        }

        /** Unique per call: `mobile_number` is unique, so a constant would trip the second insert. */
        private fun mobile(): String {
            val digits = (1_000_000_000L + kotlin.math.abs(UUID.randomUUID().hashCode().toLong()))
                .toString()
                .takeLast(9)
            return "+639$digits"
        }
    }
}
