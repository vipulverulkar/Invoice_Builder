package com.invoicebuilder.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invoicebuilder.core.database.dao.SettingsDao
import com.invoicebuilder.core.database.entity.SettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = settingsDao.observeSettings()
        .filterNotNull()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsEntity()
        )

    fun updatePrefix(prefix: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(invoicePrefix = prefix))
        }
    }

    fun updateSuffix(suffix: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(invoiceSuffix = suffix))
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(language = lang))
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(isDarkMode = enabled))
        }
    }

    fun toggleReceiptPrinting(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(receiptPrintingOn = enabled))
        }
    }

    /** Enables GST registration and e-invoice (IRN) support. */
    fun toggleGstRegistration(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(gstRegistrationOn = enabled))
        }
    }

    /** Financial year used for GST invoice numbering, e.g. "2025-26". */
    fun updateFinancialYear(financialYear: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(financialYear = financialYear.ifBlank { null }))
        }
    }

    /** Default place of supply (GST state code) for new invoices. */
    fun updateDefaultStateCode(stateCode: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(defaultStateCode = stateCode.ifBlank { null }))
        }
    }
}
