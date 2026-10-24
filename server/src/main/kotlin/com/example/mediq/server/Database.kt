package com.example.mediq.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

object Database {
    private val logger = LoggerFactory.getLogger(Database::class.java)

    fun createDataSource(config: ApplicationConfig): HikariDataSource {
        val hikariConfig = HikariConfig()
        // HOCON config (application.conf) takes precedence; environment variables
        // (DB_URL, DB_USER, DB_PASSWORD, DB_DRIVER) override for production/Supabase.
        hikariConfig.jdbcUrl = config.propertyOrNull("db.url")?.getString()
            ?: System.getenv("DB_URL")
            ?: "jdbc:h2:mem:mediq;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        hikariConfig.username = config.propertyOrNull("db.user")?.getString()
            ?: System.getenv("DB_USER")
            ?: "sa"
        hikariConfig.password = config.propertyOrNull("db.password")?.getString()
            ?: System.getenv("DB_PASSWORD")
            ?: ""
        hikariConfig.driverClassName = config.propertyOrNull("db.driver")?.getString()
            ?: System.getenv("DB_DRIVER")
            ?: "org.h2.Driver"
        hikariConfig.maximumPoolSize = config.propertyOrNull("db.pool.size")?.getString()?.toIntOrNull() ?: 10
        hikariConfig.minimumIdle = config.propertyOrNull("db.pool.minIdle")?.getString()?.toIntOrNull() ?: 2
        hikariConfig.connectionTimeout = config.propertyOrNull("db.pool.connectionTimeout")?.getString()?.toLongOrNull() ?: 30000
        hikariConfig.idleTimeout = config.propertyOrNull("db.pool.idleTimeout")?.getString()?.toLongOrNull() ?: 600000
        hikariConfig.maxLifetime = config.propertyOrNull("db.pool.maxLifetime")?.getString()?.toLongOrNull() ?: 1800000
        hikariConfig.poolName = "MediQ-Pool"
        return HikariDataSource(hikariConfig)
    }

    fun runMigrations(dataSource: HikariDataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .baselineOnMigrate(true)
            .load()
        val migrationsApplied = flyway.migrate()
        logger.info("Applied {} migrations", migrationsApplied)
    }
}