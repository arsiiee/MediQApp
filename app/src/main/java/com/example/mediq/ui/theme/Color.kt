package com.example.mediq.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * MediQ colour tokens.
 *
 * Rules:
 *  - Screens import from this file. No `Color(0x…)` literals outside `ui/theme`.
 *  - Prefer a semantic token (MediQTextSecondary) over a brand token (MediQGreen).
 *  - Values are consolidated from real usage across the screens. Where several
 *    near-identical greys existed, the most-used value won; see
 *    docs/design-contract.md §1 for the census.
 */

// ── Brand ───────────────────────────────────────────────────────────────────

/** Primary brand green. Header surfaces, primary buttons, selected nav, links. */
val MediQGreen = Color(0xFF006B3C)

/** Darker brand green for pressed states and emphasis on light green. */
val MediQDarkGreen = Color(0xFF004D2C)

/** Low-emphasis brand container. Status chips and badges. */
val MediQLightGreen = Color(0xFFE8F5E9)

/** Controls drawn on top of a brand green header: avatar, icon buttons. */
val MediQHeaderControl = Color(0xFF2D8650)

/** Text and icons drawn on top of [MediQGreen] or [MediQHeaderControl]. */
val MediQOnBrand = Color(0xFFFFFFFF)

// ── Surfaces ────────────────────────────────────────────────────────────────

/** App and card background. */
val MediQBackground = Color(0xFFFFFFFF)

/** Grouped surface inside a white screen. */
val MediQSurface = Color(0xFFF5F5F5)

/** Full-screen background behind white cards. Consolidated from F5F8F6 / F3F5F4. */
val MediQScaffoldBackground = Color(0xFFF5F8F6)

/** Filled text field and search field container. */
val MediQFieldContainer = Color(0xFFF2F6F3)

/** Neutral container for inactive chips and rows. Consolidated from F1F3F2 / F1F4F2 / F1F5F2. */
val MediQNeutralContainer = Color(0xFFF1F3F2)

// ── Text ────────────────────────────────────────────────────────────────────

/** Body copy and headings. */
val MediQTextPrimary = Color(0xFF212121)

/** Labels, supporting copy, metadata. */
val MediQTextSecondary = Color(0xFF757575)

/** Placeholder copy inside inputs. */
val MediQTextPlaceholder = Color(0xFFA0A8B5)

/** Decorative icons and chevrons that carry no text weight. */
val MediQIconMuted = Color(0xFF87918C)

// ── Borders and dividers ────────────────────────────────────────────────────

/** Default 1dp outline for cards, chips and inputs. Consolidated from 7 near-identical greys. */
val MediQBorder = Color(0xFFDCE3DE)

/** Low-emphasis divider inside a card or list. */
val MediQDivider = Color(0xFFE5E8E5)

/** Divider on a white card, slightly lighter than [MediQDivider]. */
val MediQDividerSubtle = Color(0xFFE7EAE8)

// ── Status ──────────────────────────────────────────────────────────────────

/** Error text, error icons, destructive actions. */
val MediQError = Color(0xFFB42318)

/** Error container for banners and failed rows. */
val MediQErrorContainer = Color(0xFFFFF7F6)

/** Tinted error chip on a white card; stronger than [MediQErrorContainer] so a bad status stays legible. */
val MediQErrorChip = Color(0xFFFFE9E7)

/** Error outline and separator. */
val MediQErrorBorder = Color(0xFFF2B8B5)

/** Warning text and warning chip foreground. */
val MediQWarning = Color(0xFFB76E00)

/** Warning container for notices. */
val MediQWarningContainer = Color(0xFFFFF4E5)

/** Reminder notification icon background (amber 50). */
val MediQReminderIconContainer = Color(0xFFFFF6E6)

/** Reminder notification icon tint (amber 600). */
val MediQReminderIconTint = Color(0xFFF59E0B)

/** Confirmed and success foreground. */
val MediQSuccess = Color(0xFF388E3C)

/** Success container. Consolidated from EAF6ED / E8F4EB / E7F3EA / E2F2E7. */
val MediQSuccessContainer = Color(0xFFEAF6ED)

/** Neutral status foreground: completed, cancelled, inactive. */
val MediQNeutral = Color(0xFFA0A8B5)

// Availability slots

/** Outline of an available, unselected time slot in the slot grid. */
val MediQSlotAvailableBorder = Color(0xFFB5D9BF)

/** Container of a primary button that cannot be pressed yet. */
val MediQDisabledContainer = Color(0xFF8CBA99)

// ── Components ──────────────────────────────────────────────────────────────

/** Initials-avatar tile, brand tinted. */
val MediQAvatarBrandTile = Color(0xFFE7EEE9)

/** Initials-avatar tile, alert tinted. */
val MediQAvatarAlertTile = Color(0xFFFBE5E5)

/** Unread indicator dot. */
val MediQNotificationDot = Color(0xFFE53935)
