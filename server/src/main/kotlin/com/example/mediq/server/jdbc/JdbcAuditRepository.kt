package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcAuditRepository(
    dataSource: HikariDataSource,
) : JdbcRepositoryBase(dataSource), AuditRepository {

    override suspend fun findAll(): LoadState<List<AuditEntry>> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM audit_logs ORDER BY at DESC").use { ps ->
            ps.executeQuery().use { rs ->
                val list = mutableListOf<AuditEntry>()
                while (rs.next()) {
                    list.add(mapEntry(rs))
                }
                LoadState.Success(list)
            }
        }
    }

    override suspend fun create(entry: AuditEntry): LoadState<AuditEntry> = withConnection { conn ->
        val id = newId()
        val now = nowUtc()
        conn.prepareStatement(
            "INSERT INTO audit_logs (id, actor, action, target, at, severity, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setString(1, id)
            ps.setString(2, entry.actor)
            ps.setString(3, entry.action)
            ps.setString(4, entry.target)
            ps.setTimestamp(5, toTimestamp(entry.at))
            ps.setString(6, entry.severity.name)
            ps.setTimestamp(7, toTimestamp(now))
            ps.executeUpdate()
        }
        LoadState.Success(entry.copy(id = id, at = entry.at))
    }

    private fun mapEntry(rs: ResultSet): AuditEntry =
        AuditEntry(
            id = rs.getString("id"),
            actor = rs.getString("actor"),
            action = rs.getString("action"),
            target = rs.getString("target"),
            at = fromTimestamp(rs.getTimestamp("at"))!!,
            severity = AuditSeverity.valueOf(rs.getString("severity")),
        )
}
