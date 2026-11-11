package com.invoicebuilder.core.gstn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builder for the GST e-invoice document payload.
 *
 * The structure follows the GSTN e-invoice schema (version 1.1): seller and
 * buyer details, the item list with HSN codes and tax heads, and the value
 * totals. The payload can be uploaded through the GSTN offline utility when
 * the API gateway is not used.
 */
object GstEInvoiceJsonBuilder {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = false
    }

    /** A single invoice line with its GST breakdown. */
    data class Line(
        val itemName: String,
        val hsnCode: String,
        val quantity: Double,
        val unitPricePaise: Long,
        val discountPaise: Long,
        val taxableValuePaise: Long,
        val gstRate: Double,
        val igstPaise: Long = 0,
        val cgstPaise: Long = 0,
        val sgstPaise: Long = 0,
        val cessPaise: Long = 0,
        val totalPaise: Long = 0
    )

    /** Full e-invoice document. */
    data class Document(
        val gstin: String,
        val legalName: String,
        val invoiceNumber: String,
        val invoiceDate: String,
        val invoiceType: String = "Tax Invoice",
        val placeOfSupplyCode: String,
        val reverseCharge: Boolean = false,
        val buyerGstin: String? = null,
        val buyerName: String,
        val buyerAddress: String? = null,
        val buyerStateCode: String? = null,
        val lines: List<Line> = emptyList(),
        val taxableValuePaise: Long = 0,
        val discountPaise: Long = 0,
        val cgstPaise: Long = 0,
        val sgstPaise: Long = 0,
        val igstPaise: Long = 0,
        val cessPaise: Long = 0,
        val roundOffPaise: Long = 0,
        val totalPaise: Long = 0,
        val otherTerms: String? = null
    )

    fun build(document: Document): String {
        return json.encodeToString(JsonObject.serializer(), toJson(document))
    }

    fun toJson(document: Document): JsonObject = buildJsonObject {
        put("Version", "1.1")
        put("Irn", JsonPrimitive(""))
        put("TxnTp", JsonPrimitive("1"))
        put("RefId", JsonPrimitive(document.invoiceNumber))
        put("RefDt", JsonPrimitive(document.invoiceDate))
        put("PrvDcd", JsonPrimitive("N"))
        put("Rev", JsonPrimitive("N"))

        put(
            "DocDtls",
            buildJsonObject {
                put("Typ", JsonPrimitive(document.invoiceType))
                put("No", JsonPrimitive(document.invoiceNumber))
                put("Dt", JsonPrimitive(document.invoiceDate))
                put("Pos", JsonPrimitive(document.placeOfSupplyCode))
                put("Rev", JsonPrimitive("N"))
                if (document.reverseCharge) {
                    put("Rchrg", JsonPrimitive("Y"))
                }
            }
        )

        put(
            "SellrDtls",
            buildJsonObject {
                put("Gstin", JsonPrimitive(document.gstin))
                put("LglNm", JsonPrimitive(document.legalName))
                put("Addr1", JsonPrimitive(document.legalName))
            }
        )

        put(
            "BuyerDtls",
            buildJsonObject {
                put("Gstin", JsonPrimitive(document.buyerGstin.orEmpty()))
                put("LglNm", JsonPrimitive(document.buyerName))
                document.buyerAddress?.let { put("Addr1", JsonPrimitive(it)) }
                document.buyerStateCode?.let { put("Stcd", JsonPrimitive(it)) }
                put("Pos", JsonPrimitive(document.placeOfSupplyCode))
                put("Typ", JsonPrimitive("R"))
            }
        )

        put("ItemLst", itemList(document.lines))

        put(
            "ValDtls",
            buildJsonObject {
                put("AssVal", JsonPrimitive(document.taxableValuePaise / 100.0))
                put("Discount", JsonPrimitive(document.discountPaise / 100.0))
                put("TaxVal", JsonPrimitive((document.taxableValuePaise + document.discountPaise) / 100.0))
                put("RndOffAmt", JsonPrimitive(document.roundOffPaise / 100.0))
                put("TotInvVal", JsonPrimitive(document.totalPaise / 100.0))
            }
        )

        put(
            "TotlInvVal",
            buildJsonObject {
                put("AssVal", JsonPrimitive(document.taxableValuePaise / 100.0))
                put("CgstVal", JsonPrimitive(document.cgstPaise / 100.0))
                put("SgstVal", JsonPrimitive(document.sgstPaise / 100.0))
                put("IgstVal", JsonPrimitive(document.igstPaise / 100.0))
                put("CessAmt", JsonPrimitive(document.cessPaise / 100.0))
                put("RndOffAmt", JsonPrimitive(document.roundOffPaise / 100.0))
                put("TotInvVal", JsonPrimitive(document.totalPaise / 100.0))
            }
        )

        document.otherTerms?.let { put("Notes", JsonPrimitive(it)) }
    }

    private fun itemList(lines: List<Line>): JsonArray = buildJsonArray {
        lines.forEachIndexed { index, line ->
            add(
                buildJsonObject {
                    put("No", JsonPrimitive(index + 1))
                    put("ItmDet", buildJsonObject {
                        put("Rt", JsonPrimitive(line.gstRate))
                        put("TaxAmt", JsonPrimitive((line.igstPaise + line.cgstPaise + line.sgstPaise) / 100.0))
                        put("TaxRt", JsonPrimitive(line.gstRate))
                        put("CessAmt", JsonPrimitive(line.cessPaise / 100.0))
                    })
                    put("Amt", JsonPrimitive(line.totalPaise / 100.0))
                    put("SlNo", JsonPrimitive(index + 1))
                    put("Desc", JsonPrimitive(line.itemName))
                    put("HsnCd", JsonPrimitive(line.hsnCode))
                    put("Qty", JsonPrimitive(line.quantity))
                    put("Unit", JsonPrimitive("NOS"))
                    put("Rate", JsonPrimitive(line.unitPricePaise / 100.0))
                    put("TaxAmt", JsonPrimitive((line.igstPaise + line.cgstPaise + line.sgstPaise) / 100.0))
                    put("TotAmt", JsonPrimitive(line.totalPaise / 100.0))
                    put("AssAmt", JsonPrimitive(line.taxableValuePaise / 100.0))
                    if (line.discountPaise > 0) {
                        put("Disc", JsonPrimitive(line.discountPaise / 100.0))
                    }
                }
            )
        }
    }
}
