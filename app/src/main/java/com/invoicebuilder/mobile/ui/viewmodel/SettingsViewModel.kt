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

    fun toggleEInvoice(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.insertOrUpdate(current.copy(ublON = enabled, xrechnungON = enabled))
        }
    }
}
