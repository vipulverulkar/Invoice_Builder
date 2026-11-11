package com.invoicebuilder.core.gstn

/**
 * Offline gateway used when the GSTN API is not configured.
 *
 * Every remote operation fails with a recoverable error so the UI can fall back
 * to manual IRN / e-way bill capture instead of blocking the user.
 */
class ManualEInvoiceGateway : EInvoiceGateway {

    override val mode: EInvoiceMode = EInvoiceMode.Manual

    override suspend fun generateIrn(request: IrnRequest): GatewayResult<IrnResult> {
        return GatewayResult.Failure(
            message = "GSTN gateway is not configured. Capture the IRN manually.",
            code = MANUAL_MODE,
            recoverable = true
        )
    }

    override suspend fun cancelIrn(irn: String, reason: String): GatewayResult<Unit> {
        return GatewayResult.Failure(
            message = "IRN cancellation requires the GSTN gateway. Use the GSTN portal.",
            code = MANUAL_MODE,
            recoverable = true
        )
    }

    override suspend fun generateEwb(request: EwbRequest): GatewayResult<EwbResult> {
        return GatewayResult.Failure(
            message = "E-way bill generation requires the GSTN gateway. Use the GSTN portal.",
            code = MANUAL_MODE,
            recoverable = true
        )
    }

    companion object {
        const val MANUAL_MODE = "MANUAL_MODE_REQUIRED"
    }
}
