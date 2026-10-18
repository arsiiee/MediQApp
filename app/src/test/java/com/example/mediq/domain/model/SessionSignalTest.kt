package com.example.mediq.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSignalTest {

    @Test
    fun `starts false`() {
        val signal = SessionSignal()
        assertFalse(signal.isExpired.value)
    }

    @Test
    fun `markExpired flips it to true`() {
        val signal = SessionSignal()
        signal.markExpired()
        assertTrue(signal.isExpired.value)
    }

    @Test
    fun `markExpired is idempotent`() {
        val signal = SessionSignal()
        signal.markExpired()
        signal.markExpired()
        assertTrue(signal.isExpired.value)
    }

    @Test
    fun `clear resets it back to false`() {
        val signal = SessionSignal()
        signal.markExpired()
        assertTrue(signal.isExpired.value)

        signal.clear()
        assertFalse(signal.isExpired.value)
    }
}
