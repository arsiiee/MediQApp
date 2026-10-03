package com.example.mediq.domain.model

/**
 * The specialties a consultation can be booked under.
 *
 * The wire value is the lowercase form, e.g. "internal_medicine", so the server
 * and app agree on one spelling. Sending the enum name would leak into the
 * contract and break if the constant is ever renamed.
 */
enum class Specialty(val wireValue: String, val displayName: String) {
    INTERNAL_MEDICINE("internal_medicine", "Internal Medicine"),
    PEDIATRICS("pediatrics", "Pediatrics"),
    CARDIOLOGY("cardiology", "Cardiology"),
    DERMATOLOGY("dermatology", "Dermatology"),
    OB_GYNECOLOGY("ob_gynecology", "Ob-Gynecology"),
    ORTHOPEDICS("orthopedics", "Orthopedics"),
}