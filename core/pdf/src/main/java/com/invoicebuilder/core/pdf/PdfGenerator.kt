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
import com.invoicebuilder.core.model.CurrencyFormat
import com.invoicebuilder.core.model.GstSupplyType
import com.invoicebuilder.core.model.Invoice
import java.io.ByteArrayOutputStream

data class PdfInvoiceData(
    val invoice: Invoice,
    val business: Business,
    val client: Client,
    val currencySymbol: String,
    val items: List<ItemCalculationInput>,
    val itemNames: List<String>,
    val supplyType: GstSupplyType = GstSupplyType.IntraState
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

        val primaryColor = Color.parseColor("#4F46E5")
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
        data.business.gstin?.let {
            canvas.drawText("GSTIN: $it", 40f, yPos, paint)
            yPos += 14f
        }
        data.business.stateCode?.let {
            canvas.drawText("State Code: $it", 40f, yPos, paint)
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
        data.client.gstin?.let {
            canvas.drawText("GSTIN: $it", 300f, clientY, paint)
            clientY += 14f
        }
        data.invoice.placeOfSupply?.let {
            canvas.drawText("Place of Supply: $it", 300f, clientY, paint)
        }

        // Table Header
        val tableTop = 225f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(40f, tableTop, 555f, tableTop + 24f, paint)

        paint.color = textColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEM DESCRIPTION", 50f, tableTop + 16f, paint)
        canvas.drawText("HSN", 260f, tableTop + 16f, paint)
        canvas.drawText("QTY", 320f, tableTop + 16f, paint)
        canvas.drawText("GST %", 360f, tableTop + 16f, paint)
        canvas.drawText("UNIT PRICE", 400f, tableTop + 16f, paint)
        canvas.drawText("TOTAL", 490f, tableTop + 16f, paint)

        // Line Items
        var currentY = tableTop + 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val calculated = InvoiceCalculator.calculateInvoiceTotals(
            items = data.items,
            discountType = data.invoice.discountType,
            discountAmount = com.invoicebuilder.core.model.Money.ofCents(data.invoice.discountAmountCents),
            discountPercent = data.invoice.discountPercent,
            surchargeType = data.invoice.surchargeType,
            surchargeAmount = com.invoicebuilder.core.model.Money.ofCents(data.invoice.surchargeAmountCents),
            surchargePercent = data.invoice.surchargePercent,
            shippingFee = com.invoicebuilder.core.model.Money.ofCents(data.invoice.shippingFeeCents),
            taxRate = data.invoice.taxRate,
            taxType = data.invoice.taxType,
            supplyType = data.supplyType
        )

        data.items.forEachIndexed { index, item ->
            val itemName = data.itemNames.getOrElse(index) { item.itemName.ifEmpty { "Item ${index + 1}" } }
            val lineTotal = InvoiceCalculator.getInvoiceItemAmount(item.unitPrice, item.quantity)
            val line = calculated.lines.getOrNull(index)

            paint.color = textColor
            canvas.drawText(itemName.take(40), 50f, currentY, paint)
            canvas.drawText(item.hsnCode.orEmpty(), 260f, currentY, paint)
            canvas.drawText(String.format(java.util.Locale.US, "%.1f", item.quantity), 320f, currentY, paint)
            canvas.drawText(
                String.format(java.util.Locale.US, "%.0f%%", line?.taxRate ?: 0.0),
                360f,
                currentY,
                paint
            )
            canvas.drawText(item.unitPrice.format(data.currencySymbol), 400f, currentY, paint)
            canvas.drawText(lineTotal.format(data.currencySymbol), 490f, currentY, paint)

            paint.color = dividerColor
            canvas.drawLine(40f, currentY + 10f, 555f, currentY + 10f, paint)
            currentY += 25f
        }

        // GST breakup table
        currentY += 8f
        paint.color = textColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GST BREAKUP", 40f, currentY, paint)
        currentY += 14f

        paint.color = textSecondaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Rate", 50f, currentY, paint)
        canvas.drawText("Taxable", 150f, currentY, paint)
        canvas.drawText("CGST", 280f, currentY, paint)
        canvas.drawText("SGST", 360f, currentY, paint)
        canvas.drawText("IGST", 440f, currentY, paint)
        currentY += 14f

        calculated.gstGroups.forEach { group ->
            paint.color = textColor
            canvas.drawText(String.format(java.util.Locale.US, "%.0f%%", group.rate), 50f, currentY, paint)
            canvas.drawText(group.taxable.format(data.currencySymbol), 150f, currentY, paint)
            canvas.drawText(group.cgst.format(data.currencySymbol), 280f, currentY, paint)
            canvas.drawText(group.sgst.format(data.currencySymbol), 360f, currentY, paint)
            canvas.drawText(group.igst.format(data.currencySymbol), 440f, currentY, paint)
            currentY += 14f
        }

        // Totals Section (Bottom Right)
        val totalsX = 350f
        var totalsY = maxOf(currentY + 20f, 320f)

        paint.color = textSecondaryColor
        canvas.drawText("Taxable Value:", totalsX, totalsY, paint)
        paint.color = textColor
        canvas.drawText(calculated.subtotalNet.format(data.currencySymbol), 480f, totalsY, paint)

        if (calculated.discountTotal.isPositive()) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Discount:", totalsX, totalsY, paint)
            paint.color = Color.parseColor("#16A34A")
            canvas.drawText("-" + calculated.discountTotal.format(data.currencySymbol), 480f, totalsY, paint)
        }

        if (calculated.taxTotal.isPositive()) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("GST:", totalsX, totalsY, paint)
            paint.color = textColor
            canvas.drawText(calculated.taxTotal.format(data.currencySymbol), 480f, totalsY, paint)
        }

        if (calculated.shippingTotal.isPositive()) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Shipping:", totalsX, totalsY, paint)
            paint.color = textColor
            canvas.drawText(calculated.shippingTotal.format(data.currencySymbol), 480f, totalsY, paint)
        }

        if (calculated.roundOff.isPositive() || calculated.roundOff.paise < 0) {
            totalsY += 18f
            paint.color = textSecondaryColor
            canvas.drawText("Round Off:", totalsX, totalsY, paint)
            paint.color = textColor
            canvas.drawText(calculated.roundOff.format(data.currencySymbol), 480f, totalsY, paint)
        }

        // Total Payable
        totalsY += 24f
        paint.color = dividerColor
        canvas.drawLine(totalsX, totalsY - 10f, 555f, totalsY - 10f, paint)

        paint.color = primaryColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Payable:", totalsX, totalsY + 4f, paint)
        canvas.drawText(calculated.payableTotal.format(data.currencySymbol), 480f, totalsY + 4f, paint)

        // Amount in words
        paint.color = textSecondaryColor
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AMOUNT IN WORDS:", 40f, currentY + 26f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(calculated.amountInWords.take(70), 40f, currentY + 40f, paint)

        // Notes (Bottom Left)
        data.invoice.customerNotes?.let { notes ->
            val notesY = currentY + 62f
            paint.color = textSecondaryColor
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("NOTES:", 40f, notesY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(notes.take(70), 40f, notesY + 15f, paint)
        }

        // Footer
        paint.color = textSecondaryColor
        paint.textSize = 8f
        canvas.drawText("Thank you for your business!", 40f, 800f, paint)
        data.business.gstin?.let {
            canvas.drawText("GSTIN: $it", 400f, 800f, paint)
        }

        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()

        return out.toByteArray()
    }
}
