package com.invoicebuilder.mobile.ui.screens.invoices

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.invoicebuilder.core.model.Invoice
import com.invoicebuilder.core.model.InvoiceStatus
import com.invoicebuilder.core.model.InvoiceType
import com.invoicebuilder.mobile.ui.theme.getInvoiceStatusColors
import com.invoicebuilder.mobile.ui.viewmodel.InvoiceListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    viewModel: InvoiceListViewModel,
    onCreateNewInvoice: () -> Unit = {},
    onInvoiceClick: (Invoice) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNewInvoice,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        if (uiState.selectedType == InvoiceType.invoice) "New Invoice" else "New Quotation",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Type Selector (Invoices vs Quotations)
            InvoiceTypeSegmentedControl(
                selectedType = uiState.selectedType,
                onTypeSelected = { viewModel.setInvoiceType(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // At-a-glance summary of the current document series
            InvoiceSummaryStrip(
                invoices = uiState.invoices,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        "Search by invoice # or date...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAllSelected = uiState.selectedStatus == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                items(InvoiceStatus.entries.toTypedArray()) { status ->
                    val isSelected = uiState.selectedStatus == status
                    val statusColors = getInvoiceStatusColors(status)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(status) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColors.dot)
                            )
                        },
                        label = {
                            Text(
                                status.name.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = statusColors.background,
                            selectedLabelColor = statusColors.text
                        )
                    )
                }
            }

            // List of Invoices filtered by status and search query
            val filteredList = uiState.invoices.filter { invoice ->
                val matchesStatus = uiState.selectedStatus == null || invoice.status == uiState.selectedStatus
                val matchesQuery = uiState.searchQuery.isBlank() ||
                    invoice.fullInvoiceNumber.contains(uiState.searchQuery, ignoreCase = true) ||
                    invoice.issuedAt.contains(uiState.searchQuery, ignoreCase = true) ||
                    (invoice.dueDate?.contains(uiState.searchQuery, ignoreCase = true) == true)
                matchesStatus && matchesQuery
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else if (filteredList.isEmpty()) {
                ModernEmptyInvoiceState(
                    invoiceType = uiState.selectedType,
                    hasFilters = uiState.selectedStatus != null || uiState.searchQuery.isNotBlank(),
                    onResetFilters = {
                        viewModel.setStatusFilter(null)
                        viewModel.setSearchQuery("")
                    },
                    onCreate = onCreateNewInvoice
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { invoice ->
                        ModernInvoiceCardItem(
                            invoice = invoice,
                            onClick = { onInvoiceClick(invoice) },
                            onArchive = { viewModel.archiveInvoice(invoice.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceSummaryStrip(
    invoices: List<Invoice>,
    modifier: Modifier = Modifier
) {
    val total = invoices.size
    val paid = invoices.count { it.status == InvoiceStatus.paid }
    val due = invoices.count {
        it.status == InvoiceStatus.unpaid ||
            it.status == InvoiceStatus.partially ||
            it.status == InvoiceStatus.open
    }
    val paidColors = getInvoiceStatusColors(InvoiceStatus.paid)
    val dueColors = getInvoiceStatusColors(InvoiceStatus.unpaid)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMetric(
                label = "Total",
                value = total,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer
            )
            SummaryDivider()
            SummaryMetric(
                label = "Paid",
                value = paid,
                icon = Icons.Outlined.CheckCircle,
                tint = paidColors.text,
                container = paidColors.background
            )
            SummaryDivider()
            SummaryMetric(
                label = "Due",
                value = due,
                icon = Icons.Outlined.Schedule,
                tint = dueColors.text,
                container = dueColors.background
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
fun InvoiceTypeSegmentedControl(
    selectedType: InvoiceType,
    onTypeSelected: (InvoiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val types = listOf(InvoiceType.invoice to "Invoices", InvoiceType.quotation to "Quotations")
            types.forEach { (type, title) ->
                val isSelected = selectedType == type
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tab_text"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTypeSelected(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = backgroundColor,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernInvoiceCardItem(
    invoice: Invoice,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = getInvoiceStatusColors(invoice.status)
    val overdue = isInvoiceOverdue(invoice)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (invoice.status) {
                                InvoiceStatus.paid -> Icons.Outlined.CheckCircle
                                InvoiceStatus.unpaid -> Icons.Outlined.AccessTime
                                InvoiceStatus.partially -> Icons.Outlined.HourglassBottom
                                InvoiceStatus.open -> Icons.Outlined.Drafts
                                InvoiceStatus.closed -> Icons.Outlined.Lock
                            },
                            contentDescription = null,
                            tint = statusColors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = invoice.fullInvoiceNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (invoice.invoiceType == InvoiceType.quotation) "Quotation" else "Tax Invoice",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ModernStatusBadge(status = invoice.status)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatInvoiceDate(invoice.issuedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    invoice.dueDate?.let { dueDate ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (overdue) Icons.Outlined.ErrorOutline else Icons.Outlined.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (overdue) {
                                    "Overdue · ${formatInvoiceDate(dueDate)}"
                                } else {
                                    "Due: ${formatInvoiceDate(dueDate)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = "Archive",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernStatusBadge(status: InvoiceStatus) {
    val statusColors = getInvoiceStatusColors(status)

    Surface(
        color = statusColors.background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColors.dot)
            )
            Text(
                text = status.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColors.text
            )
        }
    }
}

@Composable
fun ModernEmptyInvoiceState(
    invoiceType: InvoiceType,
    hasFilters: Boolean,
    onResetFilters: () -> Unit,
    onCreate: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (invoiceType == InvoiceType.invoice) Icons.Outlined.Receipt else Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (hasFilters) "No Matching Records" else "No ${invoiceType.name.replaceFirstChar { it.uppercase() }}s Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (hasFilters) "Try adjusting your search query or status filter." else "Generate professional, compliant ${invoiceType.name}s in seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasFilters) {
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.FilterAltOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Filters")
                }
            } else {
                Button(
                    onClick = onCreate,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create ${invoiceType.name.replaceFirstChar { it.uppercase() }}")
                }
            }
        }
    }
}

private val invoiceDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)

private fun formatInvoiceDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val datePart = raw.take(10)
    return try {
        LocalDate.parse(datePart).format(invoiceDateFormatter)
    } catch (e: Exception) {
        datePart
    }
}

private fun isInvoiceOverdue(invoice: Invoice): Boolean {
    if (invoice.status == InvoiceStatus.paid || invoice.status == InvoiceStatus.closed) return false
    val due = invoice.dueDate?.take(10) ?: return false
    return try {
        LocalDate.parse(due).isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ModernInvoiceCardPreview() {
    com.invoicebuilder.mobile.ui.theme.InvoiceBuilderTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernInvoiceCardItem(
                invoice = Invoice(
                    id = 1,
                    businessId = 1,
                    clientId = 1,
                    currencyId = 1,
                    invoiceNumber = "1042",
                    invoicePrefix = "INV-",
                    status = InvoiceStatus.paid,
                    issuedAt = "2026-11-10T10:00:00Z",
                    dueDate = "2026-11-25T10:00:00Z"
                ),
                onClick = {},
                onArchive = {}
            )

            ModernInvoiceCardItem(
                invoice = Invoice(
                    id = 2,
                    businessId = 1,
                    clientId = 2,
                    currencyId = 1,
                    invoiceNumber = "1043",
                    invoicePrefix = "INV-",
                    status = InvoiceStatus.unpaid,
                    issuedAt = "2026-11-01T10:00:00Z",
                    dueDate = "2026-11-08T10:00:00Z"
                ),
                onClick = {},
                onArchive = {}
            )
        }
    }
}
