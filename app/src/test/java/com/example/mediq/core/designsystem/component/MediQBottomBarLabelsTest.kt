package com.example.mediq.core.designsystem.component

import com.example.mediq.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the bottom navigation's labels, which are the one part of the app that
 * a unit test can check without a device.
 *
 * ## Why this file exists
 *
 * The "Appointments" label wrapped onto two lines in the navigation bar, because
 * Material3 spaces its five items with `Arrangement.spacedBy(8.dp)` and gives
 * each label `(screenWidth - 32.dp) / 5` — 57.6 dp at 320 dp, where the word
 * needs about 78.5 dp at `labelMedium`. Shortening it to "Bookings" fixed the
 * layout at every width, but shortening a visible label is exactly the change
 * that breaks WCAG 2.5.3 *Label in Name*: "the accessible name must contain the
 * text that is presented visually." Setting `contentDescription` to
 * "Appointments" — the obvious way to stop TalkBack from being renamed along
 * with the visible text — does **not** contain "Bookings", so it traded one
 * accessibility defect for another.
 *
 * Nothing would have caught either half. The repo has no composable test and no
 * instrumented test that renders a bar, and `check-contrast.ps1` measures colour
 * pairs only: it has no notion of width, of truncation, or of an accessible
 * name. A green build said nothing about any of it.
 *
 * So this pins the invariant as plain data. [BottomNavItem] is a data class and
 * [bottomNavItems] is a top-level list, so the rules below are checkable on the
 * JVM with no emulator — which is the only reason they are checkable at all
 * today. The *rendered* width still needs a device, and
 * [the appointments label carries the domain word for screen readers] is the
 * part of the change this can actually hold still.
 */
class MediQBottomBarLabelsTest {

    /**
     * WCAG 2.1 2.5.3 *Label in Name* (AA).
     *
     * A voice-control user says what they can see ("tap **Bookings**"), so the
     * accessible name has to contain that word. A name that merely paraphrases
     * the visible label fails even when it is more descriptive, which is why
     * "Appointments" is not an acceptable substitute for "Bookings".
     */
    @Test
    fun `every accessible name contains its visible label`() {
        bottomNavItems.forEach { item ->
            assertTrue(
                "WCAG 2.5.3 Label in Name: the accessible name must contain the visible " +
                    "label, so a voice-control user can say what they see. " +
                    "label=\"${item.label}\" contentDescription=\"${item.contentDescription}\"",
                item.contentDescription.contains(item.label, ignoreCase = true),
            )
        }
    }

    /**
     * The abbreviated destination still says what it is.
     *
     * "Bookings" is the short form forced by the width budget; the rest of the
     * app calls the same screen "Appointments" (its own header reads "My
     * appointments"). Dropping the domain word would leave a screen-reader user
     * with a destination name that appears nowhere else, so the accessible name
     * carries both — and still satisfies 2.5.3, which only requires that the
     * visible label appear *somewhere* in the name.
     */
    @Test
    fun `the appointments label carries the domain word for screen readers`() {
        val bookings = bottomNavItems.single { it.screen == Screen.Appointments }

        assertEquals("Bookings", bookings.label)
        assertTrue(
            "the accessible name must keep \"Appointments\", which is what the rest of " +
                "the app calls this destination, but got \"${bookings.contentDescription}\"",
            bookings.contentDescription.contains("Appointments", ignoreCase = true),
        )
    }

    /**
     * The other four are not abbreviated, so their names match their labels
     * exactly. This is what makes [BottomNavItem.contentDescription] defaulting
     * to `label` safe: only the one destination that had to shrink overrides it.
     */
    @Test
    fun `a destination that is not abbreviated needs no separate accessible name`() {
        val abbreviated = bottomNavItems.filter { it.label != it.contentDescription }

        assertEquals(
            "exactly one destination is abbreviated; if a second one changed, this " +
                "test should be updated to say why rather than silently widened",
            listOf(Screen.Appointments),
            abbreviated.map { it.screen },
        )
    }

    /** Distinct visible labels prevent ambiguous navigation. This does not measure touch targets. */
    @Test
    fun `every destination has a distinct non-blank label`() {
        val labels = bottomNavItems.map { it.label }

        assertEquals(
            "two destinations sharing a label is a voice-control ambiguity",
            labels.size,
            labels.distinct().size,
        )
        labels.forEach {
            assertTrue("a blank label renders as an empty tab", it.isNotBlank())
        }
    }

    /**
     * The five destinations are the product's navigation. Changing this list
     * changes the app's shape, so it is pinned rather than left to review.
     */
    @Test
    fun `the navigation has the five expected destinations in order`() {
        assertEquals(
            listOf(
                Screen.Home,
                Screen.Doctors,
                Screen.Appointments,
                Screen.Messages,
                Screen.Profile,
            ),
            bottomNavItems.map { it.screen },
        )
    }

    /**
     * Coarse copy-length guard, NOT proof that a label fits: glyph width, font
     * fallback and user font scale require a rendered-layout test. A future copy
     * change longer than the current longest labels deserves a device check.
     */
    @Test
    fun `visible labels stay within the current eight-character copy budget`() {
        bottomNavItems.forEach { item ->
            assertTrue(
                "\"${item.label}\" is ${item.label.length} characters. Five items across a " +
                    "320 dp screen leave 57.6 dp each, which holds about 7 characters at " +
                    "labelMedium; anything longer truncates to an ellipsis.",
                item.label.length <= MAX_LABEL_CHARACTERS,
            )
        }
    }

    /**
     * About 7 characters at `labelMedium` (12 sp) is the longest string that
     * fits 57.6 dp — see `MediQBottomBar`'s own comment for the measurement.
     */
    private companion object {
        const val MAX_LABEL_CHARACTERS = 8
    }
}
