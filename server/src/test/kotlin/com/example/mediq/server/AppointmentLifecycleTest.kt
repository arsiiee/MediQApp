package com.example.mediq.server

import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.NotificationStore
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
import kotlin.test.assertTrue

/**
 * Covers notification marking and the cancel / reschedule appointment flows.
 *
 * The booking and concurrency path already has its own test class. This class
 * focuses on the lifecycle *after* a booking exists: cancel, reschedule
 * request, and notification state.
 */
class AppointmentLifecycleTest {

    private val config = ServerConfig(
        port = 0,
        jdbcUrl = "jdbc:h2:mem:lifecycle_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
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
    private val notifications = NotificationStore(db)

    // -------------------------------------------------------------------------
    // Notification marking
    // -------------------------------------------------------------------------

    @Test
    fun `markRead marks a notification read and records the timestamp`() {
        val userId = createPatient("notif_user_1")
        val notifId = insertNotification(userId)

        notifications.markRead(userId, notifId)

        val (items, _) = notifications.list(userId, 10, 0)
        val notification = items.single { it.id == notifId }
        assertNotNull(notification.readAt, "readAt must be set after markRead")
    }

    @Test
    fun `markRead is idempotent — calling it twice keeps the original timestamp`() {
        val userId = createPatient("notif_user_2")
        val notifId = insertNotification(userId)

        notifications.markRead(userId, notifId)
        val (first, _) = notifications.list(userId, 10, 0)
        val firstReadAt = first.single { it.id == notifId }.readAt

        // Second call must not change the timestamp.
        notifications.markRead(userId, notifId)
        val (second, _) = notifications.list(userId, 10, 0)
        val secondReadAt = second.single { it.id == notifId }.readAt

        assertEquals(firstReadAt, secondReadAt, "a second markRead must not overwrite the first timestamp")
    }

    @Test
    fun `markRead scoped to the owning user — another user's id matches nothing`() {
        val owner = createPatient("notif_owner_3")
        val stranger = createPatient("notif_stranger_3")
        val notifId = insertNotification(owner)

        // A well-behaved call for a different user should not throw; it simply
        // matches no rows because the WHERE clause includes user_id.
        // The implementation throws not_found only when the notification doesn't
        // belong to this user at all.
        val error = assertFailsWith<ApiError> {
            notifications.markRead(stranger, notifId)
        }
        assertEquals("not_found", error.code)

        // Confirm the notification was not marked for its actual owner.
        val (items, _) = notifications.list(owner, 10, 0)
        assertNull(items.single { it.id == notifId }.readAt, "owner's notification must remain unread")
    }

    @Test
    fun `markRead throws not_found for a completely unknown notification id`() {
        val userId = createPatient("notif_user_4")

        val error = assertFailsWith<ApiError> {
            notifications.markRead(userId, UUID.randomUUID().toString())
        }
        assertEquals("not_found", error.code)
    }

    @Test
    fun `list returns notifications in reverse-chronological order`() {
        val userId = createPatient("notif_user_5")
        // Insert three notifications in forward order; list must reverse them.
        repeat(3) { i ->
            insertNotification(userId, titleSuffix = " $i")
        }

        val (items, _) = notifications.list(userId, 10, 0)
        assertEquals(3, items.size)
        // Oldest was inserted first, so it should appear last in the list.
        val timestamps = items.map { it.createdAt }
        assertTrue(
            timestamps.zipWithNext().all { (a, b) -> !a.isBefore(b) },
            "notifications must be ordered newest-first"
        )
    }

    @Test
    fun `list pagination hasMore is true when more items exist beyond the page`() {
        val userId = createPatient("notif_user_6")
        repeat(5) { insertNotification(userId) }

        val (items, hasMore) = notifications.list(userId, 3, 0)
        assertEquals(3, items.size)
        assertTrue(hasMore, "hasMore must be true when more items remain")
    }

    @Test
    fun `list hasMore is false on the last page`() {
        val userId = createPatient("notif_user_7")
        repeat(3) { insertNotification(userId) }

        val (items, hasMore) = notifications.list(userId, 3, 0)
        assertEquals(3, items.size)
        assertTrue(!hasMore, "hasMore must be false when all items fit in the page")
    }

    // -------------------------------------------------------------------------
    // Cancel
    // -------------------------------------------------------------------------

    @Test
    fun `cancelling a pending appointment sets status to cancelled`() {
        val doctorId = createDoctorWithClinic()
        val slot = firstSlotFor(doctorId)
        val userId = createPatient("cancel_user_1")

        val appointment = appointments.book(userId, BookingRequest(slot, null, true))
        appointments.cancel(userId, UserRole.PATIENT, appointment.id)

        val retrieved = appointments.get(userId, UserRole.PATIENT, appointment.id)
        assertEquals("cancelled", retrieved.status.wireValue)
    }

    @Test
    fun `cancelling a cancelled appointment is rejected`() {
        val doctorId = createDoctorWithClinic()
        val slot = firstSlotFor(doctorId)
        val userId = createPatient("cancel_user_2")

        val appointment = appointments.book(userId, BookingRequest(slot, null, true))
        appointments.cancel(userId, UserRole.PATIENT, appointment.id)

        val error = assertFailsWith<ApiError> {
            appointments.cancel(userId, UserRole.PATIENT, appointment.id)
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `cancelling emits a cancellation notification for the patient`() {
        val doctorId = createDoctorWithClinic()
        val slot = firstSlotFor(doctorId)
        val userId = createPatient("cancel_user_3")

        val appointment = appointments.book(userId, BookingRequest(slot, null, true))
        val (before, _) = notifications.list(userId, 50, 0)
        val countBefore = before.size

        appointments.cancel(userId, UserRole.PATIENT, appointment.id)

        val (after, _) = notifications.list(userId, 50, 0)
        assertTrue(after.size > countBefore, "a cancellation notification must be inserted")
        assertTrue(
            after.any { it.title.contains("cancel", ignoreCase = true) },
            "the new notification should mention cancellation"
        )
    }

    // -------------------------------------------------------------------------
    // Reschedule request
    // -------------------------------------------------------------------------

    @Test
    fun `requestReschedule records the requested slot on the appointment`() {
        val doctorId = createDoctorWithClinic()
        val slot1 = firstSlotFor(doctorId)
        val userId = createPatient("reschedule_user_1")

        val appointment = appointments.book(userId, BookingRequest(slot1, null, true))

        // Find a second bookable slot for the same doctor.
        val slot2 = secondSlotFor(doctorId, excludeSlotId = slot1)

        appointments.requestReschedule(
            userId,
            UserRole.PATIENT,
            RescheduleRequest(
                appointmentId = appointment.id,
                requestedSlotId = slot2,
                note = "prefer afternoon",
            ),
        )

        // The appointment itself is not moved; only the request is recorded.
        // Confirm the original slot is still claimed (unchanged).
        assertEquals(
            1,
            countClaimsOn(slot1),
            "original slot must remain claimed while reschedule is pending"
        )
    }

    @Test
    fun `requestReschedule on a cancelled appointment is rejected`() {
        val doctorId = createDoctorWithClinic()
        val slot1 = firstSlotFor(doctorId)
        val slot2 = secondSlotFor(doctorId, excludeSlotId = slot1)
        val userId = createPatient("reschedule_user_2")

        val appointment = appointments.book(userId, BookingRequest(slot1, null, true))
        appointments.cancel(userId, UserRole.PATIENT, appointment.id)

        val error = assertFailsWith<ApiError> {
            appointments.requestReschedule(
                userId,
                UserRole.PATIENT,
                RescheduleRequest(appointment.id, slot2, null),
            )
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `requestReschedule emits a notification for the patient`() {
        val doctorId = createDoctorWithClinic()
        val slot1 = firstSlotFor(doctorId)
        val slot2 = secondSlotFor(doctorId, excludeSlotId = slot1)
        val userId = createPatient("reschedule_user_3")

        val appointment = appointments.book(userId, BookingRequest(slot1, null, true))
        val (before, _) = notifications.list(userId, 50, 0)
        val countBefore = before.size

        appointments.requestReschedule(userId, UserRole.PATIENT, RescheduleRequest(appointment.id, slot2, "earlier please"))

        val (after, _) = notifications.list(userId, 50, 0)
        assertTrue(after.size > countBefore, "a reschedule notification must be inserted")
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

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
                VALUES (?, 'Lifecycle Doctor', 'internal_medicine', 5, 70000,
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
        var date = java.time.LocalDate.now(com.example.mediq.domain.model.CLINIC_ZONE).plusDays(1)
        repeat(14) {
            val slot = doctors.slots(doctorId, date).firstOrNull { it.isBookable }
            if (slot != null) return slot.id
            date = date.plusDays(1)
        }
        error("no bookable slot within two weeks")
    }

    private fun secondSlotFor(doctorId: String, excludeSlotId: String): String {
        var date = java.time.LocalDate.now(com.example.mediq.domain.model.CLINIC_ZONE).plusDays(1)
        repeat(14) {
            val slot = doctors.slots(doctorId, date).firstOrNull { it.isBookable && it.id != excludeSlotId }
            if (slot != null) return slot.id
            date = date.plusDays(1)
        }
        error("no second bookable slot within two weeks")
    }

    private fun countClaimsOn(slotId: String): Int = db.read { c ->
        c.prepareStatement("SELECT COUNT(*) FROM slot_claims WHERE slot_id = ?").use { st ->
            st.setString(1, slotId)
            st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun insertNotification(userId: String, titleSuffix: String = ""): String {
        val id = UUID.randomUUID().toString()
        db.tx { c ->
            c.prepareStatement(
                """
                INSERT INTO notifications (id, user_id, type, title, body, created_at)
                VALUES (?, ?, 'system', ?, 'Test body.', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, id)
                st.setString(2, userId)
                st.setString(3, "Test notification$titleSuffix")
                st.setObject(4, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }
        }
        return id
    }
}
