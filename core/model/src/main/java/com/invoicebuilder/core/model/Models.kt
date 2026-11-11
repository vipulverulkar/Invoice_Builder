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
    // Indian GST master data
    val gstin: String? = null,
    val pan: String? = null,
    val stateCode: String? = null,
    val registrationType: GstRegistrationType? = null,
    val declaration: String? = null,
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
    // Indian GST master data
    val gstin: String? = null,
    val tradeName: String? = null,
    val stateCode: String? = null,
    val tdsSection: String? = null,
    val tdsRate: Double = 0.0,
    val tcsApplicable: Boolean = false,
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
    // Indian GST master data
    val hsnCode: String? = null,
    val gstRate: Double = 0.0,
    val unitCode: UnitOfMeasure? = null,
    val itemType: ItemType? = null,
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
    // Indian banking details
    val ifscCode: String? = null,
    val upiId: String? = null,
    val isArchived: Boolean = false
)

@Serializable
data class Settings(
    val id: Long = 1,
    val language: String = "en",
    val amountFormat: String = "en-IN",
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
    val receiptPrintingOn: Boolean = true,
    // Indian GST preferences
    val financialYear: String? = null,
    val defaultStateCode: String? = null,
    val gstRegistrationOn: Boolean = false,
    val roundOffEnabled: Boolean = true,
    val defaultInvoiceKind: InvoiceKind? = null,
    val gstnClientId: String? = null,
    val gstnClientSecret: String? = null,
    val gstnGstin: String? = null,
    val gstnEnvironment: String? = null
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
    val language: String = "en",
    // Indian GST compliance fields
    val invoiceKind: InvoiceKind? = null,
    val placeOfSupply: String? = null,
    val reverseCharge: Boolean = false,
    val roundOffCents: String = "0",
    val amountInWords: String? = null,
    val tdsSection: String? = null,
    val tdsAmountCents: String = "0",
    val tcsSection: String? = null,
    val tcsAmountCents: String = "0",
    val totalCents: String = "0",
    val irn: String? = null,
    val ackNumber: String? = null,
    val ackDate: String? = null,
    val ewbNumber: String? = null,
    val ewbDate: String? = null
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
    val taxType: InvoiceItemTaxType? = null,
    // Immutable snapshots so catalogue edits never rewrite issued invoices
    val itemName: String? = null,
    val unitPriceCents: String = "0",
    val hsnCode: String? = null,
    val gstRate: Double = 0.0,
    val discountType: DiscountType? = null,
    val discountAmountCents: String = "0",
    val unitCode: UnitOfMeasure? = null,
    val lineTotalCents: String = "0"
)

@Serializable
data class InvoicePayment(
    val id: Long = 0,
    val parentInvoiceId: Long,
    val amountCents: String,
    val paidAt: String,
    val paymentMethod: String
)
