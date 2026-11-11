package com.invoicebuilder.core.database.entity

import androidx.room.*

@Entity(
    tableName = "invoice_business_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId"), Index("businessName"), Index("businessShortName")]
)
data class InvoiceBusinessSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val businessName: String,
    val businessShortName: String,
    val businessAddress: String? = null,
    val businessRole: String? = null,
    val businessEmail: String? = null,
    val businessPhone: String? = null,
    val businessAdditional: String? = null,
    val businessPaymentInformation: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val businessLogo: ByteArray? = null,
    val businessFileSize: Long? = null,
    val businessFileType: String? = null,
    val businessFileName: String? = null,
    val businessVatCode: String? = null,
    val businessCode: String? = null,
    val businessPeppolEndpointId: String? = null,
    val businessCountryCode: String? = null,
    val businessPeppolEndpointSchemeId: String? = null,
    // Indian GST identity captured at issue time
    val businessGstin: String? = null,
    val businessPan: String? = null,
    val businessStateCode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_client_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId"), Index("clientName"), Index("clientCode")]
)
data class InvoiceClientSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val clientName: String,
    val clientAddress: String? = null,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val clientCode: String? = null,
    val clientAdditional: String? = null,
    val clientVatCode: String? = null,
    val clientPeppolEndpointId: String? = null,
    val clientCountryCode: String? = null,
    val clientPeppolEndpointSchemeId: String? = null,
    val clientBuyerReference: String? = null,
    // Indian GST identity captured at issue time
    val clientGstin: String? = null,
    val clientTradeName: String? = null,
    val clientStateCode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_currency_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId"), Index("currencyCode")]
)
data class InvoiceCurrencySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val currencyCode: String,
    val currencySymbol: String,
    val currencySubunit: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_bank_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId")]
)
data class InvoiceBankSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val name: String,
    val bankName: String,
    val accountNumber: String,
    val swiftCode: String? = null,
    val address: String? = null,
    val branchCode: String? = null,
    val type: String? = null,
    val routingNumber: String? = null,
    val upiCode: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val qrCode: ByteArray? = null,
    val accountHolder: String? = null,
    val ifscCode: String? = null,
    val upiId: String? = null,
    val sortOrder: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_customizations",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId")]
)
data class InvoiceCustomizationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val color: String = "#006400",
    val logoSize: String = "medium",
    val fontSize: String = "medium",
    val tableHeaderStyle: String = "light",
    val tableRowStyle: String = "classic",
    val pageFormat: String = "A4",
    val labelUpperCase: Boolean = false,
    val showQuantity: Boolean = true,
    val showUnit: Boolean = true,
    val showRowNo: Boolean = true,
    val fieldSortOrders: String = "{\"no\":0,\"item\":1,\"unit\":2,\"quantity\":3,\"unitCost\":4,\"total\":5}",
    val fontFamily: String = "Roboto",
    val pdfTexts: String? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val watermarkFileData: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val paidWatermarkFileData: ByteArray? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_style_profile_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId")]
)
data class InvoiceStyleProfileSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val styleProfileName: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_item_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceItemEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceItemId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceItemId"), Index("itemName")]
)
data class InvoiceItemSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceItemId: Long,
    val itemName: String,
    val unitPriceCents: String = "0",
    val unitName: String? = null,
    // Indian GST details captured at issue time
    val hsnCode: String? = null,
    @ColumnInfo(defaultValue = "0.0") val gstRate: Double = 0.0,
    val discountType: String? = null,
    @ColumnInfo(defaultValue = "'0'") val discountAmountCents: String = "0",
    @ColumnInfo(defaultValue = "'0'") val lineTotalCents: String = "0",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(
    tableName = "invoice_layout_snapshots",
    foreignKeys = [
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["parentInvoiceId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("parentInvoiceId", unique = true)]
)
data class InvoiceLayoutSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentInvoiceId: Long,
    val layoutSchema: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
