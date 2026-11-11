package com.invoicebuilder.core.model

import kotlin.math.roundToLong

/**
 * Money in the smallest currency unit (paise for INR).
 *
 * All arithmetic is performed on integral paise so that tax totals never
 * accumulate floating point drift.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(paise + other.paise)

    operator fun minus(other: Money): Money = Money(paise - other.paise)

    operator fun times(factor: Double): Money = Money((paise * factor).roundToLong())

    /** Applies a percentage rate (e.g. 18.0 -> 18%). */
    fun percent(rate: Double): Money = Money((paise * rate / 100.0).roundToLong())

    fun isZero(): Boolean = paise == 0L

    fun isPositive(): Boolean = paise > 0L

    /** Formats without a currency symbol, using Indian digit grouping. */
    fun formatNumber(decimals: Int = 2): String = CurrencyFormat.formatPlain(paise / 100.0, decimals)

    /** Formats with the given currency symbol, using Indian digit grouping. */
    fun format(symbol: String = CurrencyFormat.SYMBOL, decimals: Int = 2): String =
        symbol + formatNumber(decimals)

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    override fun toString(): String = format()

    companion object {
        val ZERO: Money = Money(0L)

        /** Builds money from a major-unit amount (rupees). */
        fun ofRupees(rupees: Double): Money = Money((rupees * 100.0).roundToLong())

        /** Builds money from legacy minor-unit values stored as text or doubles. */
        fun ofCents(cents: Double): Money = Money(cents.roundToLong())

        /** Builds money from legacy minor-unit values stored as text. */
        fun ofCents(cents: String?): Money =
            Money(cents?.trim()?.toDoubleOrNull()?.roundToLong() ?: 0L)
    }
}
