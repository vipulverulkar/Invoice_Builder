package com.invoicebuilder.core.common

import com.invoicebuilder.core.model.CurrencyFormat
import com.invoicebuilder.core.model.Business
import com.invoicebuilder.core.model.Invoice
import java.io.ByteArrayOutputStream
import java.util.Locale

data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val unitPriceCents: Double
)

object EscPosBuilder {

    private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val GS_FEED_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Feed and cut paper

    fun buildReceiptBytes(
        business: Business,
        invoice: Invoice,
        items: List<ReceiptItem>,
        totalPayableCents: Double,
        currencySymbol: String = CurrencyFormat.SYMBOL
    ): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(ESC_INIT)

        // Header: Business Name centered & bold
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write("${business.name}\n".toByteArray(Charsets.UTF_8))
        out.write(ESC_BOLD_OFF)

        business.address?.let {
            out.write("$it\n".toByteArray(Charsets.UTF_8))
        }
        business.phone?.let {
            out.write("Tel: $it\n".toByteArray(Charsets.UTF_8))
        }

        out.write("--------------------------------\n".toByteArray(Charsets.UTF_8))

        // Invoice Meta
        out.write(ESC_ALIGN_LEFT)
        out.write("Invoice: ${invoice.fullInvoiceNumber}\n".toByteArray(Charsets.UTF_8))
        out.write("Date: ${invoice.issuedAt.take(10)}\n".toByteArray(Charsets.UTF_8))
        out.write("--------------------------------\n".toByteArray(Charsets.UTF_8))

        // Items Line by Line
        items.forEach { item ->
            val lineTotal = (item.quantity * item.unitPriceCents) / 100.0
            val lineStr = String.format(Locale.US, "%-16s %3.0f x %5.2f\n", item.name.take(16), item.quantity, item.unitPriceCents / 100.0)
            out.write(lineStr.toByteArray(Charsets.UTF_8))
            val totalStr = String.format(Locale.US, "%32s\n", String.format(Locale.US, "%s%.2f", currencySymbol, lineTotal))
            out.write(totalStr.toByteArray(Charsets.UTF_8))
        }

        out.write("--------------------------------\n".toByteArray(Charsets.UTF_8))

        // Total
        out.write(ESC_BOLD_ON)
        out.write(ESC_ALIGN_RIGHT)
        val grandTotal = String.format(Locale.US, "TOTAL: %s%.2f\n", currencySymbol, totalPayableCents / 100.0)
        out.write(grandTotal.toByteArray(Charsets.UTF_8))
        out.write(ESC_BOLD_OFF)

        out.write("--------------------------------\n".toByteArray(Charsets.UTF_8))
        out.write(ESC_ALIGN_CENTER)
        out.write("Thank you for your visit!\n\n\n".toByteArray(Charsets.UTF_8))

        out.write(GS_FEED_CUT)

        return out.toByteArray()
    }
}
