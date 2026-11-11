package com.invoicebuilder.core.model

/**
 * GST domain types shared by the tax engine, the persistence layer and the UI.
 */

/** How GST is applied for a given supply. */
enum class GstSupplyType {
    /** Same state: CGST + SGST. */
    IntraState,

    /** Inter-state: IGST. */
    InterState,

    /** Zero rated (export, SEZ without payment of tax). */
    ZeroRated,

    /** Exempt supply. */
    Exempt
}

/** Kind of document being issued. */
enum class InvoiceKind {
    TaxInvoice,
    Retail,
    Export,
    Proforma
}

/** GST registration category of the supplier. */
enum class GstRegistrationType {
    Regular,
    Composition,
    Sez,
    Ut,
    Oidc
}

/** Unit of measure used on an invoice line. */
enum class UnitOfMeasure {
    NOS,
    PCS,
    KGS,
    MTR,
    HRS,
    LTR,
    SQF,
    CMT
}

/** Whether a catalogue item is a good or a service (HSN vs SAC). */
enum class ItemType {
    Goods,
    Service
}

/** A single calculated invoice line. */
data class CalculatedLine(
    val itemName: String = "",
    val hsnCode: String? = null,
    val quantity: Double = 1.0,
    /** Taxable value of the line (net of discount, excluding tax). */
    val lineAmount: Money = Money.ZERO,
    /** Net value of a single unit. */
    val unitAmount: Money = Money.ZERO,
    /** Total tax on the line (CGST + SGST + IGST + cess). */
    val taxAmount: Money = Money.ZERO,
    val taxRate: Double = 0.0,
    /** UBL/legacy tax category: "S" for taxed, "Z" for zero/exempt. */
    val taxCategoryId: String = "Z",
    /** Gross value before discount and tax. */
    val grossAmount: Money = Money.ZERO,
    val discountAmount: Money = Money.ZERO,
    val igstAmount: Money = Money.ZERO,
    val cgstAmount: Money = Money.ZERO,
    val sgstAmount: Money = Money.ZERO,
    val cessAmount: Money = Money.ZERO
)

/** Tax totals grouped by rate and supply type. */
data class GstTaxGroup(
    val rate: Double,
    val supplyType: GstSupplyType,
    val taxable: Money = Money.ZERO,
    val cgst: Money = Money.ZERO,
    val sgst: Money = Money.ZERO,
    val igst: Money = Money.ZERO,
    val cess: Money = Money.ZERO
)

/** Fully calculated invoice totals. */
data class GstInvoiceTotals(
    val lines: List<CalculatedLine> = emptyList(),
    /** Sum of taxable values (net of discount, excluding tax). */
    val subtotalNet: Money = Money.ZERO,
    val discountTotal: Money = Money.ZERO,
    val shippingTotal: Money = Money.ZERO,
    val surchargeTotal: Money = Money.ZERO,
    /** CGST + SGST + IGST (excluding cess). */
    val taxTotal: Money = Money.ZERO,
    val cessTotal: Money = Money.ZERO,
    /** Rounding adjustment applied to reach the nearest rupee. */
    val roundOff: Money = Money.ZERO,
    /** Grand total payable. */
    val payableTotal: Money = Money.ZERO,
    val gstGroups: List<GstTaxGroup> = emptyList(),
    /** Amount in words using the Indian numbering system. */
    val amountInWords: String = ""
)
