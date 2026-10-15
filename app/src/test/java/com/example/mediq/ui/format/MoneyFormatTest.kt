package com.example.mediq.ui.format

import com.example.mediq.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the two money forms the mockups ask for: `₱850` in the doctors list and
 * `₱850.00` on a fee card or a confirmation.
 *
 * The case that matters is a centavo value below ten. `Money(85005)` is 850 pesos
 * and 5 centavos, and `centavos.toString()` is `"5"` — so without the zero padding
 * the amount renders as `₱850.5`, which is a different number on a screen someone
 * is agreeing to pay.
 */
class MoneyFormatTest {

    @Test
    fun `the list form trims to whole pesos`() {
        assertEquals("\u20b1850", Money(85_000).format())
    }

    @Test
    fun `the list form drops the centavos rather than rounding them up`() {
        assertEquals("\u20b1850", Money(85_099).format())
    }

    @Test
    fun `the summary form always shows two decimals`() {
        assertEquals("\u20b1850.00", Money(85_000).formatWithCentavos())
    }

    @Test
    fun `a single-digit centavo value is zero-padded`() {
        assertEquals("\u20b1850.05", Money(85_005).formatWithCentavos())
    }

    @Test
    fun `a non-round amount keeps its centavos`() {
        assertEquals("\u20b1850.50", Money(85_050).formatWithCentavos())
        assertEquals("\u20b1850", Money(85_050).format())
    }

    @Test
    fun `nothing owed is still two forms, not one`() {
        assertEquals("\u20b10", Money(0).format())
        assertEquals("\u20b10.00", Money(0).formatWithCentavos())
    }
}
