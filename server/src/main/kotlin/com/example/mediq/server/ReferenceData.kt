package com.example.mediq.server

import com.example.mediq.domain.model.Specialty

/**
 * Rows the clinic cannot run without.
 *
 * Distinct from [DemoData]: specialties are the shared enum the app's
 * `Specialty` type is generated from, so they are contract rather than content.
 * Seeding them here means a fresh database can hold a doctor immediately,
 * without anyone having to enable demo data.
 */
object ReferenceData {

    fun seedSpecialties(db: Database) {
        db.tx { c ->
            val existing = c.prepareStatement("SELECT id FROM specialties").use { st ->
                st.executeQuery().use { rs -> buildSet { while (rs.next()) add(rs.getString(1)) } }
            }

            // Skips rows that already exist, so this is safe to run every boot
            // and safe against a database that a future migration populated.
            c.prepareStatement(
                """
                INSERT INTO specialties (id, display_name)
                SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM specialties WHERE id = ?)
                """.trimIndent()
            ).use { st ->
                Specialty.entries
                    .filter { it.wireValue !in existing }
                    .forEach { specialty ->
                        st.setString(1, specialty.wireValue)
                        st.setString(2, specialty.displayName)
                        st.setString(3, specialty.wireValue)
                        st.addBatch()
                    }
                st.executeBatch()
            }
        }
    }
}