package com.invoicebuilder.core.calculator

import com.google.common.truth.Truth.assertThat
import com.invoicebuilder.core.model.DiscountType
import com.invoicebuilder.core.model.GstSupplyType
import com.invoicebuilder.core.model.Money
import com.invoicebuilder.core.model.TaxType
import org.junit.jupiter.api.Test

class InvoiceCalculatorTest {

    private fun calculate(
        items: List<ItemCalculationInput>,
        discountType: DiscountType? = null,
        discountAmount: Money = Money.ZERO,
        discountPercent: Double = 0.0,
        surchargeType: DiscountType? = null,
        surchargeAmount: Money = Money.ZERO,
        surchargePercent: Double = 0.0,
        shippingFee: Money = Money.ZERO,
        taxRate: Double = 0.0,
        taxType: TaxType? = null,
        supplyType: GstSupplyType = GstSupplyType.IntraState,
        roundOffEnabled: Boolean = false
    ) = InvoiceCalculator.calculateInvoiceTotals(
        items = items,
        discountType = discountType,
        discountAmount = discountAmount,
        discountPercent = discountPercent,
        surchargeType = surchargeType,
        surchargeAmount = surchargeAmount,
        surchargePercent = surchargePercent,
        shippingFee = shippingFee,
        taxRate = taxRate,
        taxType = taxType,
        supplyType = supplyType,
        roundOffEnabled = roundOffEnabled
    )

    @Test
    fun `calculates basic invoice without discount and tax`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(1000L), quantity = 2.0),
            ItemCalculationInput(unitPrice = Money(2500L), quantity = 1.0)
        )

        val result = calculate(items)

        assertThat(result.subtotalNet).isEqualTo(Money(4500L))
        assertThat(result.taxTotal).isEqualTo(Money.ZERO)
        assertThat(result.payableTotal).isEqualTo(Money(4500L))
        assertThat(result.lines).hasSize(2)
        assertThat(result.lines[0].grossAmount).isEqualTo(Money(2000L))
        assertThat(result.lines[1].grossAmount).isEqualTo(Money(2500L))
    }

    @Test
    fun `splits exclusive GST into CGST and SGST for intra state supply`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(10000L), quantity = 1.0)
        )

        val result = calculate(items, taxRate = 18.0, taxType = TaxType.exclusive)

        assertThat(result.subtotalNet).isEqualTo(Money(10000L))
        assertThat(result.taxTotal).isEqualTo(Money(1800L))
        assertThat(result.payableTotal).isEqualTo(Money(11800L))
        assertThat(result.gstGroups).hasSize(1)
        assertThat(result.gstGroups[0].cgst).isEqualTo(Money(900L))
        assertThat(result.gstGroups[0].sgst).isEqualTo(Money(900L))
        assertThat(result.gstGroups[0].igst).isEqualTo(Money.ZERO)
    }

    @Test
    fun `charges full GST as IGST for inter state supply`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(10000L), quantity = 1.0)
        )

        val result = calculate(
            items = items,
            taxRate = 18.0,
            taxType = TaxType.exclusive,
            supplyType = GstSupplyType.InterState
        )

        assertThat(result.gstGroups).hasSize(1)
        assertThat(result.gstGroups[0].igst).isEqualTo(Money(1800L))
        assertThat(result.gstGroups[0].cgst).isEqualTo(Money.ZERO)
        assertThat(result.gstGroups[0].sgst).isEqualTo(Money.ZERO)
        assertThat(result.payableTotal).isEqualTo(Money(11800L))
    }

    @Test
    fun `calculates inclusive tax correctly`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(12000L), quantity = 1.0)
        )

        val result = calculate(items, taxRate = 20.0, taxType = TaxType.inclusive)

        assertThat(result.subtotalNet).isEqualTo(Money(10000L))
        assertThat(result.taxTotal).isEqualTo(Money(2000L))
        assertThat(result.payableTotal).isEqualTo(Money(12000L))
    }

    @Test
    fun `calculates fixed discount correctly across multiple lines`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(4000L), quantity = 1.0),
            ItemCalculationInput(unitPrice = Money(6000L), quantity = 1.0)
        )

        val result = calculate(
            items = items,
            discountType = DiscountType.fixed,
            discountAmount = Money(1000L),
            shippingFee = Money(500L)
        )

        assertThat(result.discountTotal).isEqualTo(Money(1000L))
        assertThat(result.subtotalNet).isEqualTo(Money(9000L))
        assertThat(result.shippingTotal).isEqualTo(Money(500L))
        assertThat(result.payableTotal).isEqualTo(Money(9500L))
        // Proportional discount splitting: 40% of 1000 = 400, 60% of 1000 = 600
        assertThat(result.lines[0].discountAmount).isEqualTo(Money(400L))
        assertThat(result.lines[1].discountAmount).isEqualTo(Money(600L))
    }

    @Test
    fun `groups tax by rate for mixed rate invoices`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(10000L), quantity = 1.0, taxRate = 5.0),
            ItemCalculationInput(unitPrice = Money(10000L), quantity = 1.0, taxRate = 18.0)
        )

        val result = calculate(items, taxRate = 18.0, taxType = TaxType.exclusive)

        assertThat(result.gstGroups).hasSize(2)
        assertThat(result.gstGroups.map { it.rate }).containsExactly(5.0, 18.0)
        assertThat(result.taxTotal).isEqualTo(Money(2300L))
        assertThat(result.payableTotal).isEqualTo(Money(22300L))
    }

    @Test
    fun `rounds payable total to nearest rupee when enabled`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(1049L), quantity = 1.0)
        )

        val result = calculate(items, roundOffEnabled = true)

        // Rs 10.49 rounds down to Rs 10.00
        assertThat(result.roundOff).isEqualTo(Money(-49L))
        assertThat(result.payableTotal).isEqualTo(Money(1000L))
    }

    @Test
    fun `produces amount in words using indian numbering`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(12345678L), quantity = 1.0)
        )

        val result = calculate(items)

        assertThat(result.amountInWords)
            .isEqualTo("Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six and Seventy Eight Paise Only")
    }

    @Test
    fun `does not tax zero rated supplies`() {
        val items = listOf(
            ItemCalculationInput(unitPrice = Money(10000L), quantity = 1.0)
        )

        val result = calculate(
            items = items,
            taxRate = 18.0,
            taxType = TaxType.exclusive,
            supplyType = GstSupplyType.ZeroRated
        )

        assertThat(result.taxTotal).isEqualTo(Money.ZERO)
        assertThat(result.payableTotal).isEqualTo(Money(10000L))
    }
}
