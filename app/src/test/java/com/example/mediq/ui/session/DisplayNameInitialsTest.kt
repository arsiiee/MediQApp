package com.example.mediq.ui.session

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameInitialsTest {

    @Test
    fun `initials from full name with two parts`() {
        assertEquals("AZ", DisplayName.initials("Arweyne Zoe"))
    }

    @Test
    fun `initials from full name with three parts take first two`() {
        assertEquals("AZ", DisplayName.initials("Arweyne Zoe Salcedo"))
    }

    @Test
    fun `initials uppercase`() {
        assertEquals("JD", DisplayName.initials("john doe"))
    }

    @Test
    fun `initials with hyphenated name`() {
        assertEquals("AM", DisplayName.initials("Anne-Marie Smith"))
    }

    @Test
    fun `initials empty or blank`() {
        assertEquals("", DisplayName.initials(""))
        assertEquals("", DisplayName.initials("   "))
    }

    @Test
    fun `initials single name`() {
        assertEquals("A", DisplayName.initials("Arweyne"))
    }
}
