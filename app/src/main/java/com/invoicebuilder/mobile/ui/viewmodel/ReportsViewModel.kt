package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.database.repository.InvoiceRepository
import com.invoicebuilder.core.model.Invoice
import com.invoicebuilder.core.model.InvoiceStatus
import com.invoicebuilder.core.model.InvoiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MonthlyTrend(
    val monthLabel: String,
    val revenueCents: Double
)

data class ReportsUiState(
    val totalInvoicesCount: Int = 0,
    val totalRevenueCents: Double = 0.0,
    val totalReceivablesCents: Double = 0.0,
    val paidCount: Int = 0,
    val unpaidCount: Int = 0,
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = invoiceRepository.observeInvoices(InvoiceType.invoice)
        .map { invoices ->
            calculateMetrics(invoices)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState(isLoading = true))

    private fun calculateMetrics(invoices: List<Invoice>): ReportsUiState {
        var totalRev = 0.0
        var totalRec = 0.0
        var paidCount = 0
        var unpaidCount = 0

        val monthlyMap = mutableMapOf<String, Double>()

        for (inv in invoices) {
            val amount = (inv.discountAmountCents.toDoubleOrNull() ?: 0.0) // placeholder
            val month = if (inv.issuedAt.length >= 7) inv.issuedAt.take(7) else "Recent"

            when (inv.status) {
                InvoiceStatus.paid -> {
                    paidCount++
                    totalRev += 10000.0 // Simulated line totals or calculated
                    monthlyMap[month] = (monthlyMap[month] ?: 0.0) + 10000.0
                }
                InvoiceStatus.unpaid, InvoiceStatus.open, InvoiceStatus.partially -> {
                    unpaidCount++
                    totalRec += 10000.0
                }
                else -> Unit
            }
        }

        val trends = monthlyMap.entries.map {
            MonthlyTrend(monthLabel = it.key, revenueCents = it.value)
        }.sortedBy { it.monthLabel }

        return ReportsUiState(
            totalInvoicesCount = invoices.size,
            totalRevenueCents = totalRev,
            totalReceivablesCents = totalRec,
            paidCount = paidCount,
            unpaidCount = unpaidCount,
            monthlyTrends = trends,
            isLoading = false
        )
    }
}
