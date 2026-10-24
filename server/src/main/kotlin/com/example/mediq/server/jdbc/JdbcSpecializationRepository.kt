package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcSpecializationRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), SpecializationRepository {

    override suspend fun findAll(): LoadState<List<Specialty>> = withConnection { conn ->
        conn.prepareStatement("SELECT id, name, description, doctor_count FROM specializations ORDER BY name").use { ps ->
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Specialty>()
                while (rs.next()) list.add(mapSpecialty(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun findById(id: String): LoadState<Specialty> = withConnection { conn ->
        conn.prepareStatement("SELECT id, name, description, doctor_count FROM specializations WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) LoadState.Success(mapSpecialty(rs))
                else LoadState.Error(ApiFailure.notFound("Specialization not found"))
            }
        }
    }

    override suspend fun create(specialty: Specialty): LoadState<Specialty> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO specializations (id, name, description, doctor_count) VALUES (?, ?, ?, ?)").use { ps ->
            ps.setString(1, specialty.id)
            ps.setString(2, specialty.name)
            ps.setString(3, specialty.description)
            ps.setInt(4, specialty.doctorCount)
            ps.executeUpdate()
            LoadState.Success(specialty)
        }
    }

    override suspend fun update(specialty: Specialty): LoadState<Specialty> = withConnection { conn ->
        conn.prepareStatement("UPDATE specializations SET name = ?, description = ?, doctor_count = ? WHERE id = ?").use { ps ->
            ps.setString(1, specialty.name)
            ps.setString(2, specialty.description)
            ps.setInt(3, specialty.doctorCount)
            ps.setString(4, specialty.id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Specialization not found"))
            else LoadState.Success(specialty)
        }
    }

    override suspend fun delete(id: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("DELETE FROM specializations WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeUpdate()
            LoadState.Success(Unit)
        }
    }

    private fun mapSpecialty(rs: ResultSet): Specialty =
        Specialty(
            id = rs.getString("id"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            doctorCount = rs.getInt("doctor_count"),
        )
}
