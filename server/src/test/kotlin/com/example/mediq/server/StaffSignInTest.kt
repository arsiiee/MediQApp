package com.example.mediq.server

import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.UserStatus
import com.example.mediq.server.db.UserStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The staff half of sign-in, added for the Angular frontend.
 *
 * `users.role` and the token's `role` claim already carried all four roles, but
 * nothing proved it: every account the server could create was a patient, so a
 * doctor signing in was an untested path. These pin the three roles the web
 * signs in with, the seeded accounts it signs in as, and the one behaviour the
 * staff surface already depends on — a non-patient caller sees every
 * appointment instead of only their own. If that widening ever narrows, a
 * doctor's queue goes quietly empty rather than obviously wrong.
 */
class StaffSignInTest {

    private val config = ServerConfig(
        port = 0,
        jdbcUrl = "jdbc:h2:mem:staff_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        jwtSecret = "test-secret-not-used-for-anything-real-123456",
        jwtIssuer = "mediq-test",
        tokenTtlMinutes = 60,
        slotDurationMinutes = 30,
        otpTtlMinutes = 5,
        seedDemoData = false,
    )

    private val db = Database(config)
    private val users = UserStore(db)
    private val doctors = DoctorStore(db, config.slotDurationMinutes)
    private val appointments = AppointmentStore(db, doctors)
    private val tokens = Tokens(config.jwtSecret, config.jwtIssuer)
    private val auth = AuthService(db, users, tokens, config.tokenTtlMinutes, config.otpTtlMinutes)

    // -------------------------------------------------------------------------
    // Sign-in, by role
    // -------------------------------------------------------------------------

    @Test
    fun `every staff role signs in and gets its own role back`() {
        val staffRoles = listOf(UserRole.DOCTOR, UserRole.SECRETARY, UserRole.ADMIN)

        for (role in staffRoles) {
            val user = createStaff(role)
            val (token, profile) = auth.signIn(user.username, PASSWORD)

            assertEquals(role, profile.role, "$role must sign in as itself")
            // The route layer reads the role from the token, not from the row,
            // so a token that disagreed with the profile would authorize the
            // wrong thing while looking right in every other assertion.
            assertEquals(role.wireValue, tokens.verify(token.value)?.role)
        }
    }

    @Test
    fun `signIn refuses a staff account with the wrong password`() {
        val admin = createStaff(UserRole.ADMIN)

        val error = assertFailsWith<ApiError> {
            auth.signIn(admin.username, "not-the-password")
        }
        assertEquals("unauthorized", error.code)
    }

    @Test
    fun `signOut kills a staff session like a patient's`() {
        val doctor = createStaff(UserRole.DOCTOR)
        val (token, _) = auth.signIn(doctor.username, PASSWORD)

        assertNotNull(auth.authenticate(token.value), "the fresh staff token must work")

        auth.signOut(tokens.verify(token.value)!!.sessionId)

        assertNull(auth.authenticate(token.value), "sign-out must revoke a staff session too")
    }

    // -------------------------------------------------------------------------
    // A disabled account
    // -------------------------------------------------------------------------

    @Test
    fun `signIn refuses a disabled account, and only after the password matches`() {
        val secretary = createStaff(UserRole.SECRETARY)
        users.setStatus(secretary.id, UserStatus.INACTIVE)

        // The order is the point of this being one test rather than two. If the
        // status were checked before the password, the reply would tell anyone
        // who typed a username that the account exists — the one thing the
        // shared sign-in message exists to hide. Reaching the disabled message
        // has to require knowing the password first.
        val wrongPassword = assertFailsWith<ApiError> { auth.signIn(secretary.username, "not-the-password") }
        assertEquals("Those details don't match an account.", wrongPassword.message)

        val disabled = assertFailsWith<ApiError> { auth.signIn(secretary.username, PASSWORD) }
        assertEquals("That account is not active. Ask an administrator to re-enable it.", disabled.message)
    }

    @Test
    fun `disabling an account stops a token that was already issued`() {
        val doctor = createStaff(UserRole.DOCTOR)
        val (token, _) = auth.signIn(doctor.username, PASSWORD)
        assertNotNull(auth.authenticate(token.value), "the fresh token must work")

        users.setStatus(doctor.id, UserStatus.INACTIVE)

        // The reason the session row is checked on every request at all is that a
        // signed token otherwise stays valid until it expires. Disabling has to
        // ride the same check, or an admin's Disable button would take up to an
        // hour to mean anything.
        assertNull(auth.authenticate(token.value), "a disabled account's token must stop working now")
    }

    @Test
    fun `re-enabling an account lets it sign in again`() {
        val admin = createStaff(UserRole.ADMIN)
        users.setStatus(admin.id, UserStatus.INACTIVE)
        users.setStatus(admin.id, UserStatus.ACTIVE)

        val (token, profile) = auth.signIn(admin.username, PASSWORD)

        // Reversible from the same column, which is what the admin screen's
        // Enable button relies on: nothing about the account was deleted.
        assertEquals(UserRole.ADMIN, profile.role)
        assertNotNull(auth.authenticate(token.value))
    }

    // -------------------------------------------------------------------------
    // The staff widening the web already relies on
    // -------------------------------------------------------------------------

    @Test
    fun `a doctor sees every appointment while a patient sees only their own`() {
        val doctorId = createDoctorWithClinic()
        val mine = createPatient("staff_scope_mine")
        val other = createPatient("staff_scope_other")

        val slotA = firstSlotFor(doctorId)
        val slotB = secondSlotFor(doctorId, excludeSlotId = slotA)

        appointments.book(mine, BookingRequest(slotA, null, true))
        appointments.book(other, BookingRequest(slotB, null, true))

        val (asPatient, _) = appointments.list(mine, UserRole.PATIENT, AppointmentFilter.ALL, 50, 0)
        val (asStaff, _) = appointments.list(mine, UserRole.DOCTOR, AppointmentFilter.ALL, 50, 0)

        assertEquals(1, asPatient.size, "a patient must see only their own appointment")
        assertEquals(2, asStaff.size, "a doctor must see both appointments")
    }

    // -------------------------------------------------------------------------
    // The demo seed the web signs in as
    // -------------------------------------------------------------------------

    @Test
    fun `the demo seed creates one signable account per staff role`() {
        // Its own database: this test seeds doctors and is the only one here
        // that does, so sharing the class-level database would leak demo rows
        // into whichever test ran next.
        val seedConfig = config.copy(
            jdbcUrl = "jdbc:h2:mem:seed_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        )
        val seedDb = Database(seedConfig)
        try {
            ReferenceData.seedSpecialties(seedDb)
            DemoData.seed(seedDb)

            val seedUsers = UserStore(seedDb)
            val seedAuth = AuthService(seedDb, seedUsers, tokens, config.tokenTtlMinutes, config.otpTtlMinutes)

            val expected = listOf(
                "demo_admin" to UserRole.ADMIN,
                "demo_doctor" to UserRole.DOCTOR,
                "demo_secretary" to UserRole.SECRETARY,
            )
            for ((username, role) in expected) {
                val (_, profile) = seedAuth.signIn(username, "demo12345")
                assertEquals(role, profile.role, "$username must sign in as $role")
            }

            // The two clinical roles sit behind a real doctor row, so their
            // screens have a doctor to show; the admin does not.
            assertNotNull(doctorLinkOf(seedDb, "demo_doctor"), "demo_doctor must be linked to a doctor")
            assertNotNull(doctorLinkOf(seedDb, "demo_secretary"), "demo_secretary must be linked to a doctor")
            assertNull(doctorLinkOf(seedDb, "demo_admin"), "the admin has no doctor behind it")
        } finally {
            seedDb.close()
        }
    }

    @Test
    fun `seeding twice is a no-op rather than a primary key failure`() {
        val seedConfig = config.copy(
            jdbcUrl = "jdbc:h2:mem:seed_twice_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        )
        val seedDb = Database(seedConfig)
        try {
            ReferenceData.seedSpecialties(seedDb)
            DemoData.seed(seedDb)
            // A second boot against the same database is the normal case for a
            // real deployment, and every INSERT here is guarded per username.
            DemoData.seed(seedDb)

            val count = seedDb.read { c ->
                c.prepareStatement("SELECT COUNT(*) FROM users WHERE role <> 'patient'").use { st ->
                    st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
                }
            }
            assertEquals(3, count, "three staff accounts, not six")
        } finally {
            seedDb.close()
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun createStaff(role: UserRole) = users.create(
        username = "staff_${role.wireValue}_${UUID.randomUUID().toString().take(8)}",
        password = PASSWORD,
        fullName = "Staff ${role.wireValue}",
        email = null,
        mobileNumber = "+6390000000" + (10 + kotlin.math.abs(UUID.randomUUID().hashCode()) % 89),
        dateOfBirth = LocalDate.of(1985, 5, 5),
        sex = Sex.PREFER_NOT_TO_SAY,
        role = role,
    )

    private fun doctorLinkOf(db: Database, username: String): String? = db.read { c ->
        c.prepareStatement("SELECT doctor_id FROM users WHERE username = ?").use { st ->
            st.setString(1, username)
            st.executeQuery().use { rs -> if (rs.next()) rs.getString("doctor_id") else null }
        }
    }

    private fun createPatient(name: String): String = users.create(
        username = name,
        password = "placeholder-never-used",
        fullName = name,
        email = null,
        mobileNumber = "+639" + (1_000_000_000L + kotlin.math.abs(name.hashCode().toLong())).toString().takeLast(9),
        dateOfBirth = LocalDate.of(1990, 1, 1),
        sex = Sex.PREFER_NOT_TO_SAY,
    ).id

    private fun createDoctorWithClinic(): String {
        val doctorId = UUID.randomUUID().toString()
        db.tx { c ->
            c.prepareStatement(
                "MERGE INTO specialties (id, display_name) KEY (id) VALUES ('internal_medicine', 'Internal Medicine')"
            ).use { it.executeUpdate() }

            c.prepareStatement(
                """
                INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                     license_number, bio, building, floor, room, languages, created_at)
                VALUES (?, 'Staff Test Doctor', 'internal_medicine', 5, 70000,
                        'TEST-LICENCE', 'Test bio', 'Main', '1F', '101', 'en', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, doctorId)
                st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }

            c.prepareStatement(
                """
                INSERT INTO clinic_hours (id, doctor_id, day_of_week, opens_at, closes_at)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { st ->
                (1..5).forEach { day ->
                    st.setString(1, UUID.randomUUID().toString())
                    st.setString(2, doctorId)
                    st.setInt(3, day)
                    st.setObject(4, LocalTime.of(8, 0))
                    st.setObject(5, LocalTime.of(18, 0))
                    st.addBatch()
                }
                st.executeBatch()
            }
        }
        return doctorId
    }

    private fun firstSlotFor(doctorId: String): String {
        var date = LocalDate.now(com.example.mediq.domain.model.CLINIC_ZONE).plusDays(1)
        repeat(14) {
            val slot = doctors.slots(doctorId, date).firstOrNull { it.isBookable }
            if (slot != null) return slot.id
            date = date.plusDays(1)
        }
        error("no bookable slot within two weeks")
    }

    private fun secondSlotFor(doctorId: String, excludeSlotId: String): String {
        var date = LocalDate.now(com.example.mediq.domain.model.CLINIC_ZONE).plusDays(1)
        repeat(14) {
            val slot = doctors.slots(doctorId, date).firstOrNull { it.isBookable && it.id != excludeSlotId }
            if (slot != null) return slot.id
            date = date.plusDays(1)
        }
        error("no second bookable slot within two weeks")
    }

    private companion object {
        const val PASSWORD = "staff-demo-password"
    }
}
