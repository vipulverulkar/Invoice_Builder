package com.invoicebuilder.core.database.entity

import androidx.room.*
import com.invoicebuilder.core.model.*

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1,
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
    // Legacy European e-invoice flags, retained so the schema stays stable.
    // Unused by the India-only build.
    val ublON: Boolean = true,
    val xrechnungON: Boolean = true,
    val receiptPrintingOn: Boolean = true,
    // Indian GST preferences
    val financialYear: String? = null,
    val defaultStateCode: String? = null,
    @ColumnInfo(defaultValue = "0") val gstRegistrationOn: Boolean = false,
    @ColumnInfo(defaultValue = "1") val roundOffEnabled: Boolean = true,
    val defaultInvoiceKind: String? = null,
    val gstnClientId: String? = null,
    val gstnClientSecret: String? = null,
    val gstnGstin: String? = null,
    val gstnEnvironment: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shortName: String,
    val address: String? = null,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val additional: String? = null,
    val paymentInformation: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val logo: ByteArray? = null,
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
    val registrationType: String? = null,
    val declaration: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    @ColumnInfo(defaultValue = "0.0") val tdsRate: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val tcsApplicable: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val symbol: String,
    val text: String,
    val format: String,
    val subunit: Int = 100,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("unitId"), Index("categoryId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: String = "0",
    val unitId: Long? = null,
    val categoryId: Long? = null,
    val description: String? = null,
    // Indian GST master data
    val hsnCode: String? = null,
    @ColumnInfo(defaultValue = "0.0") val gstRate: Double = 0.0,
    val unitCode: String? = null,
    val itemType: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "layouts")
data class LayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schema: String,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "style_profiles",
    foreignKeys = [
        ForeignKey(entity = LayoutEntity::class, parentColumns = ["id"], childColumns = ["layoutId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("layoutId")]
)
data class StyleProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false,
    val color: String? = null,
    val logoSize: String? = null,
    val fontSize: String? = null,
    val fontFamily: String? = null,
    val pdfTexts: String? = null,
    val layoutId: Long? = null,
    val tableHeaderStyle: String? = null,
    val tableRowStyle: String? = null,
    val pageFormat: String? = null,
    val labelUpperCase: Boolean = false,
    val showQuantity: Boolean = true,
    val showUnit: Boolean = true,
    val showRowNo: Boolean = true,
    val fieldSortOrders: String = "{\"no\":0,\"item\":1,\"unit\":2,\"quantity\":3,\"unitCost\":4,\"total\":5}",
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val watermarkFileData: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val paidWatermarkFileData: ByteArray? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val swiftCode: String? = null,
    val address: String? = null,
    val branchCode: String? = null,
    val type: String? = null,
    val routingNumber: String? = null,
    val upiCode: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val qrCode: ByteArray? = null,
    val accountHolder: String? = null,
    // Indian banking details
    val ifscCode: String? = null,
    val upiId: String? = null,
    val sortOrder: String? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "presets",
    foreignKeys = [
        ForeignKey(entity = StyleProfileEntity::class, parentColumns = ["id"], childColumns = ["styleProfilesId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BusinessEntity::class, parentColumns = ["id"], childColumns = ["businessId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CurrencyEntity::class, parentColumns = ["id"], childColumns = ["currencyId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = BankEntity::class, parentColumns = ["id"], childColumns = ["bankId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("styleProfilesId"), Index("businessId"), Index("clientId"), Index("currencyId"), Index("bankId")]
)
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val businessId: Long? = null,
    val clientId: Long? = null,
    val currencyId: Long? = null,
    val bankId: Long? = null,
    val customerNotes: String? = null,
    val thanksNotes: String? = null,
    val termsConditionNotes: String? = null,
    val language: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val signatureData: ByteArray? = null,
    val signatureName: String? = null,
    val styleProfilesId: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(entity = BusinessEntity::class, parentColumns = ["id"], childColumns = ["businessId"]),
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"]),
        ForeignKey(entity = CurrencyEntity::class, parentColumns = ["id"], childColumns = ["currencyId"]),
        ForeignKey(entity = BankEntity::class, parentColumns = ["id"], childColumns = ["bankId"]),
        ForeignKey(entity = LayoutEntity::class, parentColumns = ["id"], childColumns = ["layoutId"]),
        ForeignKey(entity = StyleProfileEntity::class, parentColumns = ["id"], childColumns = ["styleProfilesId"])
    ],
    indices = [
        Index("businessId"), Index("clientId"), Index("currencyId"), Index("bankId"),
        Index("layoutId"), Index("styleProfilesId"),
        Index(value = ["businessId", "invoiceNumber", "clientId", "invoiceType"], unique = true)
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val signatureData: ByteArray? = null,
    val signatureName: String? = null,
    val styleProfilesId: Long? = null,
    val language: String = "en",
    // Indian GST compliance fields
    val invoiceKind: String? = null,
    val placeOfSupply: String? = null,
    @ColumnInfo(defaultValue = "0") val reverseCharge: Boolean = false,
    @ColumnInfo(defaultValue = "'0'") val roundOffCents: String = "0",
    val amountInWords: String? = null,
    val tdsSection: String? = null,
    @ColumnInfo(defaultValue = "'0'") val tdsAmountCents: String = "0",
    val tcsSection: String? = null,
    @ColumnInfo(defaultValue = "'0'") val tcsAmountCents: String = "0",
    @ColumnInfo(defaultValue = "'0'") val totalCents: String = "0",
    val irn: String? = null,
    val ackNumber: String? = null,
    val ackDate: String? = null,
    val ewbNumber: String? = null,
    val ewbDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"])
    ],
    indices = [Index("parentInvoiceId"), Index("itemId")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val itemId: Long,
    val customField: String? = null,
    val quantity: String = "1",
    val taxRate: Double = 0.0,
    val taxType: InvoiceItemTaxType? = null,
    // Immutable snapshots so catalogue edits never rewrite issued invoices
    val itemName: String? = null,
    @ColumnInfo(defaultValue = "'0'") val unitPriceCents: String = "0",
    val hsnCode: String? = null,
    @ColumnInfo(defaultValue = "0.0") val gstRate: Double = 0.0,
    val discountType: String? = null,
    @ColumnInfo(defaultValue = "'0'") val discountAmountCents: String = "0",
    val unitCode: String? = null,
    @ColumnInfo(defaultValue = "'0'") val lineTotalCents: String = "0",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_payments",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId")]
)
data class InvoicePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val amountCents: String,
    val paidAt: String,
    val paymentMethod: String
)

@Entity(
    tableName = "invoice_sequences",
    indices = [Index(value = ["businessId", "clientId", "invoiceType"], unique = true)]
)
data class InvoiceSequenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long,
    val clientId: Long,
    val nextSequence: Long,
    val invoiceType: InvoiceType = InvoiceType.invoice,
    val financialYear: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
