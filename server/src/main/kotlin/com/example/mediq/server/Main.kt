package com.example.mediq.server

import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.NotificationStore
import com.example.mediq.server.db.UserStore
import com.example.mediq.server.http.configureAuth
import com.example.mediq.server.http.configureStatusPages
import com.example.mediq.server.http.healthRoute
import com.example.mediq.server.http.mediQRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val isProduction = System.getenv("MEDIQ_ENV") == "production"
    val config = ServerConfig.fromEnv()
    ServerConfig.validate(config, isProduction)

    val db = Database(config)
    val users = UserStore(db)
    val doctors = DoctorStore(db, config.slotDurationMinutes)
    val appointments = AppointmentStore(db, doctors)
    val notifications = NotificationStore(db)
    val tokens = Tokens(config.jwtSecret, config.jwtIssuer)
    val auth = AuthService(db, users, tokens, config.tokenTtlMinutes, config.otpTtlMinutes)

    // Specialties are the shared enum, not content: without these rows a doctor
    // cannot reference a specialty at all, so they are seeded unconditionally.
    ReferenceData.seedSpecialties(db)

    if (config.seedDemoData) {
        DemoData.seed(db)
    }

    Runtime.getRuntime().addShutdownHook(Thread { db.close() })

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        mediQModule(config, db, users, doctors, appointments, notifications, auth, tokens)
    }.start(wait = true)
}

/**
 * Wiring kept separate from [main] so tests can start the same application
 * without binding a port.
 */
fun Application.mediQModule(
    config: ServerConfig,
    db: Database,
    users: UserStore,
    doctors: DoctorStore,
    appointments: AppointmentStore,
    notifications: NotificationStore,
    auth: AuthService,
    tokens: Tokens,
) {
    // Installed before the rest: a browser's preflight is an unauthenticated
    // OPTIONS request, so if this ran after Authentication the preflight itself
    // would be rejected and the real request would never be sent.
    install(CORS) {
        // Matched as the exact `scheme://host:port` the browser sends, so a
        // different port on the same host is not implicitly allowed. The
        // predicate form is used rather than `allowHost` because the latter
        // ignores the port entirely.
        config.webOrigins.forEach { origin ->
            allowOrigins { it == origin }
        }
        // The bearer token travels in a header, which is not a "simple" header,
        // so the browser asks permission for it on every cross-origin call.
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    install(ContentNegotiation) {
        json(
            Json {
                // A missing field is a bug on one side or the other. Silently
                // filling it in would hide exactly the mismatches this contract
                // exists to catch.
                ignoreUnknownKeys = false
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }

    install(CallLogging)

    install(Authentication) {
        configureAuth(tokens, users)
    }

    install(StatusPages) {
        configureStatusPages()
    }

    routing {
        healthRoute()
        mediQRoutes(
            db = db,
            config = config,
            users = users,
            doctors = doctors,
            appointments = appointments,
            notifications = notifications,
            auth = auth,
            tokens = tokens,
        )
    }
}