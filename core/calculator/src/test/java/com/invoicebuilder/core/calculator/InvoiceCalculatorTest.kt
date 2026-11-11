package com.invoicebuilder.core.calculator

import com.google.common.truth.Truth.assertThat
import com.invoicebuilder.core.model.DiscountType
import com.invoicebuilder.core.model.TaxType
import org.junit.jupiter.api.Test

class InvoiceCalculatorTest {

    @Test
    fun `calculates basic invoice without discount and tax`() {
        val items = listOf(
            ItemCalculationInput(unitPriceCents = 1000.0, quantity = 2.0),
            ItemCalculationInput(unitPriceCents = 2500.0, quantity = 1.0)
        )

        val result = InvoiceCalculator.calculateInvoiceTotals(
            items = items,
            discountType = null,
            discountAmountCents = 0.0,
            discountPercent = 0.0,
            surchargeType = null,
            surchargeAmountCents = 0.0,
            surchargePercent = 0.0,
            shippingFeeCents = 0.0,
            taxRate = 0.0,
            taxType = null
        )

        assertThat(result.subtotalNet).isEqualTo(4500.0)
        assertThat(result.taxTotal).isEqualTo(0.0)
        assertThat(result.payableTotal).isEqualTo(4500.0)
        assertThat(result.lines).hasSize(2)
        assertThat(result.lines[0].grossAmount).isEqualTo(2000.0)
        assertThat(result.lines[1].grossAmount).isEqualTo(2500.0)
    }

    @Test
    fun `calculates exclusive tax correctly`() {
        val items = listOf(
            ItemCalculationInput(unitPriceCents = 10000.0, quantity = 1.0)
        )

        val result = InvoiceCalculator.calculateInvoiceTotals(
            items = items,
            discountType = null,
            discountAmountCents = 0.0,
            discountPercent = 0.0,
            surchargeType = null,
            surchargeAmountCents = 0.0,
            surchargePercent = 0.0,
            shippingFeeCents = 0.0,
            taxRate = 20.0,
            taxType = TaxType.exclusive
        )

        assertThat(result.subtotalNet).isEqualTo(10000.0)
        assertThat(result.taxTotal).isEqualTo(2000.0)
        assertThat(result.payableTotal).isEqualTo(12000.0)
        assertThat(result.vatGroups).hasSize(1)
        assertThat(result.vatGroups[0].tax).isEqualTo(2000.0)
    }

    @Test
    fun `calculates inclusive tax correctly`() {
        val items = listOf(
            ItemCalculationInput(unitPriceCents = 12000.0, quantity = 1.0)
        )

        val result = InvoiceCalculator.calculateInvoiceTotals(
            items = items,
            discountType = null,
            discountAmountCents = 0.0,
            discountPercent = 0.0,
            surchargeType = null,
            surchargeAmountCents = 0.0,
            surchargePercent = 0.0,
            shippingFeeCents = 0.0,
            taxRate = 20.0,
            taxType = TaxType.inclusive
        )

        assertThat(result.subtotalNet).isEqualTo(10000.0)
        assertThat(result.taxTotal).isEqualTo(2000.0)
        assertThat(result.payableTotal).isEqualTo(12000.0)
    }

    @Test
    fun `calculates fixed discount correctly across multiple lines`() {
        val items = listOf(
            ItemCalculationInput(unitPriceCents = 4000.0, quantity = 1.0),
            ItemCalculationInput(unitPriceCents = 6000.0, quantity = 1.0)
        )

        val result = InvoiceCalculator.calculateInvoiceTotals(
            items = items,
            discountType = DiscountType.fixed,
            discountAmountCents = 1000.0,
            discountPercent = 0.0,
            surchargeType = null,
            surchargeAmountCents = 0.0,
            surchargePercent = 0.0,
            shippingFeeCents = 500.0,
            taxRate = 0.0,
            taxType = null
        )

        assertThat(result.discountTotal).isEqualTo(1000.0)
        assertThat(result.subtotalNet).isEqualTo(9000.0)
        assertThat(result.shippingTotal).isEqualTo(500.0)
        assertThat(result.payableTotal).isEqualTo(9500.0)
        // Check proportional discount splitting: 40% of 1000 = 400, 60% of 1000 = 600
        assertThat(result.lines[0].discountAmount).isEqualTo(400.0)
        assertThat(result.lines[1].discountAmount).isEqualTo(600.0)
    }
}
