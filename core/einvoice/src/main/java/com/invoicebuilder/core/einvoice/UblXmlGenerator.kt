package com.invoicebuilder.core.einvoice

import com.invoicebuilder.core.calculator.InvoiceCalculator
import com.invoicebuilder.core.calculator.ItemCalculationInput
import com.invoicebuilder.core.model.*
import java.util.Locale

object UblXmlGenerator {

    private fun xmlEscape(str: String?): String {
        if (str == null) return ""
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatAmount(amountCents: Double): String {
        val amount = amountCents / 100.0
        return String.format(Locale.US, "%.2f", amount)
    }

    fun generateUblXml(
        invoice: Invoice,
        business: Business,
        client: Client,
        currencyCode: String,
        items: List<ItemCalculationInput>,
        eInvoiceType: EInvoice = EInvoice.UBL21
    ): String {
        val calculated = InvoiceCalculator.calculateInvoiceTotals(
            items = items,
            discountType = invoice.discountType,
            discountAmountCents = invoice.discountAmountCents.toDoubleOrNull() ?: 0.0,
            discountPercent = invoice.discountPercent,
            surchargeType = invoice.surchargeType,
            surchargeAmountCents = invoice.surchargeAmountCents.toDoubleOrNull() ?: 0.0,
            surchargePercent = invoice.surchargePercent,
            shippingFeeCents = invoice.shippingFeeCents.toDoubleOrNull() ?: 0.0,
            taxRate = invoice.taxRate,
            taxType = invoice.taxType
        )

        val customizationId = if (eInvoiceType == EInvoice.XRechnung) {
            "urn:cen.eu:en16931:2017#compliant#urn:xoev-de:kosit:standard:xrechnung_2.2"
        } else {
            "urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0"
        }

        val buyerRef = if (eInvoiceType == EInvoice.XRechnung) {
            client.buyerReference ?: "04011000-12345-12"
        } else {
            client.buyerReference ?: "NA"
        }

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"")
        sb.appendLine("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"")
        sb.appendLine("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">")

        sb.appendLine("  <cbc:CustomizationID>$customizationId</cbc:CustomizationID>")
        sb.appendLine("  <cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>")
        sb.appendLine("  <cbc:ID>${xmlEscape(invoice.fullInvoiceNumber)}</cbc:ID>")
        sb.appendLine("  <cbc:IssueDate>${xmlEscape(invoice.issuedAt.take(10))}</cbc:IssueDate>")
        invoice.dueDate?.let {
            sb.appendLine("  <cbc:DueDate>${xmlEscape(it.take(10))}</cbc:DueDate>")
        }
        sb.appendLine("  <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>")
        sb.appendLine("  <cbc:DocumentCurrencyCode>${xmlEscape(currencyCode)}</cbc:DocumentCurrencyCode>")
        sb.appendLine("  <cbc:BuyerReference>${xmlEscape(buyerRef)}</cbc:BuyerReference>")

        // AccountingSupplierParty (Business)
        sb.appendLine("  <cac:AccountingSupplierParty>")
        sb.appendLine("    <cac:Party>")
        business.peppolEndpointId?.let { endpoint ->
            val scheme = business.peppolEndpointSchemeId ?: "9930"
            sb.appendLine("      <cbc:EndpointID schemeID=\"${xmlEscape(scheme)}\">${xmlEscape(endpoint)}</cbc:EndpointID>")
        }
        sb.appendLine("      <cac:PartyName>")
        sb.appendLine("        <cbc:Name>${xmlEscape(business.name)}</cbc:Name>")
        sb.appendLine("      </cac:PartyName>")
        sb.appendLine("      <cac:PostalAddress>")
        sb.appendLine("        <cbc:StreetName>${xmlEscape(business.address ?: "")}</cbc:StreetName>")
        sb.appendLine("        <cac:Country>")
        sb.appendLine("          <cbc:IdentificationCode>${xmlEscape(business.countryCode ?: "DE")}</cbc:IdentificationCode>")
        sb.appendLine("        </cac:Country>")
        sb.appendLine("      </cac:PostalAddress>")
        business.vatCode?.let { vat ->
            sb.appendLine("      <cac:PartyTaxScheme>")
            sb.appendLine("        <cbc:CompanyID>${xmlEscape(vat)}</cbc:CompanyID>")
            sb.appendLine("        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>")
            sb.appendLine("      </cac:PartyTaxScheme>")
        }
        sb.appendLine("      <cac:PartyLegalEntity>")
        sb.appendLine("        <cbc:RegistrationName>${xmlEscape(business.name)}</cbc:RegistrationName>")
        sb.appendLine("      </cac:PartyLegalEntity>")
        sb.appendLine("    </cac:Party>")
        sb.appendLine("  </cac:AccountingSupplierParty>")

        // AccountingCustomerParty (Client)
        sb.appendLine("  <cac:AccountingCustomerParty>")
        sb.appendLine("    <cac:Party>")
        client.peppolEndpointId?.let { endpoint ->
            val scheme = client.peppolEndpointSchemeId ?: "9930"
            sb.appendLine("      <cbc:EndpointID schemeID=\"${xmlEscape(scheme)}\">${xmlEscape(endpoint)}</cbc:EndpointID>")
        }
        sb.appendLine("      <cac:PartyName>")
        sb.appendLine("        <cbc:Name>${xmlEscape(client.name)}</cbc:Name>")
        sb.appendLine("      </cac:PartyName>")
        sb.appendLine("      <cac:PostalAddress>")
        sb.appendLine("        <cbc:StreetName>${xmlEscape(client.address ?: "")}</cbc:StreetName>")
        sb.appendLine("        <cac:Country>")
        sb.appendLine("          <cbc:IdentificationCode>${xmlEscape(client.countryCode ?: "DE")}</cbc:IdentificationCode>")
        sb.appendLine("        </cac:Country>")
        sb.appendLine("      </cac:PostalAddress>")
        client.vatCode?.let { vat ->
            sb.appendLine("      <cac:PartyTaxScheme>")
            sb.appendLine("        <cbc:CompanyID>${xmlEscape(vat)}</cbc:CompanyID>")
            sb.appendLine("        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>")
            sb.appendLine("      </cac:PartyTaxScheme>")
        }
        sb.appendLine("      <cac:PartyLegalEntity>")
        sb.appendLine("        <cbc:RegistrationName>${xmlEscape(client.name)}</cbc:RegistrationName>")
        sb.appendLine("      </cac:PartyLegalEntity>")
        sb.appendLine("    </cac:Party>")
        sb.appendLine("  </cac:AccountingCustomerParty>")

        // TaxTotal
        sb.appendLine("  <cac:TaxTotal>")
        sb.appendLine("    <cbc:TaxAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(calculated.taxTotal)}</cbc:TaxAmount>")
        for (vat in calculated.vatGroups) {
            sb.appendLine("    <cac:TaxSubtotal>")
            sb.appendLine("      <cbc:TaxableAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(vat.taxable)}</cbc:TaxableAmount>")
            sb.appendLine("      <cbc:TaxAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(vat.tax)}</cbc:TaxAmount>")
            sb.appendLine("      <cac:TaxCategory>")
            sb.appendLine("        <cbc:ID>${if (vat.rate > 0) "S" else "Z"}</cbc:ID>")
            sb.appendLine("        <cbc:Percent>${String.format(Locale.US, "%.2f", vat.rate)}</cbc:Percent>")
            sb.appendLine("        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>")
            sb.appendLine("      </cac:TaxCategory>")
            sb.appendLine("    </cac:TaxSubtotal>")
        }
        sb.appendLine("  </cac:TaxTotal>")

        // LegalMonetaryTotal
        sb.appendLine("  <cac:LegalMonetaryTotal>")
        sb.appendLine("    <cbc:LineExtensionAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(calculated.subtotalNet)}</cbc:LineExtensionAmount>")
        sb.appendLine("    <cbc:TaxExclusiveAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(calculated.subtotalNet)}</cbc:TaxExclusiveAmount>")
        sb.appendLine("    <cbc:TaxInclusiveAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(calculated.payableTotal)}</cbc:TaxInclusiveAmount>")
        sb.appendLine("    <cbc:PayableAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(calculated.payableTotal)}</cbc:PayableAmount>")
        sb.appendLine("  </cac:LegalMonetaryTotal>")

        // Lines
        items.forEachIndexed { index, item ->
            val line = calculated.lines[index]
            sb.appendLine("  <cac:InvoiceLine>")
            sb.appendLine("    <cbc:ID>${index + 1}</cbc:ID>")
            sb.appendLine("    <cbc:InvoicedQuantity unitCode=\"C62\">${String.format(Locale.US, "%.2f", item.quantity)}</cbc:InvoicedQuantity>")
            sb.appendLine("    <cbc:LineExtensionAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(line.lineAmount)}</cbc:LineExtensionAmount>")
            sb.appendLine("    <cac:Item>")
            sb.appendLine("      <cbc:Name>Item ${index + 1}</cbc:Name>")
            sb.appendLine("      <cac:ClassifiedTaxCategory>")
            sb.appendLine("        <cbc:ID>${line.taxCategoryId}</cbc:ID>")
            sb.appendLine("        <cbc:Percent>${String.format(Locale.US, "%.2f", line.taxRate)}</cbc:Percent>")
            sb.appendLine("        <cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme>")
            sb.appendLine("      </cac:ClassifiedTaxCategory>")
            sb.appendLine("    </cac:Item>")
            sb.appendLine("    <cac:Price>")
            sb.appendLine("      <cbc:PriceAmount currencyID=\"${xmlEscape(currencyCode)}\">${formatAmount(item.unitPriceCents)}</cbc:PriceAmount>")
            sb.appendLine("    </cac:Price>")
            sb.appendLine("  </cac:InvoiceLine>")
        }

        sb.appendLine("</Invoice>")
        return sb.toString()
    }
}
