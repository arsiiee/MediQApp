package com.example.mediq.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

/**
 * Configuration, read from environment variables.
 *
 * Every value has a development default so the server runs with no setup, but
 * the two that matter for security have no usable default in production —
 * see [ServerConfig.validate].
 */
data class ServerConfig(
    val port: Int,
    val jdbcUrl: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val tokenTtlMinutes: Long,
    val slotDurationMinutes: Int,
    val otpTtlMinutes: Long,
    val seedDemoData: Boolean,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig = ServerConfig(
            port = env["MEDIQ_PORT"]?.toInt() ?: 8080,
            // In-memory by default so `run` works with nothing installed. Point
            // this at Postgres before there are real patients: H2 has no
            // concurrent-write behaviour like a real server.
            jdbcUrl = env["MEDIQ_JDBC_URL"] ?: "jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1",
            jwtSecret = env["MEDIQ_JWT_SECRET"] ?: "dev-only-insecure-secret-change-me",
            jwtIssuer = env["MEDIQ_JWT_ISSUER"] ?: "mediq",
            tokenTtlMinutes = env["MEDIQ_TOKEN_TTL_MINUTES"]?.toLong() ?: 60,
            slotDurationMinutes = env["MEDIQ_SLOT_MINUTES"]?.toInt() ?: 30,
            otpTtlMinutes = env["MEDIQ_OTP_TTL_MINUTES"]?.toLong() ?: 5,
            seedDemoData = env["MEDIQ_SEED_DEMO"]?.lowercase() == "true",
        )

        /**
         * Refuses to start with the development secret. A leaked signing key
         * means anyone can mint a token for any patient.
         */
        fun validate(config: ServerConfig, isProduction: Boolean) {
            if (!isProduction) return
            if (config.jwtSecret == "dev-only-insecure-secret-change-me" || config.jwtSecret.length < 32) {
                error(
                    "MEDIQ_JWT_SECRET must be set to a random value of at least 32 characters. " +
                        "Generate one with: openssl rand -base64 48"
                )
            }
            if (config.jdbcUrl.startsWith("jdbc:h2")) {
                error("MEDIQ_JDBC_URL points at H2. Use a real database before storing patient records.")
            }
        }
    }
}

/**
 * Owns the connection pool and applies the schema on startup.
 *
 * Applying [schema.sql] at boot keeps the first run to a single command. It
 * uses `CREATE TABLE IF NOT EXISTS` throughout, so it is safe on an existing
 * database — but it is not a migration tool. Once real data exists, switch to
 * Flyway or Liquibase before changing any column.
 */
class Database(private val config: ServerConfig) {

    private val dataSource: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            maximumPoolSize = 10
            isAutoCommit = true
            // H2 console, local development only.
            if (config.jdbcUrl.startsWith("jdbc:h2")) {
                addDataSourceProperty("DB_CLOSE_ON_EXIT", "FALSE")
            }
        }
    )

    init {
        applySchema()
    }

    private fun applySchema() {
        val sql = Database::class.java.getResourceAsStream("/schema.sql")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("schema.sql is missing from the server resources.")

        connection().use { c ->
            c.createStatement().use { st ->
                splitStatements(sql).forEach { st.execute(it) }
            }
        }
    }

    fun connection(): Connection = dataSource.connection

    /**
     * Runs [block] inside one transaction.
     *
     * Booking depends on this: the claim insert, the appointment insert, and
     * the notification must all succeed or none of them happen. A partially
     * applied booking would mark a slot taken for an appointment the patient
     * never received.
     */
    fun <T> tx(block: (Connection) -> T): T {
        connection().use { c ->
            val previousAutoCommit = c.autoCommit
            c.autoCommit = false
            try {
                val result = block(c)
                c.commit()
                return result
            } catch (t: Throwable) {
                runCatching { c.rollback() }
                throw t
            } finally {
                runCatching { c.autoCommit = previousAutoCommit }
            }
        }
    }

    /** Reads without opening a transaction. */
    fun <T> read(block: (Connection) -> T): T = connection().use(block)

    fun close() = dataSource.close()

    private companion object {
        /**
         * Splits on semicolons after stripping `--` comments, so a semicolon
         * inside an explanatory comment does not cut a statement in half.
         */
        fun splitStatements(sql: String): List<String> =
            sql.lines()
                .map { it.substringBefore("--") }
                .joinToString("\n")
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
    }
}