package com.example.mediq.server.db

import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import java.time.LocalDate

/**
 * A staff account as the admin's create form describes it.
 *
 * [practice] and [assignedDoctorId] are role-dependent and both nullable, which
 * means this type can describe a doctor with no practice details. That is
 * deliberate. The alternative is one subtype per role, and the rules that decide
 * which combinations are legal are sentences an administrator reads when they
 * get one wrong — not something a type checker can say. [StaffAccountService.create]
 * is where all of them are checked, before anything is written.
 */
data class NewStaffAccount(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val role: UserRole,
    val status: UserStatus,
    /** A secretary's doctor: the `doctors` row they work for. */
    val assignedDoctorId: String? = null,
    /** A doctor's practice record, which becomes their `doctors` row. */
    val practice: NewDoctorPractice? = null,
)

/**
 * The `doctors` row behind a doctor account.
 *
 * A distinct type rather than nine more fields on [NewStaffAccount], because
 * these are written together or not at all: there is no such thing as a doctor
 * with a fee and no specialty.
 */
data class NewDoctorPractice(
    val specialty: Specialty,
    val licenseNumber: String,
    val yearsExperience: Int,
    val feeCentavos: Long,
    val bio: String,
    val building: String,
    val floor: String,
    val room: String,
    val languages: List<ConsultationLanguage>,
)

/**
 * Creates staff accounts, and for a doctor the practice record the account sits
 * behind.
 *
 * A doctor account is two rows in two tables and both have to be written in one
 * transaction, which is why this is not just another `UserStore` method. It is
 * kept out of [AuthService] so that class stays about signing in and the
 * registration flow, and out of `Routes.kt` so password hashing never happens in
 * a request handler.
 *
 * **The password is chosen by whoever creates the account.** Nothing forces the
 * holder to change it and there is no invitation email, so an administrator has
 * to pass it on out of band. That is how every staff account works today; it is
 * recorded in `README.md`'s known gaps rather than papered over here.
 */
class StaffAccountService(
    private val db: Database,
    private val users: UserStore,
    private val doctors: DoctorStore,
) {

    /**
     * Validates and writes one account with its practice record, or refuses.
     *
     * Every rejection is an [ApiError.badRequest] naming the field in words the
     * caller can act on. The caller is an administrator filling in a form, and
     * an unhandled constraint violation answers 500, which tells them nothing
     * about which box to fix.
     */
    fun create(account: NewStaffAccount): UserRow {
        if (account.role == UserRole.PATIENT) {
            // Registration exists to prove the person controls the number the
            // account is bound to, with an OTP. This route has no way to do
            // that, so it must not be able to mint the accounts that flow
            // produces — including the ones nobody verified.
            throw ApiError.badRequest(
                "A patient account is created by the patient, because registration is what proves the number is theirs."
            )
        }
        if (account.role != UserRole.DOCTOR && account.practice != null) {
            throw ApiError.badRequest("Practice details are for a doctor account.")
        }
        if (account.role != UserRole.SECRETARY && !account.assignedDoctorId.isNullOrBlank()) {
            throw ApiError.badRequest("Only a secretary is assigned to a doctor.")
        }

        val fullName = account.fullName.trim()
        if (fullName.isEmpty()) throw ApiError.badRequest("Enter the account holder's full name.")
        if (account.password.length < MIN_PASSWORD_LENGTH) {
            throw ApiError.badRequest("Use a password of at least $MIN_PASSWORD_LENGTH characters.")
        }

        val username = normalizeUsername(account.username)
        val mobileNumber = normalizeMobile(account.mobileNumber)
        val email = account.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        // Read before the write rather than inside it, so a duplicate is a
        // sentence the administrator can act on rather than a 500. The UNIQUE
        // constraints are still the real guard — a second request racing this
        // one is refused by the database, which is the same trade `register`
        // already makes.
        if (users.usernameExists(username)) throw ApiError.badRequest("That username is already taken.")
        if (email != null && users.emailExists(email)) {
            throw ApiError.badRequest("That email is already registered.")
        }
        if (users.mobileExists(mobileNumber)) {
            throw ApiError.badRequest("An account already exists for that number.")
        }

        return db.tx { c ->
            val doctorId = when (account.role) {
                UserRole.DOCTOR -> {
                    val practice = account.practice
                        ?: throw ApiError.badRequest("A doctor account needs the practice details.")
                    validate(practice)
                    // Written first because the account row links to it.
                    doctors.insert(
                        c = c,
                        fullName = fullName,
                        specialty = practice.specialty,
                        yearsExperience = practice.yearsExperience,
                        feeCentavos = practice.feeCentavos,
                        licenseNumber = practice.licenseNumber,
                        bio = practice.bio,
                        building = practice.building,
                        floor = practice.floor,
                        room = practice.room,
                        languages = practice.languages,
                    )
                }

                UserRole.SECRETARY -> {
                    val assigned = account.assignedDoctorId?.takeIf { it.isNotBlank() }
                        ?: throw ApiError.badRequest("Choose the doctor this secretary works for.")
                    // `users.doctor_id` deliberately has no foreign key, so this
                    // is the only thing that refuses a link to a doctor that is
                    // not there. Checked in this transaction, not before it.
                    if (!doctors.existsIn(c, assigned)) {
                        throw ApiError.badRequest("That doctor was not found.")
                    }
                    assigned
                }

                else -> null
            }

            users.createIn(
                c = c,
                username = username,
                password = account.password,
                fullName = fullName,
                email = email,
                mobileNumber = mobileNumber,
                dateOfBirth = account.dateOfBirth,
                // Not collected by the admin form. The column is nullable, and
                // filling it with a guess would put invented data on a profile
                // patients read.
                sex = null,
                role = account.role,
                address = null,
                status = account.status,
                doctorId = doctorId,
            )
        }
    }

    /**
     * The doctor-only rules the `doctors` table cannot state as a sentence.
     *
     * The two ranges are also `CHECK` constraints, but those answer a generic
     * 500; these catch the same typo in a number box and say which one it was.
     */
    private fun validate(practice: NewDoctorPractice) {
        if (practice.licenseNumber.isBlank()) throw ApiError.badRequest("A licence number is required.")
        if (practice.bio.isBlank()) throw ApiError.badRequest("A short biography is required.")
        if (practice.building.isBlank() || practice.floor.isBlank() || practice.room.isBlank()) {
            throw ApiError.badRequest("A clinic room — building, floor, and room — is required.")
        }
        if (practice.languages.isEmpty()) {
            throw ApiError.badRequest("Choose at least one language the doctor consults in.")
        }
        if (practice.yearsExperience < 0) {
            throw ApiError.badRequest("Years of experience cannot be negative.")
        }
        if (practice.feeCentavos < 0) {
            throw ApiError.badRequest("A consultation fee cannot be negative.")
        }
    }
}
