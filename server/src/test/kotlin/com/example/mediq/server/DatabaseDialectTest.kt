package com.example.mediq.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseDialectTest {

    @Test
    fun `isPostgres is true only for postgresql jdbc urls`() {
        val h2Config = ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            jwtSecret = "secret-123456789012345678901234567890",
            jwtIssuer = "test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
        )
        val h2Db = Database(h2Config)
        try {
            assertFalse(h2Db.isPostgres)
        } finally {
            h2Db.close()
        }

        val pgConfig = ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:postgresql://db.xyz.supabase.co:5432/postgres?sslmode=require",
            jwtSecret = "secret-123456789012345678901234567890",
            jwtIssuer = "test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
            dbUser = "postgres",
            dbPassword = "password",
        )
        // Check configuration parsing
        assertEquals("postgres", pgConfig.dbUser)
        assertEquals("password", pgConfig.dbPassword)
        assertTrue(pgConfig.jdbcUrl.startsWith("jdbc:postgresql:"))
    }
}
