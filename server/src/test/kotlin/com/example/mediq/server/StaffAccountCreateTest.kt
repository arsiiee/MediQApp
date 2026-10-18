package com.example.mediq.server

import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.NewDoctorPractice
import com.example.mediq.server.db.NewStaffAccount
import com.example.mediq.server.db.StaffAccountService
import com.example.mediq.server.db.UserStatus
import com.example.mediq.server.db.UserStore
import com.example.mediq.server.http.CreateAccountRequestDto
import com.example.mediq.server.http.DoctorPracticeDto
import com.example.mediq.server.http.toDomain
import io.ktor.http.HttpStatusCode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Creating a staff account, and for a doctor the practice record behind it.
 *
 * This is the module's only write that adds rows rather than changing one, and
 * it is the first place the server writes a `doctors` row at all — the demo seed
 * did it with raw SQL and nothing else ever has. So three things are pinned
 * here that no other test can: the two rows appear together, the role decides
 * which fields are required, and a refusal writes nothing.
 *
 * Every test gets its own database. They create accounts and assert on whole
 * directories, so a shared one would make each assertion depend on which test
 * ran first.
 */
class StaffAccountCreateTest {

    @Test
    fun `a doctor account and its practice record are written together`() {
        val f = Fixture()

        val doctor = f.accounts.create(
            f.doctorAccount(name = "Dr Ada Reyes", practice = f.newPractice(Specialty.CARDIOLOGY)),
        )

        assertEquals(UserRole.DOCTOR, doctor.role)
        assertEquals(UserStatus.ACTIVE, doctor.status)
        val doctorId = assertNotNull(doctor.doctorId, "a doctor account must link to its doctors row")

        val practice = f.doctors.get(doctorId)
        assertEquals("Dr Ada Reyes", practice.fullName)
        assertEquals(Specialty.CARDIOLOGY, practice.specialty)
        assertEquals(12, practice.yearsOfExperience)
        assertEquals(85_000L, practice.consultationFee.amountInCentavos)
        assertEquals("PRC-1234567", practice.licenseNumber)
        assertEquals("Main", practice.location.building)
        assertEquals("2F", practice.location.floor)
        assertEquals("204", practice.location.room)
        assertEquals(
            listOf(ConsultationLanguage.ENGLISH, ConsultationLanguage.FILIPINO),
            practice.languages,
        )

        // And it reaches the directory the admin screen reads, not only the
        // table the link points at.
        assertEquals(listOf(doctor.fullName), f.users.listByRole(UserRole.DOCTOR).map { it.fullName })
    }

    @Test
    fun `a doctor created here has no clinic hours, so no slots follow`() {
        val f = Fixture()
        val doctor = f.accounts.create(f.doctorAccount())
        val doctorId = assertNotNull(doctor.doctorId)

        // The create form collects no schedule, and inventing a week of sessions
        // would make the clinic's bookable calendar a guess. Until the
        // doctor-workspace module adds the schedule editor this is the state a
        // new doctor is in, so it is pinned as a known consequence rather than
        // left to be discovered by a patient finding an unbookable doctor.
        assertTrue(f.doctors.get(doctorId).clinicHours.isEmpty(), "a new doctor has no clinic hours")
        assertTrue(
            f.doctors.availableDates(doctorId, YearMonth.now().atDay(1)).isEmpty(),
            "with no clinic hours there is nothing for a patient to book",
        )
    }

    @Test
    fun `a secretary is linked to the doctor they are assigned to and gets no practice record`() {
        val f = Fixture()
        val doctor = f.accounts.create(f.doctorAccount())
        val doctorId = assertNotNull(doctor.doctorId)
        val doctorsBefore = f.doctorRowCount()

        val secretary = f.accounts.create(
            f.secretaryAccount(name = "Sam Secretary", assignedDoctorId = doctorId),
        )

        assertEquals(UserRole.SECRETARY, secretary.role)
        assertEquals(doctorId, secretary.doctorId, "the assignment is the link the secretary area reads")
        assertEquals(
            doctorsBefore,
            f.doctorRowCount(),
            "a secretary is not a clinician and must not add a doctors row",
        )
    }

    @Test
    fun `an admin account is created with no doctor link`() {
        val f = Fixture()
        val admin = f.accounts.create(f.staffAccount(role = UserRole.ADMIN, name = "Ada Admin"))

        assertEquals(UserRole.ADMIN, admin.role)
        assertNull(admin.doctorId, "an administrator is not a clinician and nobody's secretary")
    }

    @Test
    fun `the created account can sign in with the password it was given`() {
        val f = Fixture()
        val created = f.accounts.create(f.staffAccount(role = UserRole.ADMIN, name = "Ada Admin"))

        // Signed in with the case and spacing a person would actually type, so
        // this also proves the create path and the sign-in path normalize the
        // username the same way.
        val (_, signedIn) = f.auth.signIn("  ${created.username.uppercase()}  ", FIXTURE_PASSWORD)
        assertEquals(created.id, signedIn.id)
        assertEquals(UserRole.ADMIN, signedIn.role)
    }

    @Test
    fun `an account created as inactive cannot sign in until it is re-enabled`() {
        val f = Fixture()
        val created = f.accounts.create(
            f.staffAccount(role = UserRole.ADMIN, status = UserStatus.INACTIVE),
        )
        assertEquals(UserStatus.INACTIVE, created.status, "the status the form chose is the status written")

        val refused = assertFailsWith<ApiError> { f.auth.signIn(created.username, FIXTURE_PASSWORD) }
        assertContains(refused.message, "not active")

        f.users.setStatus(created.id, UserStatus.ACTIVE)
        assertEquals(created.id, f.auth.signIn(created.username, FIXTURE_PASSWORD).second.id)
    }

    // --- Refusals -----------------------------------------------------------

    @Test
    fun `a patient account is refused`() {
        val f = Fixture()

        val message = refusal { f.accounts.create(f.staffAccount(role = UserRole.PATIENT)) }

        assertContains(message, "created by the patient")
        // The refusal is before the transaction, so no account can be left behind
        // that no OTP ever proved belonged to anyone.
        assertEquals(0, f.userRowCount(), "a refused create writes nothing")
    }

    @Test
    fun `practice details for a non-doctor are refused`() {
        val f = Fixture()

        val message = refusal {
            f.accounts.create(
                f.secretaryAccount(
                    assignedDoctorId = UUID.randomUUID().toString(),
                    practice = f.newPractice(),
                ),
            )
        }

        assertContains(message, "Practice details are for a doctor account")
        assertEquals(0, f.userRowCount())
    }

    @Test
    fun `a doctor account with no practice details is refused`() {
        val f = Fixture()

        val message = refusal {
            f.accounts.create(f.staffAccount(role = UserRole.DOCTOR, practice = null))
        }

        assertContains(message, "needs the practice details")
        assertEquals(0, f.doctorRowCount(), "a refused doctor create must not leave a doctors row")
    }

    @Test
    fun `an assigned doctor for a non-secretary is refused`() {
        val f = Fixture()

        val message = refusal {
            f.accounts.create(
                f.staffAccount(role = UserRole.ADMIN, assignedDoctorId = UUID.randomUUID().toString()),
            )
        }

        assertContains(message, "Only a secretary is assigned to a doctor")
    }

    @Test
    fun `a secretary with no assigned doctor is refused`() {
        val f = Fixture()

        val message = refusal { f.accounts.create(f.secretaryAccount(assignedDoctorId = null)) }

        assertContains(message, "Choose the doctor")
    }

    @Test
    fun `a secretary assigned to a doctor that is not there is refused`() {
        val f = Fixture()

        // `users.doctor_id` has no foreign key, so nothing but this check refuses
        // a link that points at nothing — the secretary would sign in to an empty
        // area with no way to tell why.
        val message = refusal {
            f.accounts.create(f.secretaryAccount(assignedDoctorId = UUID.randomUUID().toString()))
        }

        assertContains(message, "That doctor was not found")
        assertEquals(0, f.userRowCount())
    }

    @Test
    fun `a missing licence, biography, room, or language is refused`() {
        val f = Fixture()

        assertContains(doctorRefusal(f, f.newPractice(licenseNumber = "   ")), "licence number")
        assertContains(doctorRefusal(f, f.newPractice(bio = "")), "biography")
        assertContains(doctorRefusal(f, f.newPractice(room = " ")), "clinic room")
        assertContains(doctorRefusal(f, f.newPractice(languages = emptyList())), "language")
    }

    @Test
    fun `a negative fee or years of experience is refused`() {
        val f = Fixture()

        // Both columns carry a CHECK, so without these the caller gets a 500 for
        // what is plainly a typo in a number box.
        assertContains(doctorRefusal(f, f.newPractice(feeCentavos = -1)), "fee cannot be negative")
        assertContains(
            doctorRefusal(f, f.newPractice(yearsExperience = -1)),
            "Years of experience cannot be negative",
        )
    }

    @Test
    fun `a short password is refused and the account is not written`() {
        val f = Fixture()
        val account = f.staffAccount(role = UserRole.ADMIN, password = "short")

        val message = refusal { f.accounts.create(account) }

        assertContains(message, "at least 8 characters")
        assertNull(f.users.findByUsername(account.username), "nothing may be written for a refused create")
    }

    @Test
    fun `a duplicate username, email, or mobile number is refused`() {
        val f = Fixture()
        val existing = f.accounts.create(f.staffAccount(role = UserRole.ADMIN, name = "First Admin"))

        assertContains(
            refusal { f.accounts.create(f.staffAccount(role = UserRole.ADMIN, username = existing.username)) },
            "username is already taken",
        )
        assertContains(
            refusal { f.accounts.create(f.staffAccount(role = UserRole.ADMIN, email = existing.email)) },
            "email is already registered",
        )
        assertContains(
            refusal {
                f.accounts.create(f.staffAccount(role = UserRole.ADMIN, mobileNumber = existing.mobileNumber))
            },
            "already exists for that number",
        )
    }

    // --- Storage form -------------------------------------------------------

    @Test
    fun `the username, email, and mobile number are stored in their normalized form`() {
        val f = Fixture()
        val created = f.accounts.create(
            f.staffAccount(role = UserRole.ADMIN, username = "  Ada.Admin  ", email = "  Ada@Clinic.Test "),
        )

        assertEquals("ada.admin", created.username)
        assertEquals("ada@clinic.test", created.email)
        assertTrue(created.mobileNumber.startsWith("+"), "a mobile number is stored in E.164")
        assertEquals(created.id, f.auth.signIn(" ADA.ADMIN ", FIXTURE_PASSWORD).second.id)
    }

    // --- The transaction ----------------------------------------------------

    @Test
    fun `a failure after the doctors row is written rolls that row back too`() {
        val f = Fixture()
        val before = f.doctorRowCount()

        assertFailsWith<IllegalStateException> {
            f.db.tx { c ->
                f.doctors.insert(
                    c = c,
                    fullName = "Rollback Doctor",
                    specialty = Specialty.CARDIOLOGY,
                    yearsExperience = 5,
                    feeCentavos = 70_000,
                    licenseNumber = "PRC-0000000",
                    bio = "bio",
                    building = "Main",
                    floor = "1F",
                    room = "101",
                    languages = listOf(ConsultationLanguage.ENGLISH),
                )
                // The account write is what happens next in
                // `StaffAccountService.create`. Anything it throws — a dropped
                // connection, a constraint — has to take this row with it, or the
                // clinic is left with a clinician nobody can sign in as and no
                // account to find them under.
                throw IllegalStateException("the account write failed")
            }
        }

        assertEquals(before, f.doctorRowCount(), "a rolled-back transaction must leave no doctors row")
    }

    // --- The wire mapping ---------------------------------------------------

    @Test
    fun `the request mapping rejects an unknown role, status, date, specialty, or language`() {
        val base = CreateAccountRequestDto(
            username = "new.staff",
            password = "long-enough-password",
            fullName = "New Staff",
            mobileNumber = "+639175550142",
            dateOfBirth = "1990-01-01",
            role = "secretary",
        )

        assertContains(refusal { base.copy(role = "nurse").toDomain() }, "Unknown role")
        assertContains(refusal { base.copy(status = "suspended").toDomain() }, "status must be")
        assertContains(refusal { base.copy(dateOfBirth = "yesterday").toDomain() }, "dateOfBirth")

        val withPractice = base.copy(
            role = "doctor",
            practice = DoctorPracticeDto(
                specializationId = "cardiology",
                licenseNumber = "PRC-1234567",
                yearsExperience = 5,
                feeCentavos = 70_000,
                bio = "bio",
                building = "Main",
                floor = "2F",
                room = "204",
                languages = listOf("en"),
            ),
        )
        assertContains(
            refusal { withPractice.copy(practice = withPractice.practice!!.copy(specializationId = "magic")) .toDomain() },
            "Unknown specialty",
        )
        assertContains(
            refusal { withPractice.copy(practice = withPractice.practice!!.copy(languages = listOf("klingon"))).toDomain() },
            "Unknown language",
        )
    }

    // --- Helpers ------------------------------------------------------------

    /** Asserts the call is refused as a bad request and returns the sentence. */
    private fun refusal(block: () -> Unit): String {
        val error = assertFailsWith<ApiError>(block = block)
        assertEquals(HttpStatusCode.BadRequest, error.status, "a form mistake is a 400, not a 500")
        return error.message
    }

    /** The same, for a doctor account built around one changed practice field. */
    private fun doctorRefusal(f: Fixture, practice: NewDoctorPractice): String =
        refusal { f.accounts.create(f.doctorAccount(practice = practice)) }

    /** Its own database, so one test's accounts cannot reach another's assertions. */
    private class Fixture {
        private val config = ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:staff_create_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            jwtSecret = "test-secret-not-used-for-anything-real-123456",
            jwtIssuer = "mediq-test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
        )

        val db = Database(config)
        val users = UserStore(db)
        val doctors = DoctorStore(db, config.slotDurationMinutes)
        val accounts = StaffAccountService(db, users, doctors)
        val auth = AuthService(
            db,
            users,
            Tokens(config.jwtSecret, config.jwtIssuer),
            config.tokenTtlMinutes,
            config.otpTtlMinutes,
        )

        fun staffAccount(
            role: UserRole,
            name: String = "New Staff",
            password: String = FIXTURE_PASSWORD,
            username: String = "acct_${UUID.randomUUID().toString().take(8)}",
            email: String? = "acct_${UUID.randomUUID().toString().take(8)}@clinic.test",
            mobileNumber: String = mobile(),
            status: UserStatus = UserStatus.ACTIVE,
            assignedDoctorId: String? = null,
            practice: NewDoctorPractice? = null,
        ) = NewStaffAccount(
            username = username,
            password = password,
            fullName = name,
            email = email,
            mobileNumber = mobileNumber,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            role = role,
            status = status,
            assignedDoctorId = assignedDoctorId,
            practice = practice,
        )

        fun doctorAccount(
            name: String = "Dr New Doctor",
            practice: NewDoctorPractice? = newPractice(),
        ) = staffAccount(role = UserRole.DOCTOR, name = name, practice = practice)

        fun secretaryAccount(
            name: String = "New Secretary",
            assignedDoctorId: String?,
            practice: NewDoctorPractice? = null,
        ) = staffAccount(
            role = UserRole.SECRETARY,
            name = name,
            assignedDoctorId = assignedDoctorId,
            practice = practice,
        )

        fun newPractice(
            specialty: Specialty = Specialty.INTERNAL_MEDICINE,
            licenseNumber: String = "PRC-1234567",
            yearsExperience: Int = 12,
            feeCentavos: Long = 85_000,
            bio: String = "Treats what the clinic can treat.",
            building: String = "Main",
            floor: String = "2F",
            room: String = "204",
            languages: List<ConsultationLanguage> =
                listOf(ConsultationLanguage.ENGLISH, ConsultationLanguage.FILIPINO),
        ) = NewDoctorPractice(
            specialty = specialty,
            licenseNumber = licenseNumber,
            yearsExperience = yearsExperience,
            feeCentavos = feeCentavos,
            bio = bio,
            building = building,
            floor = floor,
            room = room,
            languages = languages,
        )

        fun userRowCount(): Int = db.read { c ->
            c.prepareStatement("SELECT COUNT(*) FROM users").use { st ->
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

        fun doctorRowCount(): Int = db.read { c ->
            c.prepareStatement("SELECT COUNT(*) FROM doctors").use { st ->
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

        /**
         * Unique per call, and deliberately without the leading `+`: the
         * normalizer is what has to add it, so a value that is already E.164
         * would not prove anything.
         */
        private fun mobile(): String {
            val digits = (1_000_000_000L + kotlin.math.abs(UUID.randomUUID().hashCode().toLong()))
                .toString()
                .takeLast(7)
            return "639$digits"
        }
    }
}

/** The password every account in this file is created with. */
private const val FIXTURE_PASSWORD = "hunter2-secret"
