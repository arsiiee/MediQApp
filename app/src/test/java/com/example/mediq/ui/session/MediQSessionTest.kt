package com.example.mediq.ui.session

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/*
 * The screens used to print one fixed patient name, so a different patient
 * signing in still saw that name on the home header and on the profile. These
 * rules are pure data, so they are pinned down here rather than by typing into
 * a device.
 */
class MediQSessionTest {

    @Before
    fun setUp() {
        MediQSession.reset()
    }

    @After
    fun tearDown() {
        MediQSession.reset()
    }

    // Names

    @Test
    fun `a username becomes a readable name`() {
        assertEquals("Kissie Apple", DisplayName.fromUsername("kissie.apple"))
        assertEquals("Demo Patient", DisplayName.fromUsername("demo_patient"))
        assertEquals("Patient One", DisplayName.fromUsername("patient-one"))
    }

    @Test
    fun `a name keeps the capitalisation it was given`() {
        assertEquals("Kissie Ann Apple", DisplayName.fromUsername("Kissie Ann Apple"))
    }

    @Test
    fun `the first name and the initials come from the full name`() {
        assertEquals("Arweyne", DisplayName.firstName("Arweyne Zoe Salcedo"))
        assertEquals("AZ", DisplayName.initials("Arweyne Zoe Salcedo"))
        assertEquals("AS", DisplayName.initials("Arweyne Salcedo"))
    }

    @Test
    fun `an empty name has no first name and no initials`() {
        assertEquals("", DisplayName.firstName(""))
        assertEquals("", DisplayName.initials(""))
    }

    // Session

    @Test
    fun `nobody is signed in to start with`() {
        assertNull(MediQSession.user.value)
    }

    @Test
    fun `signing in records the account`() {
        MediQSession.signIn("demo_patient")

        val user = MediQSession.user.value
        assertEquals("demo_patient", user?.username)
        assertEquals("Demo Patient", user?.fullName)
    }

    @Test
    fun `an account created on this device keeps the name it was registered with`() {
        MediQSession.beginRegistration("Kissie Ann Apple", "0917 555 0142")
        MediQSession.completeRegistration("kissie.apple")

        MediQSession.signIn("kissie.apple")

        val user = MediQSession.user.value
        assertEquals("Kissie Ann Apple", user?.fullName)
        assertEquals("0917 555 0142", user?.phoneNumber)
    }

    @Test
    fun `signing out forgets the account`() {
        MediQSession.signIn("demo_patient")

        MediQSession.signOut()

        assertNull(MediQSession.user.value)
    }

    @Test
    fun `a blank username is not signed in`() {
        MediQSession.signIn("   ")

        assertNull(MediQSession.user.value)
    }
}
