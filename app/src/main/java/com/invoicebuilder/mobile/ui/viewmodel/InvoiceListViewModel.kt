package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.database.repository.InvoiceRepository
import com.invoicebuilder.core.model.Invoice
import com.invoicebuilder.core.model.InvoiceStatus
import com.invoicebuilder.core.model.InvoiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceListUiState(
    val invoices: List<Invoice> = emptyList(),
    val selectedType: InvoiceType = InvoiceType.invoice,
    val selectedStatus: InvoiceStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        loadInvoices(InvoiceType.invoice)
    }

    fun setInvoiceType(type: InvoiceType) {
        _uiState.update { it.copy(selectedType = type) }
        loadInvoices(type)
    }

    fun setStatusFilter(status: InvoiceStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun loadInvoices(type: InvoiceType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            invoiceRepository.observeInvoices(type).collect { list ->
                _uiState.update { it.copy(invoices = list, isLoading = false) }
            }
        }
    }

    fun archiveInvoice(invoiceId: Long) {
        viewModelScope.launch {
            invoiceRepository.archiveInvoice(invoiceId)
        }
    }
}
