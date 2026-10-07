package com.example.mediq.server

import com.example.mediq.domain.model.Sex
import com.example.mediq.server.db.UserStore
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A partial profile edit must not clear the fields it did not mention.
 *
 * The route is a `PUT` with every field optional, and the JSON config sets
 * `explicitNulls = false`, so a client cannot express "set this to null" — an
 * omitted field and an explicit null look identical on the wire. That makes
 * plain `column = ?` assignments wrong: changing a name would silently erase the
 * patient's email, address, and sex. This is the test for the COALESCE change,
 * and it is a data-loss test, so it should not be deleted as redundant.
 */
class ProfileUpdateTest {

    private val config = ServerConfig(
        port = 0,
        jdbcUrl = "jdbc:h2:mem:p_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        jwtSecret = "test-secret-not-used-for-anything-real-123456",
        jwtIssuer = "mediq-test",
        tokenTtlMinutes = 60,
        slotDurationMinutes = 30,
        otpTtlMinutes = 5,
        seedDemoData = false,
    )

    private val db = Database(config)
    private val users = UserStore(db)

    @Test
    fun `changing only the name leaves email address and sex intact`() {
        val created = users.create(
            username = "partial",
            password = "placeholder-never-used",
            fullName = "Original Name",
            email = "patient@example.com",
            mobileNumber = "+639171111111",
            dateOfBirth = LocalDate.of(1990, 4, 4),
            sex = Sex.FEMALE,
            // Seeded through `create`, not `.copy()` on the returned row — a
            // data-class copy only changes the in-memory object, so the column
            // would still be NULL and this test would assert against nothing.
            address = "12 Example Street",
        )

        // Exactly what the app sends when the user edits their name and nothing
        // else: one field, the rest absent.
        val updated = users.updateProfile(
            id = created.id,
            fullName = "New Name",
            email = null,
            mobileNumber = null,
            dateOfBirth = null,
            sex = null,
            address = null,
        )

        assertEquals("New Name", updated.fullName, "the field that was sent must change")
        assertEquals("patient@example.com", updated.email, "an omitted email must not be cleared")
        assertEquals("12 Example Street", updated.address, "an omitted address must not be cleared")
        assertEquals(Sex.FEMALE, updated.sex, "an omitted sex must not be cleared")
        assertEquals("+639171111111", updated.mobileNumber)
        assertEquals(LocalDate.of(1990, 4, 4), updated.dateOfBirth)
    }

    @Test
    fun `each optional field can be set on its own`() {
        val created = users.create(
            username = "single",
            password = "placeholder-never-used",
            fullName = "Single Field",
            email = null,
            mobileNumber = "+639172222222",
            dateOfBirth = LocalDate.of(1988, 8, 8),
            sex = null,
        )

        val withAddress = users.updateProfile(created.id, null, null, null, null, null, "9 New Road")
        assertEquals("9 New Road", withAddress.address)
        assertEquals("Single Field", withAddress.fullName, "the name must survive an address-only edit")

        val withSex = users.updateProfile(created.id, null, null, null, null, Sex.OTHER, null)
        assertEquals(Sex.OTHER, withSex.sex)
        assertEquals("9 New Road", withSex.address, "the address must survive a sex-only edit")
    }
}
