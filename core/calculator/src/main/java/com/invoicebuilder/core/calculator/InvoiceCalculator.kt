package com.invoicebuilder.core.calculator

import com.invoicebuilder.core.common.AmountInWords
import com.invoicebuilder.core.model.CalculatedLine
import com.invoicebuilder.core.model.DiscountType
import com.invoicebuilder.core.model.GstInvoiceTotals
import com.invoicebuilder.core.model.GstSupplyType
import com.invoicebuilder.core.model.GstTaxGroup
import com.invoicebuilder.core.model.InvoiceItemTaxType
import com.invoicebuilder.core.model.Money
import com.invoicebuilder.core.model.TaxType
import kotlin.math.roundToLong

/** Input for a single invoice line. */
data class ItemCalculationInput(
    val unitPrice: Money,
    val quantity: Double,
    /**
     * GST rate for the line. `null` means "use the invoice level rate";
     * an explicit `0.0` marks a zero-rated / exempt line.
     */
    val taxRate: Double? = null,
    val taxType: InvoiceItemTaxType? = null,
    val itemName: String = "",
    val hsnCode: String? = null
)

/**
 * Deterministic invoice calculation engine.
 *
 * All amounts are [Money] (integral paise). GST is split into CGST + SGST for
 * intra-state supplies and IGST for inter-state supplies.
 */
object InvoiceCalculator {

    fun getInvoiceItemAmount(unitPrice: Money, quantity: Double): Money =
        Money((unitPrice.paise * quantity).roundToLong())

    fun calculateDiscount(
        subtotalNet: Money,
        discountType: DiscountType?,
        discountAmount: Money,
        discountPercent: Double
    ): Money {
        if (discountType == null) return Money.ZERO
        return when (discountType) {
            DiscountType.fixed -> discountAmount
            DiscountType.percentage -> subtotalNet.percent(discountPercent)
        }
    }

    fun calculateSurcharge(
        subtotalNet: Money,
        surchargeType: DiscountType?,
        surchargeAmount: Money,
        surchargePercent: Double
    ): Money {
        if (surchargeType == null) return Money.ZERO
        return when (surchargeType) {
            DiscountType.fixed -> surchargeAmount
            DiscountType.percentage -> subtotalNet.percent(surchargePercent)
        }
    }

    /** Calculates a single line, splitting the tax into the GST heads. */
    fun calculateInvoiceLine(
        item: ItemCalculationInput,
        totalDiscount: Money,
        totalGross: Money,
        invoiceTaxRate: Double,
        invoiceTaxType: TaxType?,
        supplyType: GstSupplyType = GstSupplyType.IntraState
    ): CalculatedLine {
        val quantity = item.quantity
        val grossAmount = getInvoiceItemAmount(item.unitPrice, quantity)

        val discountPortion = if (totalGross.paise > 0) {
            Money(((grossAmount.paise.toDouble() / totalGross.paise) * totalDiscount.paise).roundToLong())
        } else {
            Money.ZERO
        }

        val rate = item.taxRate ?: invoiceTaxRate

        var resolvedTaxType = invoiceTaxType
        if (resolvedTaxType == null && item.taxType != null) {
            resolvedTaxType = if (item.taxType == InvoiceItemTaxType.exclusive) {
                TaxType.exclusive
            } else {
                TaxType.inclusive
            }
        }

        val postDiscount = grossAmount - discountPortion
        val netAmount: Money
        val taxAmount: Money

        if (supplyType == GstSupplyType.ZeroRated || supplyType == GstSupplyType.Exempt) {
            return CalculatedLine(
                itemName = item.itemName,
                hsnCode = item.hsnCode,
                quantity = quantity,
                lineAmount = postDiscount,
                unitAmount = if (quantity > 0) Money((postDiscount.paise / quantity).roundToLong()) else Money.ZERO,
                taxAmount = Money.ZERO,
                taxRate = rate,
                taxCategoryId = "Z",
                grossAmount = grossAmount,
                discountAmount = discountPortion
            )
        }

        when (resolvedTaxType) {
            TaxType.exclusive -> {
                netAmount = postDiscount
                taxAmount = netAmount.percent(rate)
            }
            TaxType.inclusive -> {
                netAmount = Money((postDiscount.paise / (1.0 + rate / 100.0)).roundToLong())
                taxAmount = postDiscount - netAmount
            }
            TaxType.deducted -> {
                netAmount = postDiscount
                taxAmount = Money(0L - netAmount.percent(rate).paise)
            }
            null -> {
                netAmount = postDiscount
                taxAmount = Money.ZERO
            }
        }

        val (cgst, sgst, igst) = splitTax(netAmount, rate, supplyType)

        return CalculatedLine(
            itemName = item.itemName,
            hsnCode = item.hsnCode,
            quantity = quantity,
            lineAmount = netAmount,
            unitAmount = if (quantity > 0) Money((netAmount.paise / quantity).roundToLong()) else Money.ZERO,
            taxAmount = taxAmount,
            taxRate = rate,
            taxCategoryId = if (rate > 0) "S" else "Z",
            grossAmount = grossAmount,
            discountAmount = discountPortion,
            igstAmount = igst,
            cgstAmount = cgst,
            sgstAmount = sgst,
            cessAmount = Money.ZERO
        )
    }

    /**
     * Splits a tax amount into the applicable GST heads.
     *
     * Intra-state supplies are split evenly into CGST and SGST, inter-state
     * supplies are charged as IGST, and zero-rated/exempt supplies carry no tax.
     */
    fun splitTax(
        netAmount: Money,
        rate: Double,
        supplyType: GstSupplyType
    ): Triple<Money, Money, Money> {
        return when (supplyType) {
            GstSupplyType.IntraState -> {
                val half = Money((netAmount.percent(rate).paise / 2.0).roundToLong())
                Triple(half, half, Money.ZERO)
            }
            GstSupplyType.InterState -> Triple(Money.ZERO, Money.ZERO, netAmount.percent(rate))
            GstSupplyType.ZeroRated, GstSupplyType.Exempt -> Triple(Money.ZERO, Money.ZERO, Money.ZERO)
        }
    }

    /** Groups tax totals by rate and supply type. */
    fun aggregateGst(lines: List<CalculatedLine>): List<GstTaxGroup> {
        val map = linkedMapOf<Pair<Double, GstSupplyType>, GstTaxGroup>()

        for (line in lines) {
            val key = line.taxRate to supplyTypeOf(line)
            val existing = map[key] ?: GstTaxGroup(
                rate = line.taxRate,
                supplyType = key.second,
                taxable = Money.ZERO,
                cgst = Money.ZERO,
                sgst = Money.ZERO,
                igst = Money.ZERO,
                cess = Money.ZERO
            )
            map[key] = existing.copy(
                taxable = existing.taxable + line.lineAmount,
                cgst = existing.cgst + line.cgstAmount,
                sgst = existing.sgst + line.sgstAmount,
                igst = existing.igst + line.igstAmount,
                cess = existing.cess + line.cessAmount
            )
        }

        return map.values.toList()
    }

    private fun supplyTypeOf(line: CalculatedLine): GstSupplyType = when {
        line.igstAmount.isPositive() -> GstSupplyType.InterState
        line.taxRate <= 0.0 -> GstSupplyType.ZeroRated
        else -> GstSupplyType.IntraState
    }

    /**
     * Calculates the complete invoice totals.
     *
     * @param roundOffEnabled rounds the payable total to the nearest rupee and
     * reports the difference in [GstInvoiceTotals.roundOff].
     */
    fun calculateInvoiceTotals(
        items: List<ItemCalculationInput>,
        discountType: DiscountType?,
        discountAmount: Money,
        discountPercent: Double,
        surchargeType: DiscountType?,
        surchargeAmount: Money,
        surchargePercent: Double,
        shippingFee: Money,
        taxRate: Double,
        taxType: TaxType?,
        supplyType: GstSupplyType = GstSupplyType.IntraState,
        roundOffEnabled: Boolean = false
    ): GstInvoiceTotals {
        val totalGross = items.fold(Money.ZERO) { acc, item ->
            acc + getInvoiceItemAmount(item.unitPrice, item.quantity)
        }
        val discountCents = calculateDiscount(totalGross, discountType, discountAmount, discountPercent)
        val surchargeCents = calculateSurcharge(
            totalGross - discountCents,
            surchargeType,
            surchargeAmount,
            surchargePercent
        )

        val lines = items.map { line ->
            calculateInvoiceLine(line, discountCents, totalGross, taxRate, taxType, supplyType)
        }

        val subtotalNet = lines.fold(Money.ZERO) { acc, line -> acc + line.lineAmount }
        val taxTotal = lines.fold(Money.ZERO) { acc, line -> acc + line.taxAmount }
        val cessTotal = lines.fold(Money.ZERO) { acc, line -> acc + line.cessAmount }
        val groups = aggregateGst(lines)

        val payableBeforeRoundOff = subtotalNet + taxTotal + cessTotal + shippingFee + surchargeCents
        val roundOff = if (roundOffEnabled) {
            Money(roundToRupee(payableBeforeRoundOff.paise) - payableBeforeRoundOff.paise)
        } else {
            Money.ZERO
        }
        val payableTotal = payableBeforeRoundOff + roundOff

        return GstInvoiceTotals(
            lines = lines,
            subtotalNet = subtotalNet,
            discountTotal = discountCents,
            shippingTotal = shippingFee,
            surchargeTotal = surchargeCents,
            taxTotal = taxTotal,
            cessTotal = cessTotal,
            roundOff = roundOff,
            payableTotal = payableTotal,
            gstGroups = groups,
            amountInWords = AmountInWords.format(payableTotal)
        )
    }

    /** Rounds an amount in paise to the nearest rupee. */
    fun roundToRupee(paise: Long): Long {
        val remainder = paise % 100L
        return if (remainder >= 50L) paise - remainder + 100L else paise - remainder
    }
}
