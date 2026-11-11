package com.invoicebuilder.core.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.invoicebuilder.core.calculator.InvoiceCalculator
import com.invoicebuilder.core.calculator.ItemCalculationInput
import com.invoicebuilder.core.model.Business
import com.invoicebuilder.core.model.Client
import com.invoicebuilder.core.model.Invoice
import java.io.ByteArrayOutputStream
import java.util.Locale

data class PdfInvoiceData(
    val invoice: Invoice,
    val business: Business,
    val client: Client,
    val currencySymbol: String,
    val items: List<ItemCalculationInput>,
    val itemNames: List<String>
)

object PdfGenerator {

    /**
     * Renders a clean, high-resolution A4 invoice PDF using Android's native PdfDocument engine.
     * Page dimensions: A4 at 72 DPI (595 x 842 points).
     */
    fun generatePdf(data: PdfInvoiceData): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryColor = Color.parseColor("#006400")
        val textColor = Color.parseColor("#0F172A")
        val textSecondaryColor = Color.parseColor("#64748B")
        val dividerColor = Color.parseColor("#E2E8F0")

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Title
        paint.color = primaryColor
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.invoice.invoiceType.name.uppercase(), 40f, 60f, paint)

        // Invoice Number & Dates (Top Right)
        paint.color = textColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val fullNumber = data.invoice.fullInvoiceNumber
        canvas.drawText("Invoice #: $fullNumber", 400f, 48f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textSecondaryColor
        canvas.drawText("Issue Date: ${data.invoice.issuedAt.take(10)}", 400f, 62f, paint)
        data.invoice.dueDate?.let {
            canvas.drawText("Due Date: ${it.take(10)}", 400f, 76f, paint)
        }

        // Top Divider
        paint.color = dividerColor
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 95f, 555f, 95f, paint)

        // Business Info (From)
        paint.color = textSecondaryColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FROM:", 40f, 115f, paint)

        paint.color = textColor
        paint.textSize = 12f
        canvas.drawText(data.business.name, 40f, 130f, paint)

        paint.color = textSecondaryColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var yPos = 145f
        data.business.address?.let {
            canvas.drawText(it, 40f, yPos, paint)
            yPos += 14f
        }
        data.business.email?.let {
            canvas.drawText(it, 40f, yPos, paint)
            yPos += 14f
        }
        data.business.vatCode?.let {
            canvas.drawText("VAT: $it", 40f, yPos, paint)
        }

        // Client Info (Bill To)
        paint.color = textSecondaryColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO:", 300f, 115f, paint)

        paint.color = textColor
        paint.textSize = 12f
        canvas.drawText(data.client.name, 300f, 130f, paint)

        paint.color = textSecondaryColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var clientY = 145f
        data.client.address?.let {
            canvas.drawText(it, 300f, clientY, paint)
            clientY += 14f
        }
        data.client.email?.let {
            canvas.drawText(it, 300f, clientY, paint)
            clientY += 14f
        }
        data.client.vatCode?.let {
            canvas.drawText("VAT: $it", 300f, clientY, paint)
        }

        // Table Header
        val tableTop = 210f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(40f, tableTop, 555f, tableTop + 24f, paint)

        paint.color = textColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEM DESCRIPTION", 50f, tableTop + 16f, paint)
        canvas.drawText("QTY", 320f, tableTop + 16f, paint)
        canvas.drawText("UNIT PRICE", 400f, tableTop + 16f, paint)
        canvas.drawText("TOTAL", 490f, tableTop + 16f, paint)

        // Line Items
        var currentY = tableTop + 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val calculated = InvoiceCalculator.calculateInvoiceTotals(
            items = data.items,
            discountType = data.invoice.discountType,
            discountAmountCents = data.invoice.discountAmountCents.toDoubleOrNull() ?: 0.0,
            discountPercent = data.invoice.discountPercent,
            surchargeType = data.invoice.surchargeType,
            surchargeAmountCents = data.invoice.surchargeAmountCents.toDoubleOrNull() ?: 0.0,
            surchargePercent = data.invoice.surchargePercent,
            shippingFeeCents = data.invoice.shippingFeeCents.toDoubleOrNull() ?: 0.0,
            taxRate = data.invoice.taxRate,
            taxType = data.invoice.taxType
        )

        data.items.forEachIndexed { index, item ->
            val itemName = data.itemNames.getOrElse(index) { "Item ${index + 1}" }
            val lineTotal = (item.unitPriceCents * item.quantity) / 100.0

            paint.color = textColor
            canvas.drawText(itemName, 50f, currentY, paint)
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), 320f, currentY, paint)
            canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, item.unitPriceCents / 100.0), 400f, currentY, paint)
            canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, lineTotal), 490f, currentY, paint)

            paint.color = dividerColor
            canvas.drawLine(40f, currentY + 10f, 555f, currentY + 10f, paint)
            currentY += 25f
        }

        // Totals Section (Bottom Right)
        val totalsX = 350f
        var totalsY = currentY + 20f

        paint.color = textSecondaryColor
        canvas.drawText("Subtotal:", totalsX, totalsY, paint)
        paint.color = textColor
        canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, calculated.subtotalNet / 100.0), 480f, totalsY, paint)

        if (calculated.discountTotal > 0) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Discount:", totalsX, totalsY, paint)
            paint.color = Color.parseColor("#16A34A")
            canvas.drawText(String.format(Locale.US, "-%s%.2f", data.currencySymbol, calculated.discountTotal / 100.0), 480f, totalsY, paint)
        }

        if (calculated.taxTotal > 0) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Tax (${data.invoice.taxRate}%):", totalsX, totalsY, paint)
            paint.color = textColor
            canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, calculated.taxTotal / 100.0), 480f, totalsY, paint)
        }

        if (calculated.shippingTotal > 0) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Shipping:", totalsX, totalsY, paint)
            paint.color = textColor
            canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, calculated.shippingTotal / 100.0), 480f, totalsY, paint)
        }

        // Total Payable
        totalsY += 24f
        paint.color = dividerColor
        canvas.drawLine(totalsX, totalsY - 10f, 555f, totalsY - 10f, paint)

        paint.color = primaryColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Payable:", totalsX, totalsY + 4f, paint)
        canvas.drawText(String.format(Locale.US, "%s%.2f", data.currencySymbol, calculated.payableTotal / 100.0), 480f, totalsY + 4f, paint)

        // Notes (Bottom Left)
        data.invoice.customerNotes?.let { notes ->
            val notesY = currentY + 30f
            paint.color = textSecondaryColor
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("NOTES:", 40f, notesY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(notes, 40f, notesY + 15f, paint)
        }

        // Footer
        paint.color = textSecondaryColor
        paint.textSize = 8f
        canvas.drawText("Thank you for your business!", 40f, 800f, paint)

        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()

        return out.toByteArray()
    }
}
