package com.example.mediq.server.http

import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.CLINIC_ZONE
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import com.example.mediq.server.ServerConfig
import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.NotificationStore
import com.example.mediq.server.db.UserRow
import com.example.mediq.server.db.UserStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.Base64

private val log = LoggerFactory.getLogger("Routes")

private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100

/** Unauthenticated liveness check, for a health probe or to confirm it started. */
fun Route.healthRoute() {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }
}

/**
 * Everything the app can call, in one file so the whole API can be read at once.
 *
 * The route shapes match the interfaces in domain/repository one to one. When an
 * interface changes, the matching route changes with it.
 */
fun Route.mediQRoutes(
    db: Database,
    config: ServerConfig,
    users: UserStore,
    doctors: DoctorStore,
    appointments: AppointmentStore,
    notifications: NotificationStore,
    auth: AuthService,
    tokens: Tokens,
) {
    // --- Auth ---------------------------------------------------------------

    route("/auth") {
        // Sign-out needs a token; the rest of /auth is how a caller gets one.
        post("/sign-in") {
            val body = call.receive<SignInRequestDto>()
            val (token, user) = auth.signIn(body.username, body.password)
            call.respond(
                AuthSessionDto(
                    accessToken = token.value,
                    expiresAt = token.expiresAt.toString(),
                    profile = user.toProfile().toDto(),
                )
            )
        }

        authenticate(AUTH_JWT) {
            post("/sign-out") {
                // Ends the server-side session, so the token stops working even
                // though it has not expired yet.
                val principal = call.requirePrincipal()
                auth.signOut(principal.sessionId)
                call.respond(mapOf("ok" to true))
            }
        }

        post("/otp/request") {
            val body = call.receive<RequestOtpRequestDto>()
            val (code, returned) = auth.requestOtp(body.mobileNumber)
            call.respond(OtpRequestedDto(sent = true, code = code.takeIf { returned }))
        }

        post("/otp/verify") {
            val body = call.receive<VerifyOtpRequestDto>()
            call.respond(mapOf("registrationId" to auth.verifyOtp(body.mobileNumber, body.otp)))
        }

        post("/register") {
            val body = call.receive<RegisterRequestDto>()
            val request = body.toDomain()
            val (token, user) = auth.register(
                registrationId = request.registrationId,
                fullName = request.fullName,
                username = request.username,
                password = request.password,
                dateOfBirth = request.dateOfBirth,
                sex = request.sex,
                email = request.email,
            )
            call.respond(
                AuthSessionDto(
                    accessToken = token.value,
                    expiresAt = token.expiresAt.toString(),
                    profile = user.toProfile().toDto(),
                )
            )
        }
    }

    // --- Doctors ------------------------------------------------------------

    route("/doctors") {
        /** Public. Browsing doctors needs no account. */
        get {
            val (items, hasMore) = doctors.list(
                query = buildDoctorQuery(
                    searchText = call.request.queryParameters["search"],
                    specialtyWire = call.request.queryParameters["specialty"],
                    building = call.request.queryParameters["building"],
                ),
                limit = call.pageSize(),
                offset = call.pageOffset(),
            )
            call.respond(PagedDto(items.map { it.toDto() }, call.nextCursor(hasMore)))
        }

        get("/{doctorId}") {
            call.respond(doctors.get(call.pathParam("doctorId")).toDto())
        }

        get("/{doctorId}/availability") {
            val month = call.request.queryParameters["month"]?.let { parseMonth(it) }
                ?: LocalDate.now(CLINIC_ZONE).withDayOfMonth(1)
            val dates = doctors.availableDates(call.pathParam("doctorId"), month)
            call.respond(PagedDto(dates.map { it.toDto() }))
        }

        get("/{doctorId}/slots") {
            val date = call.request.queryParameters["date"]
                ?: throw ApiError.badRequest("A date is required, as YYYY-MM-DD.")
            val parsed = parseDate(date, "date")

            val slots = doctors.slots(call.pathParam("doctorId"), parsed)
            call.respond(PagedDto(slots.map { it.toDto() }))
        }
    }

    // --- Appointments -------------------------------------------------------
    //
    // Wrapped in `authenticate` rather than relying on `requirePrincipal` alone.
    // The helper throws a 401 for an anonymous caller, but without the wrapper
    // Ktor never runs the JWT validation at all — every one of these routes
    // would answer 401 to a perfectly valid token as well.

    authenticate(AUTH_JWT) {
        route("/appointments") {
            get {
                val principal = call.requirePrincipal()
                val filter = call.request.queryParameters["filter"]?.toAppointmentFilter()
                    ?: AppointmentFilter.UPCOMING
                val (rows, hasMore) = appointments.list(
                    userId = principal.userId,
                    role = principal.role.toUserRole(),
                    filter = filter,
                    limit = call.pageSize(),
                    offset = call.pageOffset(),
                )
                call.respond(
                    PagedDto(
                        rows.map { appointments.toAppointment(it, withReason = false).toDto() },
                        call.nextCursor(hasMore),
                    )
                )
            }

            get("/{appointmentId}") {
                val principal = call.requirePrincipal()
                val row = appointments.get(
                    principal.userId,
                    principal.role.toUserRole(),
                    call.pathParam("appointmentId"),
                )
                // The only endpoint that returns reason_for_visit.
                call.respond(appointments.toAppointment(row, withReason = true).toDto(withReason = true))
            }

            post {
                val principal = call.requirePrincipal()
                val body = call.receive<BookingRequestDto>().toDomain()
                val row = appointments.book(principal.userId, body)
                call.respond(appointments.toAppointment(row, withReason = true).toDto(withReason = true))
            }

            delete("/{appointmentId}") {
                val principal = call.requirePrincipal()
                appointments.cancel(
                    principal.userId,
                    principal.role.toUserRole(),
                    call.pathParam("appointmentId"),
                )
                call.respond(mapOf("ok" to true))
            }

            post("/{appointmentId}/reschedule-request") {
                val principal = call.requirePrincipal()
                val body = call.receive<RescheduleRequestDto>()
                val request = body.toDomain(call.pathParam("appointmentId"))
                appointments.requestReschedule(principal.userId, principal.role.toUserRole(), request)
                call.respond(mapOf("ok" to true))
            }
        }
    }

    // --- Notifications ------------------------------------------------------

    authenticate(AUTH_JWT) {
        route("/notifications") {
            get {
                val principal = call.requirePrincipal()
                val (items, hasMore) = notifications.list(principal.userId, call.pageSize(), call.pageOffset())
                call.respond(PagedDto(items.map { it.toDto() }, call.nextCursor(hasMore)))
            }

            patch("/{notificationId}/read") {
                val principal = call.requirePrincipal()
                notifications.markRead(principal.userId, call.pathParam("notificationId"))
                call.respond(mapOf("ok" to true))
            }
        }

        // --- Profile --------------------------------------------------------
        //
        // No user id in the path or query. Both operations act on the caller
        // from the token, so there is no request a client could alter to read
        // or edit someone else's record.

        route("/profile") {
            get {
                val principal = call.requirePrincipal()
                val profile = users.findById(principal.userId)?.toProfile()
                    ?: throw ApiError.notFound("That account was not found.")
                call.respond(profile.toDto())
            }

            put {
                val principal = call.requirePrincipal()
                val body = call.receive<UpdateProfileDto>()
                val updated = users.updateProfile(
                    id = principal.userId,
                    fullName = body.fullName,
                    email = body.email,
                    mobileNumber = body.mobileNumber,
                    dateOfBirth = body.dateOfBirth?.let { parseDate(it, "dateOfBirth") },
                    sex = body.sex?.toSex(),
                    address = body.address,
                )
                call.respond(updated.toProfile().toDto())
            }
        }
    }
}

/** Who is making the request, taken from a token this server issued. */
data class Caller(
    val userId: String,
    val role: String,
    val sessionId: String,
)

private fun ApplicationCall.requirePrincipal(): Caller {
    val principal = principal<JWTPrincipal>() ?: throw ApiError.unauthorized()
    fun claim(name: String): String =
        principal.payload.getClaim(name).asString()?.takeIf { it.isNotBlank() }
            ?: throw ApiError.unauthorized("Malformed token.")

    return Caller(
        userId = principal.payload.subject?.takeIf { it.isNotBlank() } ?: throw ApiError.unauthorized(),
        role = claim("role"),
        sessionId = claim("sid"),
    )
}

/** The auth provider name routes reference when they wrap themselves in [authenticate]. */
private const val AUTH_JWT = "auth-jwt"

/**
 * Verifies the token *and* checks the session is still live.
 *
 * The signature check alone is not enough. A signed token stays valid until it
 * expires no matter what the user does, so signing out has to invalidate the
 * session row as well — otherwise "sign out" only hides the app's UI while the
 * token keeps working.
 *
 * [Tokens.verifier] has already checked the signature, issuer, and expiry by the
 * time `validate` runs, so this only reads the claims and asks the database
 * whether the session is still live.
 */
fun AuthenticationConfig.configureAuth(tokens: Tokens, users: UserStore) = jwt(AUTH_JWT) {
    realm = "mediq"
    verifier(tokens.verifier)

    validate { credential ->
        val sid = credential.payload.getClaim("sid").asString()
        val subject = credential.payload.subject

        // Reaches the database on every authenticated request. That is the price
        // of a sign-out that takes effect immediately; a cache would have to be
        // invalidated on revoke too.
        if (!sid.isNullOrBlank() && !subject.isNullOrBlank() && users.activeSession(sid) != null) {
            JWTPrincipal(credential.payload)
        } else {
            null
        }
    }

    challenge { _, _ ->
        call.respond(HttpStatusCode.Unauthorized, ErrorDto("unauthorized", "Please sign in again."))
    }
}

// --- Request helpers --------------------------------------------------------

/**
 * Reads the `month` query parameter as either `YYYY-MM` or a full `YYYY-MM-DD`.
 *
 * The parameter is called a month, so `2026-11` is what a client will send, but
 * the domain signature is `LocalDate`. Only the `YearMonth` is read from it
 * either way, so a day-of-month in the value is ignored — accepting both beats
 * picking one and rejecting the other in a 400.
 *
 * An unparseable value is an error rather than a fallback to the current month.
 * Falling back would answer a different question than the one asked and look
 * like it worked.
 */
private fun parseMonth(raw: String): LocalDate =
    runCatching { YearMonth.parse(raw).atDay(1) }.getOrElse {
        runCatching { LocalDate.parse(raw) }.getOrElse {
            throw ApiError.badRequest("month must be a month as YYYY-MM, or a date as YYYY-MM-DD.")
        }
    }

private fun ApplicationCall.pathParam(name: String): String =
    parameters[name] ?: throw ApiError.badRequest("Missing '$name'.")

private fun ApplicationCall.pageSize(): Int =
    request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, MAX_PAGE_SIZE) ?: DEFAULT_PAGE_SIZE

private fun ApplicationCall.pageOffset(): Int = decodeCursor(request.queryParameters["cursor"]) ?: 0

private fun ApplicationCall.nextCursor(hasMore: Boolean): String? {
    if (!hasMore) return null
    return Base64.getEncoder().encodeToString("off:${pageOffset() + pageSize()}".toByteArray())
}

private fun decodeCursor(cursor: String?): Int? {
    if (cursor.isNullOrBlank()) return null
    return runCatching {
        String(Base64.getDecoder().decode(cursor)).removePrefix("off:").toIntOrNull()
    }.getOrNull()
}

fun StatusPagesConfig.configureStatusPages() {
    exception<ApiError> { call, cause ->
        call.respond(cause.status, ErrorDto(cause.code, cause.message))
    }

    // A body that does not match the expected shape is the client's mistake.
    // The message is generic so it cannot be used to probe the API.
    //
    // Ktor wraps the parse failure in BadRequestException rather than letting
    // the SerializationException through, so both are handled here. Without the
    // second one a malformed body reaches the catch-all below and answers 500,
    // which tells the client the server broke when it sent the wrong shape.
    exception<SerializationException> { call, _ ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorDto("bad_request", "That request could not be read."),
        )
    }
    exception<BadRequestException> { call, _ ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorDto("bad_request", "That request could not be read."),
        )
    }

    // Anything unrecognised is a bug here. Log it in full, return nothing
    // specific, so table names and stack traces do not reach the app.
    exception<Throwable> { call, cause ->
        log.error("Unhandled error on {} {}", call.request.local.uri, cause.stackTraceToString())
        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorDto("internal_error", "Something went wrong. Please try again."),
        )
    }
}