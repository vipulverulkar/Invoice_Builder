package com.invoicebuilder.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RegionWidth {
    @SerialName("20%") W20,
    @SerialName("25%") W25,
    @SerialName("30%") W30,
    @SerialName("35%") W35,
    @SerialName("40%") W40,
    @SerialName("50%") W50,
    @SerialName("60%") W60,
    @SerialName("65%") W65,
    @SerialName("70%") W70,
    @SerialName("75%") W75,
    @SerialName("80%") W80,
    @SerialName("100%") W100
}

@Serializable
enum class RegionDirection {
    @SerialName("row") Row,
    @SerialName("column") Column,
    @SerialName("grid") Grid
}

@Serializable
enum class LayoutSectionType {
    @SerialName("watermark") Watermark,
    @SerialName("header") Header,
    @SerialName("itemsTable") ItemsTable,
    @SerialName("financialTotals") FinancialTotals,
    @SerialName("paymentInfo") PaymentInfo,
    @SerialName("totalsRow") TotalsRow,
    @SerialName("notes") Notes,
    @SerialName("signature") Signature,
    @SerialName("pageCounter") PageCounter
}

@Serializable
enum class HeaderBlockType {
    @SerialName("row") Row,
    @SerialName("column") Column,
    @SerialName("title") Title,
    @SerialName("logo") Logo,
    @SerialName("businessInfo") BusinessInfo,
    @SerialName("clientInfo") ClientInfo,
    @SerialName("invoiceMeta") InvoiceMeta,
    @SerialName("paymentInfo") PaymentInfo
}

@Serializable
data class HeaderBlock(
    val type: HeaderBlockType,
    val children: List<HeaderBlock>? = null,
    val width: String? = null,
    val align: String? = null,
    val textAlign: String? = null,
    val gap: Int? = null,
    val boxed: Boolean? = null
)

@Serializable
data class LayoutRegion(
    val id: String,
    val width: RegionWidth,
    val direction: RegionDirection,
    val gap: Int? = null,
    val blocks: List<HeaderBlock>? = null,
    val sections: List<LayoutSectionType>? = null
)

@Serializable
data class LayoutMeta(
    val name: String,
    val description: String? = null
)

@Serializable
data class LayoutSchemaV2(
    val schemaVersion: Int = 2,
    val meta: LayoutMeta,
    val regions: List<LayoutRegion>,
    val orientation: String? = "portrait"
)
