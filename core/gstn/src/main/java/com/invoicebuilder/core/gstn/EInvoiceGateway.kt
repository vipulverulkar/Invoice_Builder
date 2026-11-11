package com.invoicebuilder.core.gstn

import kotlinx.serialization.Serializable

/**
 * How the app obtains an IRN (e-invoice registration number).
 *
 * The app is offline-first: [Manual] is the default and requires no network
 * access or credentials, [Api] talks to the GSTN sandbox/production gateway.
 */
enum class EInvoiceMode {
    Manual,
    Api
}

/** Environment used when talking to the GSTN gateway. */
enum class GstnEnvironment(val value: String) {
    Sandbox("sandbox"),
    Production("production")
}

/** Credentials for the GSTN OAuth client-credentials flow. */
data class GstnCredentials(
    val clientId: String,
    val clientSecret: String,
    val gstin: String,
    val environment: GstnEnvironment = GstnEnvironment.Sandbox
)

/** Result wrapper so the UI can react to recoverable failures. */
sealed interface GatewayResult<out T> {
    data class Success<T>(val value: T) : GatewayResult<T>
    data class Failure(
        val message: String,
        val code: String? = null,
        val recoverable: Boolean = true
    ) : GatewayResult<Nothing>
}

/** IRN and acknowledgement details returned by the gateway. */
data class IrnResult(
    val irn: String,
    val ackNumber: String,
    val ackDate: String,
    val signedQrCode: String? = null
)

/** E-way bill details returned by the gateway. */
data class EwbResult(
    val ewbNumber: String,
    val ewbDate: String,
    val validUpto: String? = null
)

/** Request payload for e-invoice registration. */
data class IrnRequest(
    val document: GstEInvoiceJsonBuilder.Document,
    val credentials: GstnCredentials
)

/** Request payload for e-way bill generation. */
data class EwbRequest(
    val irn: String,
    val transporterId: String,
    val transporterName: String,
    val vehicleNumber: String?,
    val distanceKm: Int?,
    val goodsValuePaise: Long,
    val placeOfDispatchCode: String,
    val placeOfSupplyCode: String
)

/**
 * Gateway for e-invoice and e-way bill services.
 *
 * Implementations: a manual (offline) gateway and a GSTN API gateway.
 */
interface EInvoiceGateway {
    val mode: EInvoiceMode

    suspend fun generateIrn(request: IrnRequest): GatewayResult<IrnResult>

    suspend fun cancelIrn(irn: String, reason: String): GatewayResult<Unit>

    suspend fun generateEwb(request: EwbRequest): GatewayResult<EwbResult>
}
