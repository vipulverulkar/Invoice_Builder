package com.invoicebuilder.core.model

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Central currency formatting for the app.
 *
 * Amounts are handled in minor units (cents/paise) wherever the database is
 * involved, matching the rest of the domain model.
 */
object CurrencyFormat {

    /** Indian Rupee sign. */
    const val SYMBOL: String = "₹"

    /** ISO 4217 code used for e-invoicing (PEPPOL / XRechnung). */
    const val CODE: String = "INR"

    /** Locale used for grouping and number formatting. */
    const val LOCALE_TAG: String = "en-IN"

    /** Formats an amount given in major units (e.g. 1234.5 -> ₹1,234.50). */
    fun formatAmount(amount: Double, decimals: Int = 2): String {
        val negative = amount < 0
        val scale = pow10(decimals)
        val minor = (abs(amount) * scale).roundToLong()
        val whole = minor / scale
        val fraction = minor % scale

        val builder = StringBuilder()
        if (negative) builder.append('-')
        builder.append(SYMBOL)
        builder.append(groupIndian(whole.toString()))
        if (decimals > 0) {
            builder.append('.')
            builder.append(fraction.toString().padStart(decimals, '0'))
        }
        return builder.toString()
    }

    /** Formats an amount given in minor units (e.g. 123450 cents -> ₹1,234.50). */
    fun formatCents(cents: Double, decimals: Int = 2): String {
        return formatAmount(cents / 100.0, decimals)
    }

    /** Formats an amount without the currency symbol (for input fields). */
    fun formatPlain(amount: Double, decimals: Int = 2): String {
        val scale = pow10(decimals)
        val minor = (abs(amount) * scale).roundToLong()
        val whole = minor / scale
        val fraction = minor % scale
        val builder = StringBuilder()
        if (amount < 0) builder.append('-')
        builder.append(groupIndian(whole.toString()))
        if (decimals > 0) {
            builder.append('.')
            builder.append(fraction.toString().padStart(decimals, '0'))
        }
        return builder.toString()
    }

    /**
     * Applies the Indian digit grouping convention: the last three digits form
     * one group, every preceding group holds two digits (12,34,567).
     */
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val groups = mutableListOf<String>()
        var remaining = digits.dropLast(3)
        while (remaining.length > 2) {
            groups.add(remaining.takeLast(2))
            remaining = remaining.dropLast(2)
        }
        if (remaining.isNotEmpty()) groups.add(remaining)
        return (groups.reversed() + lastThree).joinToString(",")
    }

    private fun pow10(exponent: Int): Long {
        var result = 1L
        repeat(exponent.coerceAtLeast(0)) { result *= 10 }
        return result
    }
}
