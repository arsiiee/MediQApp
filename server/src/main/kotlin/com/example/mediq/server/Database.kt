package com.example.mediq.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

object Database {
    private val logger = LoggerFactory.getLogger(Database::class.java)

    private fun loadDotEnv(): Map<String, String> {
        val envFile = java.io.File(".env").takeIf { it.exists() }
            ?: java.io.File("../.env").takeIf { it.exists() }
            ?: return emptyMap()
        return runCatching {
            envFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .associate { line ->
                    val idx = line.indexOf('=')
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                    key to value
                }
        }.getOrDefault(emptyMap())
    }

    fun createDataSource(config: ApplicationConfig): HikariDataSource {
        val dotEnv = loadDotEnv()
        fun getEnv(key: String): String? = System.getenv(key) ?: dotEnv[key]

        val hikariConfig = HikariConfig()
        val url = getEnv("MEDIQ_JDBC_URL")
            ?: getEnv("DB_URL")
            ?: config.propertyOrNull("db.url")?.getString()
            ?: "jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"

        hikariConfig.jdbcUrl = url
        hikariConfig.username = getEnv("MEDIQ_DB_USER")
            ?: getEnv("DB_USER")
            ?: config.propertyOrNull("db.user")?.getString()
            ?: "sa"
        hikariConfig.password = getEnv("MEDIQ_DB_PASSWORD")
            ?: getEnv("DB_PASSWORD")
            ?: config.propertyOrNull("db.password")?.getString()
            ?: ""

        val isPostgres = url.startsWith("jdbc:postgresql:")
        hikariConfig.driverClassName = if (isPostgres) {
            "org.postgresql.Driver"
        } else {
            getEnv("DB_DRIVER") ?: config.propertyOrNull("db.driver")?.getString() ?: "org.h2.Driver"
        }
        hikariConfig.maximumPoolSize = config.propertyOrNull("db.pool.size")?.getString()?.toIntOrNull() ?: 10
        hikariConfig.minimumIdle = config.propertyOrNull("db.pool.minIdle")?.getString()?.toIntOrNull() ?: 2
        hikariConfig.connectionTimeout = config.propertyOrNull("db.pool.connectionTimeout")?.getString()?.toLongOrNull() ?: 30000
        hikariConfig.idleTimeout = config.propertyOrNull("db.pool.idleTimeout")?.getString()?.toLongOrNull() ?: 600000
        hikariConfig.maxLifetime = config.propertyOrNull("db.pool.maxLifetime")?.getString()?.toLongOrNull() ?: 1800000
        hikariConfig.poolName = "MediQ-Pool"
        return HikariDataSource(hikariConfig)
    }

    fun runMigrations(dataSource: HikariDataSource) {
        runCatching {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migration")
                .baselineOnMigrate(true)
                .load()
            val migrationsApplied = flyway.migrate()
            logger.info("Applied {} migrations", migrationsApplied)
        }.onFailure { e ->
            logger.warn("Flyway migration notice: {}", e.message)
        }
    }
}