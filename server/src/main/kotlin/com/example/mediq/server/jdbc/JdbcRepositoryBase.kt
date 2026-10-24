package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.LoadState
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID

abstract class JdbcRepositoryBase(protected val dataSource: HikariDataSource) {
    protected val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    protected suspend fun <T> withConnection(block: suspend (Connection) -> T): T = withContext(Dispatchers.IO) {
        dataSource.getConnection().use { block(it) }
    }

    protected fun newId() = UUID.randomUUID().toString()
    protected fun nowUtc(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    protected fun toTimestamp(dt: LocalDateTime): Timestamp = Timestamp.valueOf(dt.toJavaLocalDateTime())
    protected fun fromTimestamp(ts: Timestamp?): LocalDateTime? = ts?.toLocalDateTime()?.toKotlinLocalDateTime()
    protected fun toDateString(ld: LocalDate?): String? = ld?.toString()
    protected fun fromDateString(s: String?): LocalDate? = s?.let { LocalDate.parse(it) }

    protected inline fun <reified T> toJson(value: T): String = json.encodeToString(value)
    protected inline fun <reified T> fromJson(value: String?): T? = value?.let { json.decodeFromString<T>(it) }
}
