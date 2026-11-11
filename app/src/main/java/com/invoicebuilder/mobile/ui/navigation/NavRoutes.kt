package com.invoicebuilder.mobile.ui.navigation

sealed class Screen(val route: String, val titleRes: String) {
    object Invoices : Screen("invoices", "Invoices")
    object Quotations : Screen("quotations", "Quotations")
    object Clients : Screen("clients", "Clients")
    object Businesses : Screen("businesses", "Businesses")
    object Items : Screen("items", "Items")
    object Reports : Screen("reports", "Reports")
    object Settings : Screen("settings", "Settings")
    object InvoiceCreate : Screen("invoice_create", "New Invoice")
}
