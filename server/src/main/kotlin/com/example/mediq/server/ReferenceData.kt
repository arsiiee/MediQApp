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

    /**
     * The prose the admin area shows beside each specialty.
     *
     * Written down here rather than read off the enum, because the column is the
     * store: `specialties.description` is the one part of this table a future
     * admin screen would edit, which is what makes it content rather than
     * contract. The list of specialties still comes from [Specialty], so a
     * description can never introduce a specialty the patient app does not offer.
     *
     * Total over the enum by test: a specialty added without a description fails
     * `AdminAccountsTest` rather than shipping a blank card.
     */
    private val DESCRIPTIONS = mapOf(
        Specialty.INTERNAL_MEDICINE to
            "General adult medicine: chronic conditions, infections, and routine check-ups.",
        Specialty.PEDIATRICS to
            "Care for infants, children, and adolescents, including immunisation.",
        Specialty.CARDIOLOGY to
            "Heart and blood vessel conditions, from screening to long-term management.",
        Specialty.DERMATOLOGY to
            "Skin, hair, and nail conditions, including rash assessment and screening.",
        Specialty.OB_GYNECOLOGY to
            "Pregnancy care and reproductive health.",
        Specialty.ORTHOPEDICS to
            "Bones, joints, and muscles, including injury and post-operative follow-up.",
    )

    /** Exposed so a test can assert the map covers every specialty. */
    fun descriptionOf(specialty: Specialty): String = DESCRIPTIONS[specialty] ?: ""

    fun seedSpecialties(db: Database) {
        db.tx { c ->
            val existing = c.prepareStatement("SELECT id FROM specialties").use { st ->
                st.executeQuery().use { rs -> buildSet { while (rs.next()) add(rs.getString(1)) } }
            }

            // Skips rows that already exist, so this is safe to run every boot
            // and safe against a database that a future migration populated.
            c.prepareStatement(
                """
                INSERT INTO specialties (id, display_name, description)
                SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM specialties WHERE id = ?)
                """.trimIndent()
            ).use { st ->
                Specialty.entries
                    .filter { it.wireValue !in existing }
                    .forEach { specialty ->
                        st.setString(1, specialty.wireValue)
                        st.setString(2, specialty.displayName)
                        st.setString(3, descriptionOf(specialty))
                        st.setString(4, specialty.wireValue)
                        st.addBatch()
                    }
                st.executeBatch()
            }

            // A database created before the description column already has all
            // six rows, so the insert above skips every one of them and the text
            // would never arrive. Only nulls are filled: prose someone has since
            // edited is theirs, and this seed is not entitled to overwrite it.
            c.prepareStatement(
                "UPDATE specialties SET description = ? WHERE id = ? AND description IS NULL"
            ).use { st ->
                Specialty.entries.forEach { specialty ->
                    st.setString(1, descriptionOf(specialty))
                    st.setString(2, specialty.wireValue)
                    st.addBatch()
                }
                st.executeBatch()
            }
        }
    }
}