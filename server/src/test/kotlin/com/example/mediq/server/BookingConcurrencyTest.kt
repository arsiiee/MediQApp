package com.example.mediq.server

import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Sex
import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.UserStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The test that justifies the booking design.
 *
 * Everything else here can be verified by using the app. This cannot — the
 * failure it guards against only appears when two requests arrive at exactly
 * the same moment, which is exactly when nobody is testing. "It seems to
 * work" is not evidence for this; the only evidence is many simultaneous
 * attempts at one slot, where exactly one wins.
 */
class BookingConcurrencyTest {

    private val config = ServerConfig(
        port = 0,
        // A private in-memory database per test, so schema.sql is applied
        // fresh and tests cannot see each other's rows.
        jdbcUrl = "jdbc:h2:mem:t_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
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

    @Test
    fun `only one of many simultaneous bookings for the same slot succeeds`() {
        val doctorId = createDoctorWithMondayClinic()
        val slot = firstSlotFor(doctorId)

        val attempts = 12
        val startLine = CountDownLatch(1)
        val done = CountDownLatch(attempts)
        val succeeded = AtomicInteger()
        val conflicts = AtomicInteger()
        val unexpected = java.util.Collections.synchronizedList(mutableListOf<String>())

        val pool = Executors.newFixedThreadPool(attempts)
        try {
            val tasks = (1..attempts).map { index ->
                pool.submit(
                    Callable {
                        val userId = createPatient("patient$index")
                        startLine.await()
                        try {
                            appointments.book(
                                userId = userId,
                                request = BookingRequest(
                                    slotId = slot,
                                    reasonForVisit = null,
                                    confirmedByPatient = true,
                                ),
                            )
                            succeeded.incrementAndGet()
                        } catch (e: ApiError) {
                            if (e.code == "slot_taken") conflicts.incrementAndGet()
                            else unexpected.add("ApiError ${e.code}: ${e.message}")
                        } catch (e: Throwable) {
                            unexpected.add("${e::class.simpleName}: ${e.message}")
                        } finally {
                            done.countDown()
                        }
                    }
                )
            }

            // Every thread blocks on the latch, so all twelve hit the database
            // together rather than trickling in one at a time.
            startLine.countDown()
            assertTrue(done.await(60, TimeUnit.SECONDS), "bookings did not finish in time")
        } finally {
            pool.shutdownNow()
        }

        assertEquals(
            emptyList(),
            unexpected,
            "some attempts failed in an unexpected way instead of being rejected cleanly",
        )
        assertEquals(1, succeeded.get(), "exactly one booking must win the slot")
        assertEquals(attempts - 1, conflicts.get(), "every other attempt must be rejected as taken")

        // The decisive check: not "the app said it failed" but "only one
        // booking exists in the database".
        assertEquals(1, countAppointmentsOn(slot))
        assertEquals(1, countClaimsOn(slot))
    }

    @Test
    fun `cancelling releases the slot so it can be booked again`() {
        val doctorId = createDoctorWithMondayClinic()
        val slot = firstSlotFor(doctorId)
        val userId = createPatient("canceller")

        val appointment = appointments.book(
            userId = userId,
            request = BookingRequest(slotId = slot, reasonForVisit = null, confirmedByPatient = true),
        )
        appointments.cancel(userId, com.example.mediq.domain.model.UserRole.PATIENT, appointment.id)

        assertEquals(0, countClaimsOn(slot), "cancelling must free the slot")

        // A different patient can now take it.
        val second = createPatient("second")
        appointments.book(second, BookingRequest(slot, null, true))
        assertEquals(1, countClaimsOn(slot))
    }

    @Test
    fun `a patient cannot read another patient's appointment`() {
        val doctorId = createDoctorWithMondayClinic()
        val slot = firstSlotFor(doctorId)
        val owner = createPatient("owner")
        val stranger = createPatient("stranger")

        val appointment = appointments.book(owner, BookingRequest(slot, "chest pain", true))

        val denied = runCatching {
            appointments.get(stranger, com.example.mediq.domain.model.UserRole.PATIENT, appointment.id)
        }.exceptionOrNull()

        // notFound rather than forbidden, so the response does not confirm the
        // appointment exists.
        assertTrue(denied is ApiError, "expected an ApiError, got $denied")
        assertEquals("not_found", (denied as ApiError).code)
    }

    @Test
    fun `signing out invalidates the token before it expires`() {
        val auth = AuthService(db, users, tokens, 60, 5)
        val user = createPatient("signoutuser")
        val password = "correct-horse-battery"

        db.tx { c ->
            c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?").use { st ->
                st.setString(1, com.example.mediq.server.auth.Passwords.hash(password))
                st.setString(2, user)
                st.executeUpdate()
            }
        }

        val (token, _) = auth.signIn("signoutuser", password)
        assertTrue(auth.authenticate(token.value) != null, "token should work right after sign-in")

        val claims = tokens.verify(token.value)!!
        auth.signOut(claims.sessionId)

        // Still inside the token's lifetime, but the session is gone.
        assertEquals(null, auth.authenticate(token.value), "token must stop working after sign-out")
    }

    // --- Fixtures -----------------------------------------------------------

    private fun createPatient(name: String): String = users.create(
        username = name,
        password = "placeholder-never-used",
        fullName = name,
        email = null,
        mobileNumber = "+639" + (1_000_000_000L + name.hashCode().toLong().let { kotlin.math.abs(it) }),
        dateOfBirth = LocalDate.of(1990, 1, 1),
        sex = Sex.PREFER_NOT_TO_SAY,
    ).id

    /** Inserts a doctor with a one-hour window on the next day that has clinic hours. */
    private fun createDoctorWithMondayClinic(): String {
        val doctorId = UUID.randomUUID().toString()
        db.tx { c ->
            // H2 understands MERGE, which is already what schema.sql and
            // DoctorStore use. Do not assume MySQL syntax here.
            c.prepareStatement(
                "MERGE INTO specialties (id, display_name) KEY (id) VALUES ('internal_medicine', 'Internal Medicine')"
            ).use { it.executeUpdate() }

            c.prepareStatement(
                """
                INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                     license_number, bio, building, floor, room, languages, created_at)
                VALUES (?, 'Test Doctor', 'internal_medicine', 5, 70000,
                        'TEST-LICENCE', 'Test bio', 'Main', '1F', '101', 'en', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, doctorId)
                st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }

            // Open every weekday so any nearby date has slots.
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
        error("no bookable slot was generated within two weeks")
    }

    private fun countClaimsOn(slotId: String): Int = db.read { c ->
        c.prepareStatement("SELECT COUNT(*) FROM slot_claims WHERE slot_id = ?").use { st ->
            st.setString(1, slotId)
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun countAppointmentsOn(slotId: String): Int = db.read { c ->
        c.prepareStatement("SELECT COUNT(*) FROM appointments WHERE slot_id = ?").use { st ->
            st.setString(1, slotId)
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }
}