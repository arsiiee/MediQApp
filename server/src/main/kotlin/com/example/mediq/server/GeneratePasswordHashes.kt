/**
 * Generate BCrypt password hashes for all seeded doctors using the server's
 * existing PasswordHash class, then print SQL you can paste into your DB.
 *
 * Drop this into a temporary Kotlin file under server/src/main/kotlin/,
 * run `./gradlew run` (or a test), copy the output, insert into
 * password_hashes, then delete the temp file.
 */
package com.example.mediq.server

import com.example.mediq.security.PasswordHash
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File

fun main() {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:tcp://localhost/~/mediq" // adjust to your local DB URL
        username = "sa"
        password = ""
    }
    val ds = HikariDataSource(config)
    val ph = PasswordHash()

    val doctors = listOf(
        "dr-01" to "demo12345",
        "dr-02" to "demo12345",
        "dr-03" to "demo12345",
        "dr-04" to "demo12345",
        "dr-05" to "demo12345",
        "dr-06" to "demo12345",
        "dr-07" to "demo12345",
        "dr-08" to "demo12345",
        "dr-09" to "demo12345",
        "dr-10" to "demo12345",
        "dr-11" to "demo12345",
        "dr-12" to "demo12345",
        "dr-13" to "demo12345",
        "dr-14" to "demo12345",
        "dr-15" to "demo12345",
    )

    println("-- Password hashes for seeded doctors (paste into password_hashes table)")
    for ((userId, password) in doctors) {
        val hash = ph.hash(password)
        println("INSERT INTO password_hashes (user_id, hash) VALUES ('$userId', '$hash');")
    }

    ds.close()
}
