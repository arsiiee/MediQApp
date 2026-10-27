package com.example.mediq.ui.screens.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileDetailRowsTest {

    @Test
    fun `skip empty rows when values missing`() {
        val profile = ProfileData(fullName = "Test User", email = null, initials = "TU", phoneNumber = null, dateOfBirth = null)
        assertTrue(profileDetails(profile).isEmpty())
    }

    @Test
    fun `skip blank strings`() {
        val profile = ProfileData(fullName = "Test User", email = "", initials = "TU", phoneNumber = "   ", dateOfBirth = "")
        assertTrue(profileDetails(profile).isEmpty())
    }

    @Test
    fun `show only present fields`() {
        val profile = ProfileData(fullName = "Test User", email = "test@example.com", initials = "TU", phoneNumber = null, dateOfBirth = "1990-01-01")
        val rows = profileDetails(profile)
        assertEquals(2, rows.size)
        assertEquals("Email", rows[0].label)
        assertEquals("test@example.com", rows[0].value)
        assertEquals("Date of birth", rows[1].label)
        assertEquals("1990-01-01", rows[1].value)
    }

    @Test
    fun `show all fields when present`() {
        val profile = ProfileData(fullName = "Arweyne Zoe Salcedo", email = "zoe.salcedo@email.com", initials = "AZ", phoneNumber = "0917 442 8810", dateOfBirth = "1990-01-01")
        val rows = profileDetails(profile)
        assertEquals(3, rows.size)
        assertEquals(ProfileDetail("Email", "zoe.salcedo@email.com"), rows[0])
        assertEquals(ProfileDetail("Mobile number", "0917 442 8810"), rows[1])
        assertEquals(ProfileDetail("Date of birth", "1990-01-01"), rows[2])
    }
}
