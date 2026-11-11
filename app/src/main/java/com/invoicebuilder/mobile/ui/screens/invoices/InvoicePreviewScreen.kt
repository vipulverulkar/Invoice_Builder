package com.invoicebuilder.mobile.ui.screens.invoices

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.invoicebuilder.core.calculator.ItemCalculationInput
import com.invoicebuilder.core.gstn.GstEInvoiceJsonBuilder
import com.invoicebuilder.core.model.Business
import com.invoicebuilder.core.model.Client
import com.invoicebuilder.core.model.CurrencyFormat
import com.invoicebuilder.core.model.GstSupplyType
import com.invoicebuilder.core.model.Invoice
import com.invoicebuilder.core.model.Money
import com.invoicebuilder.core.pdf.PdfGenerator
import com.invoicebuilder.core.pdf.PdfInvoiceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    invoice: Invoice,
    business: Business,
    client: Client,
    currencySymbol: String = CurrencyFormat.SYMBOL,
    items: List<ItemCalculationInput>,
    itemNames: List<String>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isGenerating by remember { mutableStateOf(true) }
    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var eInvoiceJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(invoice.id) {
        withContext(Dispatchers.IO) {
            val supplyType = if (!invoice.placeOfSupply.isNullOrBlank() &&
                business.stateCode != null &&
                invoice.placeOfSupply != business.stateCode
            ) {
                GstSupplyType.InterState
            } else {
                GstSupplyType.IntraState
            }

            // 1. GST compliant tax invoice PDF
            pdfBytes = PdfGenerator.generatePdf(
                PdfInvoiceData(
                    invoice = invoice,
                    business = business,
                    client = client,
                    currencySymbol = currencySymbol,
                    items = items,
                    itemNames = itemNames,
                    supplyType = supplyType
                )
            )

            // 2. GST e-invoice payload (uploadable through the GSTN offline utility)
            eInvoiceJson = GstEInvoiceJsonBuilder.build(
                GstEInvoiceJsonBuilder.Document(
                    gstin = business.gstin.orEmpty(),
                    legalName = business.name,
                    invoiceNumber = invoice.fullInvoiceNumber,
                    invoiceDate = invoice.issuedAt.take(10),
                    invoiceType = invoice.invoiceKind?.name ?: "TaxInvoice",
                    placeOfSupplyCode = invoice.placeOfSupply ?: business.stateCode.orEmpty(),
                    reverseCharge = invoice.reverseCharge,
                    buyerGstin = client.gstin,
                    buyerName = client.name,
                    buyerAddress = client.address,
                    buyerStateCode = client.stateCode,
                    lines = items.mapIndexed { index, item ->
                        GstEInvoiceJsonBuilder.Line(
                            itemName = itemNames.getOrElse(index) { item.itemName },
                            hsnCode = item.hsnCode.orEmpty(),
                            quantity = item.quantity,
                            unitPricePaise = item.unitPrice.paise,
                            discountPaise = 0,
                            taxableValuePaise = item.unitPrice.paise * item.quantity.toLong(),
                            gstRate = item.taxRate ?: 0.0,
                            totalPaise = item.unitPrice.paise * item.quantity.toLong()
                        )
                    },
                    totalPaise = Money.ofCents(invoice.totalCents).paise,
                    otherTerms = invoice.customerNotes
                )
            )

            isGenerating = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Invoice ${invoice.fullInvoiceNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tax Invoice & E-Invoice Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            pdfBytes?.let { bytes ->
                                sharePdf(context, bytes, "Invoice_${invoice.fullInvoiceNumber}.pdf")
                            }
                        },
                        enabled = !isGenerating
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isGenerating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Text(
                            "Generating GST Tax Invoice...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            "GST Tax Invoice Ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Export the PDF for your customer or the JSON for the GSTN portal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pdfBytes?.let { bytes ->
                                sharePdf(context, bytes, "Invoice_${invoice.fullInvoiceNumber}.pdf")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            eInvoiceJson?.let { json ->
                                shareJson(context, json, "einvoice_${invoice.fullInvoiceNumber}.json")
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: Context, pdfBytes: ByteArray, filename: String) {
    val cachePath = File(context.cacheDir, "invoices")
    cachePath.mkdirs()
    val file = File(cachePath, filename)
    FileOutputStream(file).use { it.write(pdfBytes) }

    val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
}

private fun shareJson(context: Context, json: String, filename: String) {
    val cachePath = File(context.cacheDir, "invoices")
    cachePath.mkdirs()
    val file = File(cachePath, filename)
    FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }

    val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share E-Invoice JSON"))
}
