package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.calculator.InvoiceCalculator
import com.invoicebuilder.core.calculator.ItemCalculationInput
import com.invoicebuilder.core.database.dao.BankDao
import com.invoicebuilder.core.database.dao.CurrencyDao
import com.invoicebuilder.core.database.entity.BankEntity
import com.invoicebuilder.core.database.entity.CurrencyEntity
import com.invoicebuilder.core.database.repository.*
import com.invoicebuilder.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class InvoiceLineItemDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemId: Long = 0,
    val name: String = "",
    val unitPriceCents: Double = 0.0,
    val quantity: Double = 1.0,
    val taxRate: Double = 0.0,
    val taxType: InvoiceItemTaxType? = null
)

data class InvoiceFormUiState(
    val invoiceType: InvoiceType = InvoiceType.invoice,
    val businesses: List<Business> = emptyList(),
    val clients: List<Client> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val banks: List<BankEntity> = emptyList(),
    val catalogItems: List<Item> = emptyList(),
    
    val selectedBusiness: Business? = null,
    val selectedClient: Client? = null,
    val selectedCurrency: CurrencyEntity? = null,
    val selectedBank: BankEntity? = null,
    
    val invoiceNumber: String = "1",
    val prefix: String = "INV-",
    val suffix: String = "",
    val issueDate: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
    val dueDate: String = LocalDate.now().plusDays(14).format(DateTimeFormatter.ISO_DATE),
    
    val lineItems: List<InvoiceLineItemDraft> = emptyList(),
    
    val discountType: DiscountType? = null,
    val discountAmountCents: Double = 0.0,
    val discountPercent: Double = 0.0,
    
    val surchargeType: DiscountType? = null,
    val surchargeAmountCents: Double = 0.0,
    val surchargePercent: Double = 0.0,
    
    val shippingFeeCents: Double = 0.0,
    val globalTaxRate: Double = 0.0,
    val globalTaxType: TaxType? = null,
    
    val customerNotes: String = "",
    val termsNotes: String = "",
    
    val calculated: CalculatedInvoice? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false
)

@HiltViewModel
class InvoiceFormViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val clientRepository: ClientRepository,
    private val businessRepository: BusinessRepository,
    private val itemRepository: ItemRepository,
    private val currencyDao: CurrencyDao,
    private val bankDao: BankDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceFormUiState())
    val uiState: StateFlow<InvoiceFormUiState> = _uiState.asStateFlow()

    init {
        loadCatalogs()
    }

    private fun loadCatalogs() {
        viewModelScope.launch {
            combine(
                businessRepository.observeBusinesses(),
                clientRepository.observeClients(),
                currencyDao.observeActiveCurrencies(),
                bankDao.observeActiveBanks(),
                itemRepository.observeItems()
            ) { businesses, clients, currencies, banks, items ->
                val state = _uiState.value
                state.copy(
                    businesses = businesses,
                    clients = clients,
                    currencies = currencies,
                    banks = banks,
                    catalogItems = items,
                    selectedBusiness = state.selectedBusiness ?: businesses.firstOrNull(),
                    selectedClient = state.selectedClient ?: clients.firstOrNull(),
                    selectedCurrency = state.selectedCurrency ?: currencies.firstOrNull(),
                    selectedBank = state.selectedBank ?: banks.firstOrNull()
                )
            }.collect { newState ->
                _uiState.value = newState
                recalculate()
            }
        }
    }

    fun selectBusiness(business: Business) {
        _uiState.update { it.copy(selectedBusiness = business) }
        updateSequenceNumber()
    }

    fun selectClient(client: Client) {
        _uiState.update { it.copy(selectedClient = client) }
        updateSequenceNumber()
    }

    fun selectCurrency(currency: CurrencyEntity) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun addLineItem(item: Item) {
        val currentLines = _uiState.value.lineItems.toMutableList()
        currentLines.add(
            InvoiceLineItemDraft(
                itemId = item.id,
                name = item.name,
                unitPriceCents = item.amountCents.toDoubleOrNull() ?: 0.0,
                quantity = 1.0
            )
        )
        _uiState.update { it.copy(lineItems = currentLines) }
        recalculate()
    }

    fun updateLineItem(id: String, quantity: Double, unitPriceCents: Double) {
        val updated = _uiState.value.lineItems.map {
            if (it.id == id) it.copy(quantity = quantity, unitPriceCents = unitPriceCents) else it
        }
        _uiState.update { it.copy(lineItems = updated) }
        recalculate()
    }

    fun removeLineItem(id: String) {
        val filtered = _uiState.value.lineItems.filterNot { it.id == id }
        _uiState.update { it.copy(lineItems = filtered) }
        recalculate()
    }

    fun setDiscount(type: DiscountType?, amountCents: Double, percent: Double) {
        _uiState.update { it.copy(discountType = type, discountAmountCents = amountCents, discountPercent = percent) }
        recalculate()
    }

    fun setTax(rate: Double, type: TaxType?) {
        _uiState.update { it.copy(globalTaxRate = rate, globalTaxType = type) }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val itemsInput = state.lineItems.map {
            ItemCalculationInput(
                unitPriceCents = it.unitPriceCents,
                quantity = it.quantity,
                taxRate = it.taxRate,
                taxType = it.taxType
            )
        }

        val calculated = InvoiceCalculator.calculateInvoiceTotals(
            items = itemsInput,
            discountType = state.discountType,
            discountAmountCents = state.discountAmountCents,
            discountPercent = state.discountPercent,
            surchargeType = state.surchargeType,
            surchargeAmountCents = state.surchargeAmountCents,
            surchargePercent = state.surchargePercent,
            shippingFeeCents = state.shippingFeeCents,
            taxRate = state.globalTaxRate,
            taxType = state.globalTaxType
        )

        _uiState.update { it.copy(calculated = calculated) }
    }

    private fun updateSequenceNumber() {
        val state = _uiState.value
        val businessId = state.selectedBusiness?.id ?: return
        val clientId = state.selectedClient?.id ?: return

        viewModelScope.launch {
            val next = invoiceRepository.getNextSequenceNumber(businessId, clientId, state.invoiceType)
            _uiState.update { it.copy(invoiceNumber = next.toString()) }
        }
    }

    fun saveInvoice(onSuccess: () -> Unit) {
        val state = _uiState.value
        val business = state.selectedBusiness ?: return
        val client = state.selectedClient ?: return
        val currency = state.selectedCurrency ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val invoice = Invoice(
                invoiceType = state.invoiceType,
                businessId = business.id,
                clientId = client.id,
                currencyId = currency.id,
                bankId = state.selectedBank?.id,
                invoiceNumber = state.invoiceNumber,
                invoicePrefix = state.prefix,
                invoiceSuffix = state.suffix,
                issuedAt = state.issueDate,
                dueDate = state.dueDate,
                status = InvoiceStatus.unpaid,
                discountType = state.discountType,
                discountAmountCents = state.discountAmountCents.toLong().toString(),
                discountPercent = state.discountPercent,
                surchargeType = state.surchargeType,
                surchargeAmountCents = state.surchargeAmountCents.toLong().toString(),
                surchargePercent = state.surchargePercent,
                shippingFeeCents = state.shippingFeeCents.toLong().toString(),
                taxRate = state.globalTaxRate,
                taxType = state.globalTaxType,
                customerNotes = state.customerNotes.ifBlank { null },
                termsConditionNotes = state.termsNotes.ifBlank { null }
            )

            val lineItems = state.lineItems.map {
                InvoiceLineItem(
                    itemId = it.itemId,
                    quantity = it.quantity.toString(),
                    taxRate = it.taxRate,
                    taxType = it.taxType
                )
            }

            val currencyModel = Currency(
                id = currency.id,
                code = currency.code,
                symbol = currency.symbol,
                text = currency.text,
                format = currency.format,
                subunit = currency.subunit
            )

            val bankModel = state.selectedBank?.let {
                Bank(
                    id = it.id,
                    name = it.name,
                    bankName = it.bankName,
                    accountNumber = it.accountNumber,
                    swiftCode = it.swiftCode,
                    address = it.address,
                    upiCode = it.upiCode
                )
            }

            invoiceRepository.createInvoice(
                CreateInvoiceParam(
                    invoice = invoice,
                    items = lineItems,
                    business = business,
                    client = client,
                    currency = currencyModel,
                    bank = bankModel,
                    layoutSchemaJson = null
                )
            )

            _uiState.update { it.copy(isSaving = false, isSuccess = true) }
            onSuccess()
        }
    }
}
