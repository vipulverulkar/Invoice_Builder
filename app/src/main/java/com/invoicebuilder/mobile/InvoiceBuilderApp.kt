package com.invoicebuilder.mobile

import android.app.Application
import com.invoicebuilder.core.database.dao.BusinessDao
import com.invoicebuilder.core.database.dao.CurrencyDao
import com.invoicebuilder.core.database.entity.BusinessEntity
import com.invoicebuilder.core.database.entity.CurrencyEntity
import com.invoicebuilder.core.model.CurrencyFormat
import com.invoicebuilder.core.pdf.FacturXAttacher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class InvoiceBuilderApp : Application() {

    @Inject
    lateinit var currencyDao: CurrencyDao

    @Inject
    lateinit var businessDao: BusinessDao

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        FacturXAttacher.init(this)
        appScope.launch { seedDefaults() }
    }

    /**
     * Guarantees the minimum data required to issue an invoice:
     * an INR currency and a business profile (both editable later on).
     */
    private suspend fun seedDefaults() {
        if (currencyDao.observeActiveCurrencies().first().isEmpty()) {
            currencyDao.insert(
                CurrencyEntity(
                    code = CurrencyFormat.CODE,
                    symbol = CurrencyFormat.SYMBOL,
                    text = "Indian Rupee",
                    format = CurrencyFormat.LOCALE_TAG,
                    subunit = 100
                )
            )
        }
        if (businessDao.observeActiveBusinesses().first().isEmpty()) {
            businessDao.insert(
                BusinessEntity(
                    name = "My Business",
                    shortName = "MB",
                    address = "",
                    countryCode = "IN"
                )
            )
        }
    }
}
