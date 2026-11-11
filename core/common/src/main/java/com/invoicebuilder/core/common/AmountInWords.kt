package com.invoicebuilder.core.common

import com.invoicebuilder.core.model.Money

/**
 * Converts an amount into words using the Indian numbering system
 * (crore / lakh / thousand), as required on Indian tax invoices.
 *
 * Example: `123456.78` -> "Rupees One Lakh Twenty Three Thousand Four Hundred
 * Fifty Six and Seventy Eight Paise Only".
 */
object AmountInWords {

    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    /** Formats [money] as a full invoice phrase. */
    fun format(money: Money): String {
        val negative = money.paise < 0
        val absPaise = if (negative) -money.paise else money.paise
        val rupees = absPaise / 100
        val paise = (absPaise % 100).toInt()

        val builder = StringBuilder()
        if (negative) builder.append("Minus ")
        builder.append("Rupees ")
        builder.append(convert(rupees))
        builder.append(" and ")
        builder.append(convert(paise.toLong()))
        builder.append(" Paise Only")
        return builder.toString()
    }

    /** Formats a plain rupee amount (no currency unit suffix). */
    fun convert(value: Long): String {
        if (value == 0L) return "Zero"

        val parts = mutableListOf<String>()
        var remaining = value

        val crore = remaining / 10_000_000L
        remaining %= 10_000_000L
        if (crore > 0) parts += "${convertUnderThousand(crore)} Crore"

        val lakh = remaining / 100_000L
        remaining %= 100_000L
        if (lakh > 0) parts += "${convertUnderThousand(lakh)} Lakh"

        val thousand = remaining / 1_000L
        remaining %= 1_000L
        if (thousand > 0) parts += "${convertUnderThousand(thousand)} Thousand"

        if (remaining > 0) parts += convertUnderThousand(remaining)

        return parts.joinToString(" ")
    }

    private fun convertUnderThousand(value: Long): String {
        if (value == 0L) return "Zero"
        val parts = mutableListOf<String>()
        val hundred = value / 100L
        val rest = value % 100L
        if (hundred > 0) parts += "${ones[hundred.toInt()]} Hundred"
        if (rest > 0) parts += convertUnderHundred(rest)
        return parts.joinToString(" ")
    }

    private fun convertUnderHundred(value: Long): String {
        if (value == 0L) return ""
        val tensValue = (value / 10L).toInt()
        val onesValue = (value % 10L).toInt()
        return when {
            tensValue == 1 -> ones[10 + onesValue]
            tensValue > 1 -> tens[tensValue] + if (onesValue > 0) " ${ones[onesValue]}" else ""
            else -> ones[onesValue]
        }
    }
}
