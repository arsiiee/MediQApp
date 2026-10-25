package com.example.mediq.service

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AuditService(
    private val auditRepository: AuditRepository,
) {
    suspend fun findAll(): LoadState<List<AuditEntry>> = auditRepository.findAll()

    suspend fun create(
        actor: String,
        action: String,
        target: String,
        severity: AuditSeverity,
    ): LoadState<AuditEntry> {
        val entry = AuditEntry(
            id = "",
            actor = actor,
            action = action,
            target = target,
            at = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            severity = severity,
        )
        return auditRepository.create(entry)
    }
}
