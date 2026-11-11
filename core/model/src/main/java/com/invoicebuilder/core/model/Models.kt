package com.invoicebuilder.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Business(
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val address: String? = null,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val additional: String? = null,
    val paymentInformation: String? = null,
    val logo: ByteArray? = null,
    val fileSize: Long? = null,
    val fileType: String? = null,
    val fileName: String? = null,
    val vatCode: String? = null,
    val code: String? = null,
    val peppolEndpointId: String? = null,
    val countryCode: String? = null,
    val peppolEndpointSchemeId: String? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class Client(
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val address: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val code: String? = null,
    val additional: String? = null,
    val vatCode: String? = null,
    val peppolEndpointId: String? = null,
    val countryCode: String? = null,
    val peppolEndpointSchemeId: String? = null,
    val buyerReference: String? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class Item(
    val id: Long = 0,
    val name: String,
    val amountCents: String = "0",
    val unitId: Long? = null,
    val categoryId: Long? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class UnitItem(
    val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false
)

@Serializable
data class Category(
    val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false
)

@Serializable
data class Currency(
    val id: Long = 0,
    val code: String,
    val symbol: String,
    val text: String,
    val format: String,
    val subunit: Int = 100,
    val isArchived: Boolean = false
)

@Serializable
data class Bank(
    val id: Long = 0,
    val name: String,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val swiftCode: String? = null,
    val address: String? = null,
    val branchCode: String? = null,
    val type: String? = null,
    val routingNumber: String? = null,
    val upiCode: String? = null,
    val qrCode: ByteArray? = null,
    val accountHolder: String? = null,
    val isArchived: Boolean = false
)

@Serializable
data class Settings(
    val id: Long = 1,
    val language: String = "en",
    val amountFormat: String = "en-US",
    val dateFormat: String = "MM/dd/yyyy",
    val isDarkMode: Boolean = true,
    val invoicePrefix: String? = null,
    val invoiceSuffix: String? = null,
    val shouldIncludeYear: Boolean = true,
    val shouldIncludeMonth: Boolean = true,
    val shouldIncludeBusinessName: Boolean = true,
    val quotesON: Boolean = true,
    val reportsON: Boolean = true,
    val styleProfilesON: Boolean = true,
    val presetsON: Boolean = true,
    val ublON: Boolean = true,
    val xrechnungON: Boolean = true,
    val receiptPrintingOn: Boolean = true
)

@Serializable
data class Invoice(
    val id: Long = 0,
    val invoiceType: InvoiceType = InvoiceType.invoice,
    val convertedFromQuotationId: Long? = null,
    val businessId: Long,
    val clientId: Long,
    val currencyId: Long,
    val bankId: Long? = null,
    val layoutId: Long? = null,
    val invoiceNumber: String,
    val invoicePrefix: String? = null,
    val invoiceSuffix: String? = null,
    val isArchived: Boolean = false,
    val status: InvoiceStatus = InvoiceStatus.unpaid,
    val issuedAt: String,
    val dueDate: String? = null,
    val paidAt: String? = null,
    val closedAt: String? = null,
    val customerNotes: String? = null,
    val thanksNotes: String? = null,
    val termsConditionNotes: String? = null,
    val discountName: String? = null,
    val discountType: DiscountType? = null,
    val discountAmountCents: String = "0",
    val discountPercent: Double = 0.0,
    val surchargeName: String? = null,
    val surchargeType: DiscountType? = null,
    val surchargeAmountCents: String = "0",
    val surchargePercent: Double = 0.0,
    val shippingFeeCents: String = "0",
    val taxName: String? = null,
    val taxRate: Double = 0.0,
    val taxType: TaxType? = null,
    val signatureData: ByteArray? = null,
    val styleProfilesId: Long? = null,
    val language: String = "en"
) {
    val fullInvoiceNumber: String
        get() = "${invoicePrefix.orEmpty()}$invoiceNumber${invoiceSuffix.orEmpty()}"
}

@Serializable
data class InvoiceLineItem(
    val id: Long = 0,
    val parentInvoiceId: Long = 0,
    val itemId: Long,
    val customField: String? = null,
    val quantity: String = "1",
    val taxRate: Double = 0.0,
    val taxType: InvoiceItemTaxType? = null
)

@Serializable
data class InvoicePayment(
    val id: Long = 0,
    val parentInvoiceId: Long,
    val amountCents: String,
    val paidAt: String,
    val paymentMethod: String
)

@Serializable
data class CalculatedLine(
    val lineAmount: Double,
    val unitAmount: Double,
    val taxAmount: Double,
    val taxRate: Double,
    val taxCategoryId: String,
    val grossAmount: Double,
    val discountAmount: Double
)

@Serializable
data class VatGroup(
    val rate: Double,
    val taxable: Double,
    val tax: Double
)

@Serializable
data class CalculatedInvoice(
    val lines: List<CalculatedLine>,
    val subtotalNet: Double,
    val discountTotal: Double,
    val shippingTotal: Double,
    val surchargeTotal: Double,
    val taxTotal: Double,
    val payableTotal: Double,
    val vatGroups: List<VatGroup>
)
