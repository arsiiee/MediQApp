package com.example.mediq.server.db

import com.example.mediq.server.auth.Passwords
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.UserRole
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Row access for users and sessions.
 *
 * Timestamps go in and out as [Instant]; H2 hands back `OffsetDateTime` from
 * `TIMESTAMP WITH TIME ZONE`, so they are normalised at the boundary rather
 * than in every call site.
 */

data class UserRow(
    val id: String,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val sex: Sex?,
    val address: String?,
    val role: UserRole,
) {
    fun toProfile(): UserProfile = UserProfile(
        id = id,
        username = username,
        fullName = fullName,
        // The domain models email as non-null because the profile screen shows
        // it; registration allows it to be left blank, so an absent email
        // surfaces as an empty string rather than a crash.
        email = email ?: "",
        mobileNumber = mobileNumber,
        dateOfBirth = dateOfBirth,
        sex = sex,
        address = address,
        role = role,
    )
}

class UserStore(private val db: com.example.mediq.server.Database) {

    fun create(
        username: String,
        password: String,
        fullName: String,
        email: String?,
        mobileNumber: String,
        dateOfBirth: LocalDate,
        sex: Sex?,
        role: UserRole = UserRole.PATIENT,
        address: String? = null,
    ): UserRow = db.tx { c ->
        val id = UUID.randomUUID().toString()
        c.prepareStatement(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, mobile_number,
                               date_of_birth, sex, address, role, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setString(1, id)
            st.setString(2, username)
            st.setString(3, Passwords.hash(password))
            st.setString(4, fullName)
            st.setString(5, email)
            st.setString(6, mobileNumber)
            st.setObject(7, dateOfBirth)
            st.setString(8, sex?.wireValue)
            st.setString(9, address)
            st.setString(10, role.wireValue)
            st.setObject(11, OffsetDateTime.now(ZoneOffset.UTC))
            st.executeUpdate()
        }
        findById(c, id)!!
    }

    fun findByUsername(username: String): UserRow? =
        db.read { c -> findByUsername(c, username) }

    fun findById(id: String): UserRow? = db.read { c -> findById(c, id) }

    /**
     * Applies a partial profile edit: a null argument leaves that column alone.
     *
     * Every column is COALESCE'd, including the ones that look optional.
     * `Json { explicitNulls = false }` in `Main` means a client cannot send an
     * explicit null, so "leave it alone" and "clear it" are indistinguishable on
     * the wire — and a plain assignment would wipe a real patient's email or
     * address every time they changed their name. These columns are also
     * COALESCE-able only because they are nullable; `date_of_birth` is not, which
     * is why it is passed through [setObject] and never nulled.
     */
    fun updateProfile(
        id: String,
        fullName: String?,
        email: String?,
        mobileNumber: String?,
        dateOfBirth: LocalDate?,
        sex: Sex?,
        address: String?,
    ): UserRow = db.tx { c ->
        c.prepareStatement(
            """
            UPDATE users SET
                full_name     = COALESCE(?, full_name),
                email         = COALESCE(?, email),
                mobile_number = COALESCE(?, mobile_number),
                date_of_birth = COALESCE(?, date_of_birth),
                sex           = COALESCE(?, sex),
                address       = COALESCE(?, address)
            WHERE id = ?
            """.trimIndent()
        ).use { st ->
            st.setString(1, fullName)
            st.setString(2, email)
            st.setString(3, mobileNumber)
            if (dateOfBirth != null) st.setObject(4, dateOfBirth) else st.setNull(4, java.sql.Types.DATE)
            st.setString(5, sex?.wireValue)
            st.setString(6, address)
            st.setString(7, id)
            st.executeUpdate()
        }
        findById(c, id) ?: throw IllegalStateException("user $id vanished during update")
    }

    fun usernameExists(username: String): Boolean =
        db.read { c -> findByUsername(c, username) != null }

    fun mobileExists(mobileNumber: String): Boolean = db.read { c ->
        c.prepareStatement("SELECT 1 FROM users WHERE mobile_number = ?").use { st ->
            st.setString(1, mobileNumber)
            st.executeQuery().use { rs -> rs.next() }
        }
    }

    fun emailExists(email: String): Boolean = db.read { c ->
        c.prepareStatement("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)").use { st ->
            st.setString(1, email)
            st.executeQuery().use { rs -> rs.next() }
        }
    }

    // --- Sessions ----------------------------------------------------------

    /** @return the new session id. */
    fun createSession(userId: String, expiresAt: Instant): String = db.tx { c ->
        val id = UUID.randomUUID().toString()
        c.prepareStatement(
            "INSERT INTO sessions (id, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)"
        ).use { st ->
            st.setString(1, id)
            st.setString(2, userId)
            st.setObject(3, OffsetDateTime.now(ZoneOffset.UTC))
            st.setObject(4, expiresAt.atOffset(ZoneOffset.UTC))
            st.executeUpdate()
        }
        id
    }

    /**
     * Null when the session is unknown, revoked, or past its expiry. Called on
     * every authenticated request, which is what makes sign-out real.
     */
    fun activeSession(sessionId: String): String? = db.read { c ->
        c.prepareStatement(
            """
            SELECT user_id FROM sessions
            WHERE id = ? AND revoked_at IS NULL AND expires_at > ?
            """.trimIndent()
        ).use { st ->
            st.setString(1, sessionId)
            st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
            st.executeQuery().use { rs -> if (rs.next()) rs.getString("user_id") else null }
        }
    }

    fun revokeSession(sessionId: String) = db.tx { c ->
        c.prepareStatement("UPDATE sessions SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL")
            .use { st ->
                st.setObject(1, OffsetDateTime.now(ZoneOffset.UTC))
                st.setString(2, sessionId)
                st.executeUpdate()
            }
    }

    private fun findById(c: Connection, id: String): UserRow? =
        c.prepareStatement("$SELECT_COLUMNS WHERE id = ?").use { st ->
            st.setString(1, id)
            st.executeQuery().use { rs -> if (rs.next()) rs.readUser() else null }
        }

    private fun findByUsername(c: Connection, username: String): UserRow? =
        c.prepareStatement("$SELECT_COLUMNS WHERE username = ?").use { st ->
            st.setString(1, username)
            st.executeQuery().use { rs -> if (rs.next()) rs.readUser() else null }
        }

    private companion object {
        const val SELECT_COLUMNS = """
            SELECT id, username, password_hash, full_name, email, mobile_number,
                   date_of_birth, sex, address, role
            FROM users
        """
    }
}

private fun java.sql.ResultSet.readUser() = UserRow(
    id = getString("id"),
    username = getString("username"),
    passwordHash = getString("password_hash"),
    fullName = getString("full_name"),
    email = getString("email"),
    mobileNumber = getString("mobile_number"),
    dateOfBirth = getObject("date_of_birth", LocalDate::class.java),
    sex = getString("sex")?.let { wire -> Sex.entries.firstOrNull { it.wireValue == wire } },
    address = getString("address"),
    role = UserRole.entries.first { it.wireValue == getString("role") },
)