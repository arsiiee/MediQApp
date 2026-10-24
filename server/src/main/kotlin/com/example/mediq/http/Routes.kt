package com.example.mediq.http

import com.example.mediq.domain.model.*
import com.example.mediq.mcp.mcpServer
import com.example.mediq.security.JwtClaims
import com.example.mediq.security.JwtProvider
import com.example.mediq.service.*
import com.example.mediq.util.toLocalDate
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

fun Routing.configureRoutes(
    authService: AuthService,
    doctorService: DoctorService,
    adminService: AdminService,
    appointmentService: AppointmentService,
    messageService: MessageService,
    notificationService: NotificationService,
    profileService: ProfileService,
    jwtProvider: JwtProvider,
) {
    mcpServer()

    // Public routes
    route("auth") {
        post("sign-in") {
            val request = call.receive<SignInRequest>()
            authService.signIn(request.username, request.password).fold(
                onSuccess = { call.respond(it) },
                onError = { call.respond(HttpStatusCode.Unauthorized, it) },
            )
        }

        post("register") {
            val request = call.receive<RegisterRequest>()
            authService.register(request).fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it) },
                onError = { call.respond(HttpStatusCode.BadRequest, it) },
            )
        }

        post("request-otp") {
            val request = call.receive<RequestOtpRequest>()
            authService.requestOtp(request).fold(
                onSuccess = { call.respond(HttpStatusCode.OK, it) },
                onError = { call.respond(HttpStatusCode.BadRequest, it) },
            )
        }

        post("verify-otp") {
            val request = call.receive<VerifyOtpRequest>()
            authService.verifyOtp(request).fold(
                onSuccess = { call.respond(it) },
                onError = { call.respond(HttpStatusCode.BadRequest, it) },
            )
        }

        post("refresh") {
            val refreshToken = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            authService.refreshToken(refreshToken).fold(
                onSuccess = { call.respond(it) },
                onError = { call.respond(HttpStatusCode.Unauthorized, it) },
            )
        }

        // A password reset, after the one-time code proved the number.
        authenticate("bearer") {
            patch("password") {
                val userId = call.authentication.principal<JwtClaims>()?.userId
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<UpdatePasswordRequest>()
                authService.updatePassword(userId, request.newPassword).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }
        }
    }

    // Authenticated routes
    authenticate("bearer") {
        // Profile routes
        route("profile") {
            get {
                val userId = call.authentication.principal<JwtClaims>()?.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                profileService.findById(userId).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }

            patch {
                val userId = call.authentication.principal<JwtClaims>()?.userId ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val draft = call.receive<ProfileDraft>()
                profileService.findById(userId).fold(
                    onSuccess = { profile ->
                        val updated = profile.copy(
                            fullName = draft.name ?: profile.fullName,
                            email = draft.email ?: profile.email,
                            mobileNumber = draft.mobileNumber ?: profile.mobileNumber,
                            updatedAt = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC),
                        )
                        profileService.update(updated).fold(
                            onSuccess = { call.respond(it) },
                            onError = { call.respond(HttpStatusCode.BadRequest, it) },
                        )
                    },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }
        }

        // Doctor routes
        route("doctors") {
            get {
                val query = DoctorQuery(
                    specializationId = call.request.queryParameters["specializationId"],
                    search = call.request.queryParameters["search"],
                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20,
                    offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0,
                )
                doctorService.findAll(query).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            get("{id}") {
                val id = call.parameters["id"]!!
                doctorService.findById(id).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }

            get("{id}/schedule") {
                val id = call.parameters["id"]!!
                doctorService.getSchedule(id).fold(
                    onSuccess = { hours ->
                        val grouped = hours.groupBy { it.dayOfWeek }
                        val wire = (0..6).map { day ->
                            val sessions = grouped[day]?.filter { !it.isClosed }?.map { ScheduleBlockWire(it.startTime, it.endTime) } ?: emptyList()
                            ScheduleDayWire(day, sessions)
                        }.filter { it.sessions.isNotEmpty() || grouped.containsKey(it.dayOfWeek) }
                        call.respond(wire)
                    },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }

            get("{id}/appointments") {
                val id = call.parameters["id"]!!
                val fromParam = call.request.queryParameters["from"]
                val from = fromParam?.let { runCatching { kotlinx.datetime.LocalDateTime.parse(it) }.getOrNull() }
                if (fromParam != null && from == null) return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid 'from' date"))
                val toParam = call.request.queryParameters["to"]
                val to = toParam?.let { runCatching { kotlinx.datetime.LocalDateTime.parse(it) }.getOrNull() }
                if (toParam != null && to == null) return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid 'to' date"))
                appointmentService.findForDoctor(id, from, to).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            get("{id}/availability") {
                val id = call.parameters["id"]!!
                val date = call.request.queryParameters["date"]?.let { runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull() }
                    ?: kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC).toLocalDate()
                if (call.request.queryParameters["date"] != null && runCatching { kotlinx.datetime.LocalDate.parse(call.request.queryParameters["date"]!!) }.isFailure) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid date"))
                }
                doctorService.getAvailability(id, date).fold(
                    onSuccess = { avail ->
                        val wire = avail.map { a ->
                            DatedAvailabilityWire(a.date.toString(), a.sessions.map { ScheduleBlockWire(it.startTime, it.endTime) })
                        }
                        call.respond(wire)
                    },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            post("{id}/schedule") {
                if (!call.requireRole(UserRole.DOCTOR, UserRole.ADMIN)) return@post
                val id = call.parameters["id"]!!
                val wire = call.receive<List<ScheduleDayWire>>()
                val schedule = wire.flatMap { day ->
                    if (day.sessions.isEmpty()) listOf(ClinicHours(day.dayOfWeek, "00:00", "00:00", isClosed = true))
                    else day.sessions.map { s -> ClinicHours(day.dayOfWeek, s.startTime, s.endTime) }
                }
                doctorService.saveSchedule(id, schedule).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            post("{id}/availability") {
                if (!call.requireRole(UserRole.DOCTOR, UserRole.ADMIN)) return@post
                val id = call.parameters["id"]!!
                val wire = call.receive<DatedAvailabilityWire>()
                val date = kotlinx.datetime.LocalDate.parse(wire.date)
                val availability = AvailableDate(
                    date = date,
                    sessions = wire.sessions.map { s -> TimeSlot(s.startTime, s.endTime) },
                    isOverride = true,
                )
                doctorService.saveAvailability(id, availability).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            // Doctor-only: record outcome
            patch("appointments/{appointmentId}/outcome") {
                if (!call.requireRole(UserRole.DOCTOR, UserRole.ADMIN)) return@patch
                val userId = call.authentication.principal<JwtClaims>()?.userId ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val appointmentId = call.parameters["appointmentId"]!!
                val outcome = call.receive<VisitOutcome>()
                val status = when (outcome) {
                    VisitOutcome.COMPLETED -> AppointmentStatus.COMPLETED
                    VisitOutcome.NO_SHOW -> AppointmentStatus.NO_SHOW
                }
                appointmentService.updateStatus(appointmentId, status, outcome).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }
        }

        // Specializations
        route("specializations") {
            get {
                doctorService.getSpecializations().fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            post {
                val claims = call.authentication.principal<JwtClaims>()
                if (claims?.role != UserRole.ADMIN.serialName) {
                    return@post call.respond(HttpStatusCode.Forbidden)
                }
                val specialty = call.receive<Specialty>()
                doctorService.createSpecialization(specialty).fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            put("{id}") {
                val claims = call.authentication.principal<JwtClaims>()
                if (claims?.role != UserRole.ADMIN.serialName) {
                    return@put call.respond(HttpStatusCode.Forbidden)
                }
                val id = call.parameters["id"]!!
                val specialty = call.receive<Specialty>()
                doctorService.updateSpecialization(specialty.copy(id = id)).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            delete("{id}") {
                val claims = call.authentication.principal<JwtClaims>()
                if (claims?.role != UserRole.ADMIN.serialName) {
                    return@delete call.respond(HttpStatusCode.Forbidden)
                }
                val id = call.parameters["id"]!!
                doctorService.deleteSpecialization(id).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }
        }

        // Patient routes
        route("patients") {
            get {
                val claims = call.authentication.principal<JwtClaims>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val query = call.request.queryParameters
                val requestedDoctorId = query["doctorId"]
                val effectiveDoctorId = if (claims.role == UserRole.DOCTOR.serialName) claims.userId
                    else if (claims.role == UserRole.SECRETARY.serialName) {
                        authService.findById(claims.userId).getOrNull()?.assignedDoctorId
                    } else requestedDoctorId
                val appointments = appointmentService.findAll(AppointmentFilter(doctorId = effectiveDoctorId, limit = 500)).getOrNull()?.items ?: emptyList()
                val patientIds = appointments.map { it.patientId }.distinct()
                val patients = patientIds.mapNotNull { authService.findById(it).getOrNull() }
                call.respond(patients)
            }

            get("{id}") {
                val id = call.parameters["id"]!!
                profileService.findById(id).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }
        }

        // Appointments
        route("appointments") {
            get {
                val userId = call.authentication.principal<JwtClaims>()?.userId ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val claims = call.authentication.principal<JwtClaims>()!!
                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let { runCatching { AppointmentStatus.valueOf(it.uppercase()) }.getOrNull() }
                if (statusParam != null && status == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid status"))
                }
                val fromParam = call.request.queryParameters["from"]
                val from = fromParam?.let { runCatching { kotlinx.datetime.LocalDateTime.parse(it) }.getOrNull() }
                if (fromParam != null && from == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid 'from' date"))
                }
                val toParam = call.request.queryParameters["to"]
                val to = toParam?.let { runCatching { kotlinx.datetime.LocalDateTime.parse(it) }.getOrNull() }
                if (toParam != null && to == null) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiFailure.validation("Invalid 'to' date"))
                }
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 500)
                val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val filter = AppointmentFilter(
                    doctorId = if (claims.role == UserRole.DOCTOR.serialName) userId else null,
                    patientId = if (claims.role == UserRole.PATIENT.serialName) userId else null,
                    status = status,
                    from = from,
                    to = to,
                    limit = limit,
                    offset = offset,
                )
                appointmentService.findAll(filter).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            post {
                val claims = call.authentication.principal<JwtClaims>()!!
                if (claims.role != UserRole.PATIENT.serialName) {
                    return@post call.respond(HttpStatusCode.Forbidden)
                }
                val request = call.receive<BookingRequest>()
                val conflict = appointmentService.checkConflict(request.doctorId, request.startsAt, request.durationMinutes)
                if (conflict is LoadState.Error) {
                    return@post call.respond(HttpStatusCode.InternalServerError, conflict.failure)
                }
                if ((conflict as? LoadState.Success)?.data == true) {
                    return@post call.respond(HttpStatusCode.Conflict, ApiFailure.validation("Slot no longer available"))
                }
                val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                val appointment = Appointment(
                    id = java.util.UUID.randomUUID().toString(),
                    patientId = claims.userId,
                    doctorId = request.doctorId,
                    startsAt = request.startsAt,
                    durationMinutes = request.durationMinutes,
                    status = AppointmentStatus.BOOKED,
                    reason = request.reason,
                    createdAt = now,
                    updatedAt = now,
                )
                appointmentService.create(appointment).fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            get("{id}") {
                val id = call.parameters["id"]!!
                appointmentService.findById(id).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }
        }

        // Messages
        route("messages") {
            get("conversations") {
                val claims = call.authentication.principal<JwtClaims>()!!
                val conversations = when (claims.role) {
                    UserRole.DOCTOR.serialName -> messageService.findConversationsForDoctor(claims.userId)
                    UserRole.PATIENT.serialName -> messageService.findConversationsForPatient(claims.userId)
                    UserRole.SECRETARY.serialName -> messageService.findConversationsForSecretary(claims.userId)
                    else -> LoadState.Success(emptyList<MessageThread>())
                }
                conversations.fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            get("conversations/{id}") {
                val id = call.parameters["id"]!!
                messageService.findConversation(id).fold(
                    onSuccess = { conversation ->
                        if (conversation != null) {
                            call.respond(conversation)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    },
                    onError = { call.respond(HttpStatusCode.NotFound, it) },
                )
            }

            get("conversations/{id}/messages") {
                val id = call.parameters["id"]!!
                messageService.findMessages(id).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            post("conversations/{id}/messages") {
                val conversationId = call.parameters["id"]!!
                val claims = call.authentication.principal<JwtClaims>()!!
                val body = call.receive<String>()

                // Only a participant of the conversation may post into it.
                val thread = messageService.findConversation(conversationId).getOrNull()
                if (thread == null) return@post call.respond(HttpStatusCode.NotFound)

                val allowed = when (claims.role) {
                    UserRole.PATIENT.serialName -> thread.patientId == claims.userId
                    UserRole.DOCTOR.serialName -> thread.doctorId == claims.userId
                    UserRole.SECRETARY.serialName -> {
                        authService.findById(claims.userId).getOrNull()?.assignedDoctorId == thread.doctorId
                    }
                    else -> false
                }
                if (!allowed) return@post call.respond(HttpStatusCode.Forbidden)

                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    authorId = claims.userId,
                    authorRole = MessageAuthorRole.valueOf(claims.role.uppercase()),
                    body = body,
                    sentAt = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC),
                )
                messageService.createMessage(message).fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            patch("conversations/{id}/read") {
                val conversationId = call.parameters["id"]!!
                val claims = call.authentication.principal<JwtClaims>()!!
                messageService.markAsRead(conversationId, claims.userId).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }
        }

        // Notifications
        route("notifications") {
            get {
                val claims = call.authentication.principal<JwtClaims>()!!
                val unreadOnly = call.request.queryParameters["unread"]?.toBoolean() ?: false
                notificationService.findByUserId(claims.userId, unreadOnly).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            patch("{id}/read") {
                val id = call.parameters["id"]!!
                notificationService.markAsRead(id).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            post("read-all") {
                val claims = call.authentication.principal<JwtClaims>()!!
                notificationService.markAllAsRead(claims.userId).fold(
                    onSuccess = { call.respond(HttpStatusCode.OK) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }
        }

        // Admin routes
        route("admin") {
            get("accounts") {
                if (!call.requireRole(UserRole.ADMIN)) return@get
                adminService.getAllAccounts().fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            get("specializations") {
                if (!call.requireRole(UserRole.ADMIN)) return@get
                doctorService.getSpecializations().fold(
                    onSuccess = { call.respond(mapOf("items" to it)) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }

            post("accounts") {
                if (!call.requireRole(UserRole.ADMIN)) return@post
                val draft = call.receive<StaffDraft>()
                val kind = call.request.queryParameters["kind"] ?: draft.role ?: "doctor"
                adminService.createStaffAccount(kind, draft).fold(
                    onSuccess = { call.respond(HttpStatusCode.Created, it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            patch("accounts/{id}/status") {
                if (!call.requireRole(UserRole.ADMIN)) return@patch
                val id = call.parameters["id"]!!
                val body = call.receive<AccountStatusUpdate>()
                adminService.updateAccountStatus(id, body.status).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.BadRequest, it) },
                )
            }

            get("doctors") {
                if (!call.requireRole(UserRole.ADMIN)) return@get
                adminService.getDoctorsForAdmin().fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }
        }

        // Secretary routes
        route("secretary") {
            get("appointments") {
                if (!call.requireRole(UserRole.SECRETARY, UserRole.ADMIN)) return@get
                val claims = call.authentication.principal<JwtClaims>()!!
                val profile = authService.findById(claims.userId).getOrNull()
                val doctorId = profile?.assignedDoctorId
                val result = appointmentService.findAll(AppointmentFilter(doctorId = doctorId, limit = 500))
                call.respond(result.getOrNull()?.items ?: emptyList())
            }

            post("appointments/{id}/confirm") {
                if (!call.requireRole(UserRole.SECRETARY, UserRole.ADMIN)) return@post
                val id = call.parameters["id"]!!
                val result = appointmentService.updateStatus(id, AppointmentStatus.CONFIRMED, null)
                result.fold(onSuccess = { call.respond(it) }, onError = { call.respond(HttpStatusCode.BadRequest, it) })
            }

            post("appointments/{id}/cancel") {
                if (!call.requireRole(UserRole.SECRETARY, UserRole.ADMIN)) return@post
                val id = call.parameters["id"]!!
                val result = appointmentService.updateStatus(id, AppointmentStatus.CANCELLED, null)
                result.fold(onSuccess = { call.respond(it) }, onError = { call.respond(HttpStatusCode.BadRequest, it) })
            }

            get("patients") {
                if (!call.requireRole(UserRole.SECRETARY, UserRole.ADMIN)) return@get
                val claims = call.authentication.principal<JwtClaims>()!!
                val profile = authService.findById(claims.userId).getOrNull()
                val doctorId = profile?.assignedDoctorId
                val appointments = appointmentService.findAll(AppointmentFilter(doctorId = doctorId, limit = 500)).getOrNull()?.items ?: emptyList()
                val patientIds = appointments.map { it.patientId }.distinct()
                val patients = patientIds.mapNotNull { authService.findById(it).getOrNull() }
                call.respond(patients)
            }

            get("messages") {
                if (!call.requireRole(UserRole.SECRETARY, UserRole.ADMIN)) return@get
                val claims = call.authentication.principal<JwtClaims>()!!
                messageService.findConversationsForSecretary(claims.userId).fold(
                    onSuccess = { call.respond(it) },
                    onError = { call.respond(HttpStatusCode.InternalServerError, it) },
                )
            }
        }
    }
}

/**
 * Guards a route with a role check. Responds with 403 and returns `false` when the
 * authenticated principal does not hold one of [roles].
 */
private suspend fun ApplicationCall.requireRole(vararg roles: UserRole): Boolean {
    val claims = authentication.principal<JwtClaims>()
    if (claims == null || roles.none { it.serialName == claims.role }) {
        respond(HttpStatusCode.Forbidden)
        return false
    }
    return true
}

@Serializable
data class ProfileDraft(
    val name: String?,
    val email: String?,
    val mobileNumber: String?,
)