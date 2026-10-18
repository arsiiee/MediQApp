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
    /**
     * Whether the account may be used.
     *
     * Read as [UserStatus.INACTIVE] when the column holds something this build
     * does not recognise, rather than as active: the value gates sign-in and
     * every authenticated request, so the only safe reading of "cannot tell" is
     * the one that refuses. A hand-edited column reads as disabled and is
     * visible as such on the admin screen.
     */
    val status: UserStatus,
    /**
     * The `doctors` row this staff account sits behind, when it has one.
     *
     * Carried on the row because the admin area links a secretary to the desk
     * they work, and the link is a column (`users.doctor_id`) that only the demo
     * seed had ever written. Patients and unlinked staff have none.
     */
    val doctorId: String?,
    /** When the account was created. The admin area shows it as "joined on". */
    val createdAt: Instant,
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

/**
 * A session that is still live, and whether its account may still be used.
 *
 * The two travel together because the second is what decides the first: a
 * revoked session and a disabled account are the same answer to the caller, and
 * separating them would mean a second query on every authenticated request to
 * find out whether the first answer was worth having.
 */
data class LiveSession(
    val userId: String,
    val status: UserStatus,
)

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
        /**
         * Active on creation, always, for every caller that matters — the
         * registration flow and the admin's create-staff form both produce an
         * account someone can sign in with. The parameter exists so a test can
         * build a disabled account in one write rather than create-then-disable,
         * which is the shape a fixture wants and would otherwise be two calls
         * away from the thing under test.
         */
        status: UserStatus = UserStatus.ACTIVE,
        /**
         * The `doctors` row this account sits behind, when there is one.
         *
         * Default null because registration, the demo seed, and every patient
         * account produce none; [StaffAccountService] is the only caller that
         * passes one, for a doctor it has just created and for a secretary
         * assigned to an existing doctor.
         */
        doctorId: String? = null,
    ): UserRow = db.tx { c ->
        createIn(c, username, password, fullName, email, mobileNumber, dateOfBirth, sex, role, address, status, doctorId)
    }

    /**
     * [create] without opening its own transaction, for a caller that has to
     * write more than one table.
     *
     * A doctor account is two rows — this one and the `doctors` row `doctor_id`
     * points at — and they have to appear together. `Database.tx` takes a fresh
     * connection every call, so calling [create] and a doctor-store method from
     * the same service would be two transactions that can half-apply, leaving an
     * account that can sign in with no practice record behind it.
     */
    internal fun createIn(
        c: Connection,
        username: String,
        password: String,
        fullName: String,
        email: String?,
        mobileNumber: String,
        dateOfBirth: LocalDate,
        sex: Sex?,
        role: UserRole,
        address: String?,
        status: UserStatus,
        doctorId: String?,
    ): UserRow {
        val id = UUID.randomUUID().toString()
        c.prepareStatement(
            """
            INSERT INTO users (id, username, password_hash, full_name, email, mobile_number,
                               date_of_birth, sex, address, role, status, doctor_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            st.setString(11, status.wireValue)
            st.setString(12, doctorId)
            st.setObject(13, OffsetDateTime.now(ZoneOffset.UTC))
            st.executeUpdate()
        }
        return findById(c, id)!!
    }

    fun findByUsername(username: String): UserRow? =
        db.read { c -> findByUsername(c, username) }

    fun findById(id: String): UserRow? = db.read { c -> findById(c, id) }

    /**
     * Every account holding [role], ordered by name.
     *
     * The admin area is the only caller: its three directories are this one
     * read, so what makes them real rather than a fixture is this method
     * existing. Ordered here rather than at the call sites so two screens cannot
     * disagree about the order of the same list.
     *
     * No paging. The admin screens load a directory whole, which is what they
     * did against a fixture; at real volume this is the method that grows a
     * limit (see the pagination gap in SPEC-web.md).
     */
    fun listByRole(role: UserRole): List<UserRow> = db.read { c ->
        c.prepareStatement("$SELECT_COLUMNS WHERE role = ? ORDER BY full_name").use { st ->
            st.setString(1, role.wireValue)
            st.executeQuery().use { rs ->
                generateSequence { if (rs.next()) rs.readUser() else null }.toList()
            }
        }
    }

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
        fullName: String? = null,
        email: String? = null,
        mobileNumber: String? = null,
        dateOfBirth: LocalDate? = null,
        sex: Sex? = null,
        address: String? = null,
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

    /**
     * Enables or disables an account.
     *
     * Never a delete: an account carries its appointments, notifications, and
     * its row in `sessions`, so removing it would take that history with it. The
     * column is what the admin area's Enable/Disable buttons write, and
     * [AuthService.signIn] plus `configureAuth` are what make the value mean
     * anything.
     *
     * Returns the updated row, so the caller answers with what the column now
     * holds rather than with what it asked for.
     */
    fun setStatus(id: String, status: UserStatus): UserRow = db.tx { c ->
        c.prepareStatement("UPDATE users SET status = ? WHERE id = ?").use { st ->
            st.setString(1, status.wireValue)
            st.setString(2, id)
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

    /**
     * The cohort of patients who have booked or attended an appointment with this doctor.
     *
     * Used by the Doctor area's patient list and relationship checks: a doctor
     * may only see patients with whom they have an existing appointment record.
     */
    fun listPatientsForDoctor(doctorId: String): List<UserRow> = db.read { c ->
        c.prepareStatement(
            """
            $SELECT_COLUMNS
            WHERE id IN (
                SELECT DISTINCT user_id FROM appointments WHERE doctor_id = ?
            )
            ORDER BY full_name ASC
            """.trimIndent()
        ).use { st ->
            st.setString(1, doctorId)
            st.executeQuery().use { rs ->
                generateSequence { if (rs.next()) rs.readUser() else null }.toList()
            }
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
     * The account a live session belongs to, or null when the session is
     * unknown, revoked, or past its expiry. Called on every authenticated
     * request, which is what makes sign-out real.
     */
    fun liveSession(sessionId: String): LiveSession? = db.read { c ->
        // The status is joined here rather than fetched in a second query. This
        // runs on every authenticated request, and it is also what makes
        // disabling an account take effect on the account's next call instead of
        // at token expiry — the same reason the session row is checked at all
        // rather than trusting the signature.
        c.prepareStatement(
            """
            SELECT s.user_id, u.status FROM sessions s
            JOIN users u ON u.id = s.user_id
            WHERE s.id = ? AND s.revoked_at IS NULL AND s.expires_at > ?
            """.trimIndent()
        ).use { st ->
            st.setString(1, sessionId)
            st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC))
            st.executeQuery().use { rs ->
                if (!rs.next()) return@use null
                LiveSession(
                    userId = rs.getString("user_id"),
                    // A status this build cannot read is refused, not accepted:
                    // this is the check that decides whether a token still works.
                    status = UserStatus.fromWire(rs.getString("status")) ?: UserStatus.INACTIVE,
                )
            }
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
                   date_of_birth, sex, address, role, status, doctor_id, created_at
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
    status = UserStatus.fromWire(getString("status")) ?: UserStatus.INACTIVE,
    doctorId = getString("doctor_id"),
    // Normalised at the boundary like every other timestamp: H2 hands back an
    // OffsetDateTime from TIMESTAMP WITH TIME ZONE.
    createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
)