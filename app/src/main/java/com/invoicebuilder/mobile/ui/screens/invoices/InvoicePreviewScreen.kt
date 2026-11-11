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
import com.invoicebuilder.core.einvoice.UblXmlGenerator
import com.invoicebuilder.core.model.Business
import com.invoicebuilder.core.model.Client
import com.invoicebuilder.core.model.CurrencyFormat
import com.invoicebuilder.core.model.Invoice
import com.invoicebuilder.core.pdf.FacturXAttacher
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
    var hybridPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var xmlContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(invoice.id) {
        withContext(Dispatchers.IO) {
            // 1. Generate base PDF
            val basePdf = PdfGenerator.generatePdf(
                PdfInvoiceData(
                    invoice = invoice,
                    business = business,
                    client = client,
                    currencySymbol = currencySymbol,
                    items = items,
                    itemNames = itemNames
                )
            )

            // 2. Generate UBL 2.1 XML
            val xml = UblXmlGenerator.generateUblXml(
                invoice = invoice,
                business = business,
                client = client,
                currencyCode = CurrencyFormat.CODE,
                items = items
            )
            xmlContent = xml

            // 3. Attach XML to PDF creating Factur-X / ZUGFeRD hybrid invoice
            val hybridPdf = FacturXAttacher.attachXml(basePdf, xml.toByteArray(Charsets.UTF_8))
            hybridPdfBytes = hybridPdf
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
                            "PDF & E-Invoice Preview",
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
                            hybridPdfBytes?.let { bytes ->
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
                            "Generating hybrid PDF (Factur-X & UBL)...",
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
                            "Hybrid E-Invoice PDF Ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Embedded with PEPPOL BIS 3.0 / XRechnung UBL XML",
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
                            hybridPdfBytes?.let { bytes ->
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
                            xmlContent?.let { xml ->
                                shareXml(context, xml, "ubl_${invoice.fullInvoiceNumber}.xml")
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export XML")
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

private fun shareXml(context: Context, xmlString: String, filename: String) {
    val cachePath = File(context.cacheDir, "invoices")
    cachePath.mkdirs()
    val file = File(cachePath, filename)
    FileOutputStream(file).use { it.write(xmlString.toByteArray(Charsets.UTF_8)) }

    val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/xml"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share UBL XML"))
}
