package com.invoicebuilder.mobile

import android.app.Application
import com.invoicebuilder.core.pdf.FacturXAttacher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InvoiceBuilderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FacturXAttacher.init(this)
    }
}
