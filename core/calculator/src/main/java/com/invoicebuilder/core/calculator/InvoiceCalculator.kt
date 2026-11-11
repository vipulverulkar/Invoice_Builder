package com.invoicebuilder.core.calculator

import com.invoicebuilder.core.model.*
import kotlin.math.roundToLong

data class ItemCalculationInput(
    val unitPriceCents: Double,
    val quantity: Double,
    val taxRate: Double = 0.0,
    val taxType: InvoiceItemTaxType? = null
)

object InvoiceCalculator {

    fun getInvoiceItemAmount(unitPriceCents: Double, quantity: Double): Double {
        return unitPriceCents * quantity
    }

    fun calculateDiscount(subtotalNet: Double, discountType: DiscountType?, amountCents: Double, percent: Double): Double {
        if (discountType == null) return 0.0
        return when (discountType) {
            DiscountType.fixed -> amountCents
            DiscountType.percentage -> (subtotalNet * percent) / 100.0
        }
    }

    fun calculateSurcharge(subtotalNet: Double, surchargeType: DiscountType?, amountCents: Double, percent: Double): Double {
        if (surchargeType == null) return 0.0
        return when (surchargeType) {
            DiscountType.fixed -> amountCents
            DiscountType.percentage -> (subtotalNet * percent) / 100.0
        }
    }

    fun calculateInvoiceLine(
        item: ItemCalculationInput,
        totalDiscountCents: Double,
        totalGrossCents: Double,
        invoiceTaxRate: Double,
        invoiceTaxType: TaxType?
    ): CalculatedLine {
        val quantity = item.quantity
        val grossCents = getInvoiceItemAmount(item.unitPriceCents, quantity)

        val discountPortion = if (totalGrossCents > 0) {
            ((grossCents / totalGrossCents) * totalDiscountCents).roundToLong().toDouble()
        } else {
            0.0
        }

        val rate = if (item.taxType != null) item.taxRate else invoiceTaxRate

        var resolvedTaxType = invoiceTaxType
        if (resolvedTaxType == null && item.taxType != null) {
            resolvedTaxType = if (item.taxType == InvoiceItemTaxType.exclusive) {
                TaxType.exclusive
            } else {
                TaxType.inclusive
            }
        }

        val postDiscount = grossCents - discountPortion
        val netCents: Double
        val taxCents: Double

        when (resolvedTaxType) {
            TaxType.exclusive -> {
                netCents = postDiscount
                taxCents = (netCents * rate) / 100.0
            }
            TaxType.inclusive -> {
                netCents = postDiscount / (1.0 + rate / 100.0)
                taxCents = postDiscount - netCents
            }
            TaxType.deducted -> {
                netCents = postDiscount
                taxCents = -(netCents * rate) / 100.0
            }
            null -> {
                netCents = postDiscount
                taxCents = 0.0
            }
        }

        return CalculatedLine(
            lineAmount = netCents,
            unitAmount = if (quantity > 0) netCents / quantity else 0.0,
            taxAmount = taxCents,
            taxRate = rate,
            taxCategoryId = if (rate > 0) "S" else "Z",
            grossAmount = grossCents,
            discountAmount = discountPortion
        )
    }

    fun aggregateVat(lines: List<CalculatedLine>): List<VatGroup> {
        val map = mutableMapOf<Double, Pair<Double, Double>>()

        for (line in lines) {
            val existing = map[line.taxRate] ?: Pair(0.0, 0.0)
            map[line.taxRate] = Pair(
                existing.first + line.lineAmount,
                existing.second + line.taxAmount
            )
        }

        return map.entries.map { (rate, data) ->
            VatGroup(
                rate = rate,
                taxable = data.first,
                tax = data.second
            )
        }
    }

    fun calculateInvoiceTotals(
        items: List<ItemCalculationInput>,
        discountType: DiscountType?,
        discountAmountCents: Double,
        discountPercent: Double,
        surchargeType: DiscountType?,
        surchargeAmountCents: Double,
        surchargePercent: Double,
        shippingFeeCents: Double,
        taxRate: Double,
        taxType: TaxType?
    ): CalculatedInvoice {
        val totalGrossCents = items.sumOf { getInvoiceItemAmount(it.unitPriceCents, it.quantity) }
        val discountCents = calculateDiscount(totalGrossCents, discountType, discountAmountCents, discountPercent)
        val surchargeCents = calculateSurcharge(totalGrossCents - discountCents, surchargeType, surchargeAmountCents, surchargePercent)

        val lines = items.map { line ->
            calculateInvoiceLine(line, discountCents, totalGrossCents, taxRate, taxType)
        }

        val subtotalNet = lines.sumOf { it.lineAmount }
        val taxTotal = lines.sumOf { it.taxAmount }
        val vatGroups = aggregateVat(lines)
        val payableTotal = subtotalNet + taxTotal + shippingFeeCents + surchargeCents

        return CalculatedInvoice(
            lines = lines,
            subtotalNet = subtotalNet,
            discountTotal = discountCents,
            shippingTotal = shippingFeeCents,
            surchargeTotal = surchargeCents,
            taxTotal = taxTotal,
            payableTotal = payableTotal,
            vatGroups = vatGroups
        )
    }
}
