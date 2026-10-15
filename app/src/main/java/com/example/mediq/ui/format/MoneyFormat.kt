package com.example.mediq.ui.format

import com.example.mediq.domain.model.Money

/**
 * Money's two display forms, in one place.
 *
 * Both used to be private extensions on whichever screen needed them, which is
 * how two screens end up formatting the same amount differently. They live in
 * `ui/` rather than on [Money] because `AGENTS.md` keeps formatting out of
 * `domain/` — the server compiles that, and it has no business rendering a peso
 * sign. The models stay plain; the UI joins them into something readable.
 *
 * Which one to use is a rule, not a preference: **lists round to whole pesos and
 * summaries show centavos.** A list row answers "roughly what will this cost" and
 * `₱850.00` in a card title is visual noise; a fee card or a confirmation is the
 * amount someone is agreeing to, so it shows the centavos it will be charged.
 */

/** `₱850` — whole pesos, for a list row. Centavos are dropped, not rounded. */
fun Money.format(): String = "\u20b1$pesos"

/**
 * `₱850.00` — for a fee card, a confirmation, or a detail row, where the exact
 * amount matters. The centavos are zero-padded: `Money(85005)` is `₱850.05`, not
 * `₱850.5`.
 */
fun Money.formatWithCentavos(): String =
    "\u20b1$pesos.${centavos.toString().padStart(2, '0')}"
