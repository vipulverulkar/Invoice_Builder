package com.invoicebuilder.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InvoiceType {
    @SerialName("invoice") invoice,
    @SerialName("quotation") quotation
}

@Serializable
enum class InvoiceStatus {
    @SerialName("unpaid") unpaid,
    @SerialName("open") open,
    @SerialName("partially") partially,
    @SerialName("paid") paid,
    @SerialName("closed") closed
}

@Serializable
enum class DiscountType {
    @SerialName("fixed") fixed,
    @SerialName("percentage") percentage
}

@Serializable
enum class TaxType {
    @SerialName("exclusive") exclusive,
    @SerialName("inclusive") inclusive,
    @SerialName("deducted") deducted
}

@Serializable
enum class InvoiceItemTaxType {
    @SerialName("exclusive") exclusive,
    @SerialName("inclusive") inclusive
}

@Serializable
enum class AmountFormat(val localeTag: String) {
    @SerialName("en-US") EnUS("en-US"),
    @SerialName("de-DE") DeDE("de-DE"),
    @SerialName("fr-FR") FrFR("fr-FR"),
    @SerialName("cs-CZ") CsCZ("cs-CZ"),
    @SerialName("lt-LT") LtLT("lt-LT"),
    @SerialName("pt-PT") PtPT("pt-PT")
}

@Serializable
enum class DateFormat(val pattern: String) {
    @SerialName("MM/dd/yyyy") US("MM/dd/yyyy"),
    @SerialName("dd/MM/yyyy") EU("dd/MM/yyyy"),
    @SerialName("yyyy-MM-dd") ISO("yyyy-MM-dd"),
    @SerialName("dd.MM.yyyy") DOT("dd.MM.yyyy")
}

@Serializable
enum class Language(val code: String) {
    @SerialName("en") English("en"),
    @SerialName("hi") Hindi("hi"),
    @SerialName("bn") Bengali("bn"),
    @SerialName("te") Telugu("te"),
    @SerialName("mr") Marathi("mr"),
    @SerialName("ta") Tamil("ta"),
    @SerialName("gu") Gujarati("gu"),
    @SerialName("kn") Kannada("kn"),
    @SerialName("ml") Malayalam("ml"),
    @SerialName("pa") Punjabi("pa"),
    @SerialName("de") German("de"),
    @SerialName("fr") French("fr"),
    @SerialName("cs") Czech("cs"),
    @SerialName("lt") Lithuanian("lt"),
    @SerialName("pt") Portuguese("pt")
}

@Serializable
enum class PageFormat {
    @SerialName("A4") A4,
    @SerialName("letter") Letter
}

@Serializable
enum class TableHeaderStyle {
    @SerialName("light") Light,
    @SerialName("dark") Dark,
    @SerialName("colored") Colored
}

@Serializable
enum class TableRowStyle {
    @SerialName("classic") Classic,
    @SerialName("striped") Striped,
    @SerialName("minimal") Minimal
}

@Serializable
enum class FontFamily {
    @SerialName("Roboto") Roboto,
    @SerialName("Inter") Inter
}

@Serializable
enum class SizeType {
    @SerialName("small") Small,
    @SerialName("medium") Medium,
    @SerialName("large") Large
}

@Serializable
enum class EInvoice {
    @SerialName("ubl21") UBL21,
    @SerialName("xrechnung") XRechnung
}
