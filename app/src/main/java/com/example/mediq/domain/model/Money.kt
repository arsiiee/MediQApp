package com.example.mediq.domain.model

import kotlin.jvm.JvmInline

/**
 * An amount of money in the smallest unit of the Philippine peso, which is the
 * centavos. 700 pesos is stored as `Money(70000)`.
 *
 * Storing minor units avoids the rounding problems that come with floating
 * point, and keeps the server from having to send an already-formatted
 * string like "₱700.00" that only makes sense for display.
 */
@JvmInline
value class Money(val amountInCentavos: Long) : Comparable<Money> {

    val pesos: Long get() = amountInCentavos / 100

    val centavos: Long get() = amountInCentavos % 100

    operator fun plus(other: Money) = Money(amountInCentavos + other.amountInCentavos)

    operator fun minus(other: Money) = Money(amountInCentavos - other.amountInCentavos)

    override fun compareTo(other: Money): Int =
        amountInCentavos.compareTo(other.amountInCentavos)

    companion object {
        fun pesos(amount: Long) = Money(amount * 100)
    }
}