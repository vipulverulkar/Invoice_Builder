package com.invoicebuilder.core.database.repository

import com.invoicebuilder.core.database.dao.InvoiceDao
import com.invoicebuilder.core.database.entity.*
import com.invoicebuilder.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

data class CreateInvoiceParam(
    val invoice: Invoice,
    val items: List<InvoiceLineItem>,
    val business: Business,
    val client: Client,
    val currency: Currency,
    val bank: Bank?,
    val layoutSchemaJson: String?
)

interface InvoiceRepository {
    fun observeInvoices(type: InvoiceType): Flow<List<Invoice>>
    suspend fun getInvoiceById(id: Long): Invoice?
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceLineItem>
    fun observePayments(invoiceId: Long): Flow<List<InvoicePayment>>
    suspend fun createInvoice(param: CreateInvoiceParam): Long
    suspend fun recordPayment(invoiceId: Long, amountCents: String, method: String): Long
    suspend fun archiveInvoice(invoiceId: Long)
    suspend fun getNextSequenceNumber(businessId: Long, clientId: Long, type: InvoiceType): Long
}

class InvoiceRepositoryImpl(private val invoiceDao: InvoiceDao) : InvoiceRepository {

    override fun observeInvoices(type: InvoiceType): Flow<List<Invoice>> {
        return invoiceDao.observeInvoicesByType(type).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.toModel()
    }

    override suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceLineItem> {
        return invoiceDao.getInvoiceItems(invoiceId).map { it.toModel() }
    }

    override fun observePayments(invoiceId: Long): Flow<List<InvoicePayment>> {
        return invoiceDao.observePayments(invoiceId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun createInvoice(param: CreateInvoiceParam): Long {
        val now = Instant.now().toString()
        val invoiceEntity = param.invoice.toEntity().copy(
            createdAt = now,
            updatedAt = now
        )

        // Insert primary invoice
        val invoiceId = invoiceDao.insertInvoice(invoiceEntity)

        // Insert items
        val itemEntities = param.items.map {
            it.toEntity().copy(parentInvoiceId = invoiceId, createdAt = now, updatedAt = now)
        }
        val invoiceItemIds = invoiceDao.insertItems(itemEntities)

        // Immutable line item snapshots (name, price, HSN and GST rate at issue time)
        invoiceDao.insertItemSnapshots(
            invoiceItemIds.mapIndexed { index, id ->
                val line = param.items[index]
                InvoiceItemSnapshotEntity(
                    parentInvoiceItemId = id,
                    itemName = line.itemName.orEmpty(),
                    unitPriceCents = line.unitPriceCents,
                    unitName = line.unitCode?.name,
                    hsnCode = line.hsnCode,
                    gstRate = line.gstRate,
                    discountType = line.discountType?.name,
                    discountAmountCents = line.discountAmountCents,
                    lineTotalCents = line.lineTotalCents,
                    createdAt = now
                )
            }
        )

        // Insert immutable historic snapshots
        invoiceDao.insertBusinessSnapshot(
            InvoiceBusinessSnapshotEntity(
                parentInvoiceId = invoiceId,
                businessName = param.business.name,
                businessShortName = param.business.shortName,
                businessAddress = param.business.address,
                businessRole = param.business.role,
                businessEmail = param.business.email,
                businessPhone = param.business.phone,
                businessAdditional = param.business.additional,
                businessPaymentInformation = param.business.paymentInformation,
                businessLogo = param.business.logo,
                businessVatCode = param.business.vatCode,
                businessCode = param.business.code,
                businessPeppolEndpointId = param.business.peppolEndpointId,
                businessCountryCode = param.business.countryCode,
                businessPeppolEndpointSchemeId = param.business.peppolEndpointSchemeId,
                businessGstin = param.business.gstin,
                businessPan = param.business.pan,
                businessStateCode = param.business.stateCode,
                createdAt = now,
                updatedAt = now
            )
        )

        invoiceDao.insertClientSnapshot(
            InvoiceClientSnapshotEntity(
                parentInvoiceId = invoiceId,
                clientName = param.client.name,
                clientAddress = param.client.address,
                clientEmail = param.client.email,
                clientPhone = param.client.phone,
                clientCode = param.client.code,
                clientAdditional = param.client.additional,
                clientVatCode = param.client.vatCode,
                clientPeppolEndpointId = param.client.peppolEndpointId,
                clientCountryCode = param.client.countryCode,
                clientPeppolEndpointSchemeId = param.client.peppolEndpointSchemeId,
                clientBuyerReference = param.client.buyerReference,
                clientGstin = param.client.gstin,
                clientTradeName = param.client.tradeName,
                clientStateCode = param.client.stateCode,
                createdAt = now,
                updatedAt = now
            )
        )

        invoiceDao.insertCurrencySnapshot(
            InvoiceCurrencySnapshotEntity(
                parentInvoiceId = invoiceId,
                currencyCode = param.currency.code,
                currencySymbol = param.currency.symbol,
                currencySubunit = param.currency.subunit,
                createdAt = now,
                updatedAt = now
            )
        )

        param.bank?.let { bank ->
            invoiceDao.insertBankSnapshot(
                InvoiceBankSnapshotEntity(
                    parentInvoiceId = invoiceId,
                    name = bank.name,
                    bankName = bank.bankName ?: "",
                    accountNumber = bank.accountNumber ?: "",
                    swiftCode = bank.swiftCode,
                    address = bank.address,
                    branchCode = bank.branchCode,
                    type = bank.type,
                    routingNumber = bank.routingNumber,
                    upiCode = bank.upiCode,
                    qrCode = bank.qrCode,
                    accountHolder = bank.accountHolder,
                    ifscCode = bank.ifscCode,
                    upiId = bank.upiId,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        param.layoutSchemaJson?.let { schema ->
            invoiceDao.insertLayoutSnapshot(
                InvoiceLayoutSnapshotEntity(
                    parentInvoiceId = invoiceId,
                    layoutSchema = schema,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        return invoiceId
    }

    override suspend fun recordPayment(invoiceId: Long, amountCents: String, method: String): Long {
        return invoiceDao.insertPayment(
            InvoicePaymentEntity(
                parentInvoiceId = invoiceId,
                amountCents = amountCents,
                paidAt = Instant.now().toString(),
                paymentMethod = method
            )
        )
    }

    override suspend fun archiveInvoice(invoiceId: Long) {
        invoiceDao.archiveInvoice(invoiceId)
    }

    override suspend fun getNextSequenceNumber(businessId: Long, clientId: Long, type: InvoiceType): Long {
        val existing = invoiceDao.getSequence(businessId, clientId, type)
        val next = (existing?.nextSequence ?: 0L) + 1L
        invoiceDao.saveSequence(
            InvoiceSequenceEntity(
                id = existing?.id ?: 0L,
                businessId = businessId,
                clientId = clientId,
                nextSequence = next,
                invoiceType = type,
                updatedAt = Instant.now().toString()
            )
        )
        return next
    }

    private fun InvoiceEntity.toModel() = Invoice(
        id = id,
        invoiceType = invoiceType,
        convertedFromQuotationId = convertedFromQuotationId,
        businessId = businessId,
        clientId = clientId,
        currencyId = currencyId,
        bankId = bankId,
        layoutId = layoutId,
        invoiceNumber = invoiceNumber,
        invoicePrefix = invoicePrefix,
        invoiceSuffix = invoiceSuffix,
        isArchived = isArchived,
        status = status,
        issuedAt = issuedAt,
        dueDate = dueDate,
        paidAt = paidAt,
        closedAt = closedAt,
        customerNotes = customerNotes,
        thanksNotes = thanksNotes,
        termsConditionNotes = termsConditionNotes,
        discountName = discountName,
        discountType = discountType,
        discountAmountCents = discountAmountCents,
        discountPercent = discountPercent,
        surchargeName = surchargeName,
        surchargeType = surchargeType,
        surchargeAmountCents = surchargeAmountCents,
        surchargePercent = surchargePercent,
        shippingFeeCents = shippingFeeCents,
        taxName = taxName,
        taxRate = taxRate,
        taxType = taxType,
        signatureData = signatureData,
        styleProfilesId = styleProfilesId,
        language = language,
        invoiceKind = invoiceKind?.let { runCatching { InvoiceKind.valueOf(it) }.getOrNull() },
        placeOfSupply = placeOfSupply,
        reverseCharge = reverseCharge,
        roundOffCents = roundOffCents,
        amountInWords = amountInWords,
        tdsSection = tdsSection,
        tdsAmountCents = tdsAmountCents,
        tcsSection = tcsSection,
        tcsAmountCents = tcsAmountCents,
        totalCents = totalCents,
        irn = irn,
        ackNumber = ackNumber,
        ackDate = ackDate,
        ewbNumber = ewbNumber,
        ewbDate = ewbDate
    )

    private fun Invoice.toEntity() = InvoiceEntity(
        id = id,
        invoiceType = invoiceType,
        convertedFromQuotationId = convertedFromQuotationId,
        businessId = businessId,
        clientId = clientId,
        currencyId = currencyId,
        bankId = bankId,
        layoutId = layoutId,
        invoiceNumber = invoiceNumber,
        invoicePrefix = invoicePrefix,
        invoiceSuffix = invoiceSuffix,
        isArchived = isArchived,
        status = status,
        issuedAt = issuedAt,
        dueDate = dueDate,
        paidAt = paidAt,
        closedAt = closedAt,
        customerNotes = customerNotes,
        thanksNotes = thanksNotes,
        termsConditionNotes = termsConditionNotes,
        discountName = discountName,
        discountType = discountType,
        discountAmountCents = discountAmountCents,
        discountPercent = discountPercent,
        surchargeName = surchargeName,
        surchargeType = surchargeType,
        surchargeAmountCents = surchargeAmountCents,
        surchargePercent = surchargePercent,
        shippingFeeCents = shippingFeeCents,
        taxName = taxName,
        taxRate = taxRate,
        taxType = taxType,
        signatureData = signatureData,
        styleProfilesId = styleProfilesId,
        language = language,
        invoiceKind = invoiceKind?.name,
        placeOfSupply = placeOfSupply,
        reverseCharge = reverseCharge,
        roundOffCents = roundOffCents,
        amountInWords = amountInWords,
        tdsSection = tdsSection,
        tdsAmountCents = tdsAmountCents,
        tcsSection = tcsSection,
        tcsAmountCents = tcsAmountCents,
        totalCents = totalCents,
        irn = irn,
        ackNumber = ackNumber,
        ackDate = ackDate,
        ewbNumber = ewbNumber,
        ewbDate = ewbDate
    )

    private fun InvoiceItemEntity.toModel() = InvoiceLineItem(
        id = id,
        parentInvoiceId = parentInvoiceId,
        itemId = itemId,
        customField = customField,
        quantity = quantity,
        taxRate = taxRate,
        taxType = taxType,
        itemName = itemName,
        unitPriceCents = unitPriceCents,
        hsnCode = hsnCode,
        gstRate = gstRate,
        discountType = discountType?.let { runCatching { DiscountType.valueOf(it) }.getOrNull() },
        discountAmountCents = discountAmountCents,
        unitCode = unitCode?.let { runCatching { UnitOfMeasure.valueOf(it) }.getOrNull() },
        lineTotalCents = lineTotalCents
    )

    private fun InvoiceLineItem.toEntity() = InvoiceItemEntity(
        id = id,
        parentInvoiceId = parentInvoiceId,
        itemId = itemId,
        customField = customField,
        quantity = quantity,
        taxRate = taxRate,
        taxType = taxType,
        itemName = itemName,
        unitPriceCents = unitPriceCents,
        hsnCode = hsnCode,
        gstRate = gstRate,
        discountType = discountType?.name,
        discountAmountCents = discountAmountCents,
        unitCode = unitCode?.name,
        lineTotalCents = lineTotalCents
    )

    private fun InvoicePaymentEntity.toModel() = InvoicePayment(
        id = id,
        parentInvoiceId = parentInvoiceId,
        amountCents = amountCents,
        paidAt = paidAt,
        paymentMethod = paymentMethod
    )
}
