package com.example.mediq.server

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.http.configureRoutes
import com.example.mediq.server.jdbc.DatabaseSeeder
import com.example.mediq.server.jdbc.JdbcAdminRepository
import com.example.mediq.server.jdbc.JdbcAppointmentRepository
import com.example.mediq.server.jdbc.JdbcAuthRepository
import com.example.mediq.server.jdbc.JdbcDoctorRepository
import com.example.mediq.server.jdbc.JdbcMessageRepository
import com.example.mediq.server.jdbc.JdbcNotificationRepository
import com.example.mediq.server.jdbc.JdbcProfileRepository
import com.example.mediq.server.jdbc.JdbcSpecializationRepository
import com.example.mediq.security.JwtClaims
import com.example.mediq.security.JwtProvider
import com.example.mediq.security.PasswordHash
import com.example.mediq.service.*
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) {
    // The application module is wired in directly so the server is configured exactly once.
    val port = System.getenv("PORT")?.toIntOrNull()
        ?: System.getenv("MEDIQ_PORT")?.toIntOrNull()
        ?: System.getProperty("ktor.deployment.port")?.toIntOrNull()
        ?: 8099
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureApplication()
    }.start(wait = true)
}

// To use a different port, run with: -Dktor.deployment.port=8099

@Suppress("UNUSED_PARAMETER")
fun Application.configureApplication() {
    val appLogger = LoggerFactory.getLogger(Application::class.java)

    // Configuration
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: "mediq-dev-secret-change-in-production"
    val corsOrigins = environment.config.propertyOrNull("cors.origins")?.getString()?.split(",")
        ?: listOf("http://localhost:4200")

    // Services
    val passwordHash = PasswordHash()
    val jwtProvider = JwtProvider(jwtSecret)

    // Database
    val dataSource = Database.createDataSource(environment.config)
    Database.runMigrations(dataSource)

    // Repository implementations (JDBC-backed; configure db.url/db.user/db.password for PostgreSQL)
    val appointmentRepository = JdbcAppointmentRepository(dataSource)
    val authRepository = JdbcAuthRepository(dataSource, passwordHash)
    val doctorRepository = JdbcDoctorRepository(dataSource)
    val messageRepository = JdbcMessageRepository(dataSource)
    val notificationRepository = JdbcNotificationRepository(dataSource)
    val profileRepository = JdbcProfileRepository(dataSource)
    val specializationRepository = JdbcSpecializationRepository(dataSource)
    val adminRepository = JdbcAdminRepository(dataSource, passwordHash)

    // Seed demo data on first run (no-op once tables are populated)
    DatabaseSeeder(dataSource, passwordHash).seedIfEmpty()

    // Services
    val authService = AuthService(authRepository, jwtProvider, passwordHash)
    val doctorService = DoctorService(doctorRepository, specializationRepository)
    val adminService = AdminService(adminRepository, doctorRepository, authRepository, passwordHash)
    val appointmentService = AppointmentService(appointmentRepository, doctorRepository)
    val messageService = MessageService(messageRepository)
    val notificationService = NotificationService(notificationRepository)
    val profileService = ProfileService(profileRepository)

    // Serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }

    // Status pages: every error becomes a JSON ApiFailure envelope (never an empty body)
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiFailure.validation(cause.message ?: "Invalid request"))
        }
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiFailure.validation(cause.message ?: "Malformed request"))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ApiFailure.internal(cause.message ?: "Unexpected error"))
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ApiFailure.unauthorized("Authentication required"))
        }
        status(HttpStatusCode.Forbidden) { call, status ->
            call.respond(status, ApiFailure.forbidden("Insufficient permissions"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiFailure.notFound("Resource not found"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ApiFailure("RATE_LIMITED", "Rate limit exceeded"))
        }
    }

    // Logging
    install(CallLogging) {
        logger = appLogger
        format { call ->
            "${call.request.httpMethod} ${call.request.uri} -> ${call.response.status()}"
        }
    }

    // CORS
    install(CORS) {
        corsOrigins.forEach { origin ->
            // Ktor 3 expects the host (and optionally the scheme) separately.
            val uri = runCatching { java.net.URI(origin) }.getOrNull()
            val host = uri?.host ?: origin
            val hostWithPort = if (uri != null && uri.port > 0) "$host:${uri.port}" else host
            val schemes = uri?.scheme?.let { listOf(it) } ?: emptyList()
            allowHost(hostWithPort, schemes)
        }
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader("Authorization")
        allowHeader("Content-Type")
        allowHeader("Accept")
        allowCredentials = true
        maxAgeInSeconds = 3600
    }

    // Rate limiting
    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteHost }
        }
    }

    // Authentication
    install(Authentication) {
        jwt("bearer") {
            verifier(jwtProvider.verifier)
            validate { credential ->
                val claims = jwtProvider.verifyAccessToken(
                    request.headers["Authorization"]?.removePrefix("Bearer ").orEmpty()
                )
                if (claims != null) {
                    claims
                } else {
                    val userId = credential.subject
                    val role = credential.getClaim("role", String::class)
                    if (userId != null && role != null) {
                        JwtClaims(
                            userId = userId,
                            username = credential.getClaim("username", String::class) ?: "",
                            role = role,
                            fullName = credential.getClaim("fullName", String::class) ?: "",
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    // Routes
    routing {
        configureRoutes(
            authService = authService,
            doctorService = doctorService,
            adminService = adminService,
            appointmentService = appointmentService,
            messageService = messageService,
            notificationService = notificationService,
            profileService = profileService,
            jwtProvider = jwtProvider,
        )

        // API documentation
        swaggerUI("/swagger-ui", "openapi/documentation.yaml")

        // Health check
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
