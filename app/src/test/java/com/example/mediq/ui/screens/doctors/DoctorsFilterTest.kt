package com.example.mediq.ui.screens.doctors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour of [filterDoctors].
 *
 * Plain JVM tests: the filter is a pure function, so no emulator is involved.
 */
class DoctorsFilterTest {

    private val doctors = listOf(
        DoctorInfo(
            id = "1",
            name = "Dr. Maria Elena Sandoval",
            initials = "ME",
            specialty = "Internal Medicine",
            experience = 14,
            fee = 700,
            location = "Main Building — 2F · Clinic 204",
            openSlots = 13
        ),
        DoctorInfo(
            id = "2",
            name = "Dr. Joel Marquez",
            initials = "JM",
            specialty = "Pediatrics",
            experience = 18,
            fee = 500,
            location = "Annex Wing — 1F · Clinic 108",
            openSlots = 18
        ),
        DoctorInfo(
            id = "3",
            name = "Dr. Antonio Reyes Jr.",
            initials = "AR",
            specialty = "Cardiology",
            experience = 20,
            fee = 1000,
            location = "Main Building — 3F · Heart Station 301",
            openSlots = 10
        )
    )

    private fun ids(result: List<DoctorInfo>) = result.map { it.id }

    @Test
    fun `no filters returns the whole catalogue in order`() {
        val result = filterDoctors(doctors, DoctorFilters.Default)

        assertEquals(listOf("1", "2", "3"), ids(result))
    }

    @Test
    fun `query matches a name case insensitively`() {
        val result = filterDoctors(doctors, DoctorFilters.Default.copy(query = "marquez"))

        assertEquals(listOf("2"), ids(result))
    }

    @Test
    fun `query also matches a specialty`() {
        val result = filterDoctors(doctors, DoctorFilters.Default.copy(query = "cardio"))

        assertEquals(listOf("3"), ids(result))
    }

    @Test
    fun `blank query does not narrow the list`() {
        val result = filterDoctors(doctors, DoctorFilters.Default.copy(query = "   "))

        assertEquals(3, result.size)
    }

    @Test
    fun `specialty filter is an exact match`() {
        val result = filterDoctors(
            doctors,
            DoctorFilters.Default.copy(specialty = "Pediatrics")
        )

        assertEquals(listOf("2"), ids(result))
    }

    @Test
    fun `location filter matches the start of the room string`() {
        val result = filterDoctors(
            doctors,
            DoctorFilters.Default.copy(location = "Annex Wing — 1F")
        )

        assertEquals(listOf("2"), ids(result))
    }

    @Test
    fun `filters combine with AND`() {
        val result = filterDoctors(
            doctors,
            DoctorFilters.Default.copy(
                specialty = "Cardiology",
                location = "Annex Wing — 1F"
            )
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `all-specialties and all-locations placeholders do not filter`() {
        val result = filterDoctors(
            doctors,
            DoctorFilters(specialty = AllSpecialties, location = AllLocations)
        )

        assertEquals(3, result.size)
    }

    @Test
    fun `filter defaults report themselves as unfiltered`() {
        assertTrue(DoctorFilters.Default.isDefault)
        assertTrue(!DoctorFilters.Default.copy(query = "a").isDefault)
        assertTrue(!DoctorFilters.Default.copy(specialty = "Cardiology").isDefault)
        assertTrue(!DoctorFilters.Default.copy(location = "Annex Wing — 1F").isDefault)
    }
}
