package com.invoicebuilder.mobile.ui.screens.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invoicebuilder.core.model.InvoiceStatus
import com.invoicebuilder.mobile.ui.theme.getInvoiceStatusColors
import com.invoicebuilder.mobile.ui.viewmodel.ReportsViewModel
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val paidColors = getInvoiceStatusColors(InvoiceStatus.paid)
    val unpaidColors = getInvoiceStatusColors(InvoiceStatus.unpaid)
    val collectionRate = if (uiState.totalInvoicesCount > 0) {
        uiState.paidCount.toFloat() / uiState.totalInvoicesCount.toFloat()
    } else {
        0f
    }
    val averageInvoiceCents = if (uiState.paidCount > 0) {
        uiState.totalRevenueCents / uiState.paidCount
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Gradient Financial Overview Card
        item {
            HeroFinancialCard(
                totalRevenueCents = uiState.totalRevenueCents,
                totalInvoicesCount = uiState.totalInvoicesCount,
                paidCount = uiState.paidCount
            )
        }

        // Top Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Collected",
                    amount = String.format(Locale.US, "$%.2f", uiState.totalRevenueCents / 100.0),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = paidColors.text,
                    badgeBgColor = paidColors.background
                )

                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Receivables",
                    amount = String.format(Locale.US, "$%.2f", uiState.totalReceivablesCents / 100.0),
                    icon = Icons.Outlined.ErrorOutline,
                    accentColor = unpaidColors.text,
                    badgeBgColor = unpaidColors.background
                )
            }
        }

        // Collection performance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Collection Rate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Share of invoices fully settled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${(collectionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = paidColors.text
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val animatedRate by animateFloatAsState(
                        targetValue = collectionRate.coerceIn(0f, 1f),
                        label = "collection_rate"
                    )
                    LinearProgressIndicator(
                        progress = { animatedRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = paidColors.text,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Average invoice",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "$%.2f", averageInvoiceCents / 100.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Settled invoices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.paidCount} of ${uiState.totalInvoicesCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Status Count Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Invoice Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "${uiState.totalInvoicesCount} Total",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InvoiceDistributionBar(
                        paidCount = uiState.paidCount,
                        unpaidCount = uiState.unpaidCount,
                        totalCount = uiState.totalInvoicesCount,
                        paidColor = paidColors.text,
                        unpaidColor = unpaidColors.text
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ModernCountBadge(
                            label = "Total",
                            count = uiState.totalInvoicesCount,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ModernCountBadge(
                            label = "Paid",
                            count = uiState.paidCount,
                            color = paidColors.text
                        )
                        ModernCountBadge(
                            label = "Unpaid",
                            count = uiState.unpaidCount,
                            color = unpaidColors.text
                        )
                    }
                }
            }
        }

        // Monthly Trends Visual Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Monthly Revenue Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (uiState.monthlyTrends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No revenue history available yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val maxRevenue = uiState.monthlyTrends.maxOfOrNull { it.revenueCents } ?: 1.0

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.monthlyTrends.forEach { trend ->
                                val targetFraction = (trend.revenueCents / maxRevenue).toFloat().coerceIn(0.04f, 1f)
                                val fraction by animateFloatAsState(
                                    targetValue = targetFraction,
                                    label = "trend_${trend.monthLabel}"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatMonthLabel(trend.monthLabel),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = String.format(Locale.US, "$%.0f", trend.revenueCents / 100.0),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(56.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDistributionBar(
    paidCount: Int,
    unpaidCount: Int,
    totalCount: Int,
    paidColor: Color,
    unpaidColor: Color
) {
    val settled = if (totalCount > 0) paidCount.toFloat() / totalCount else 0f
    val open = if (totalCount > 0) unpaidCount.toFloat() / totalCount else 0f
    val animatedSettled by animateFloatAsState(targetValue = settled, label = "settled_share")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (animatedSettled > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedSettled.coerceAtLeast(0.001f))
                    .background(paidColor)
            )
        }
        if (open > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(open.coerceAtLeast(0.001f))
                    .background(unpaidColor)
            )
        }
    }
}

@Composable
fun HeroFinancialCard(
    totalRevenueCents: Double,
    totalInvoicesCount: Int,
    paidCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3730A3),
                            Color(0xFF4F46E5),
                            Color(0xFF0D9488)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL REVENUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "$paidCount / $totalInvoicesCount Paid",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = String.format(Locale.US, "$%.2f", totalRevenueCents / 100.0),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lifetime settled invoices volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun ModernMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badgeBgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ModernCountBadge(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ReportsScreenPreview() {
    com.invoicebuilder.mobile.ui.theme.InvoiceBuilderTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroFinancialCard(
                totalRevenueCents = 1845000.0,
                totalInvoicesCount = 24,
                paidCount = 19
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val paid = getInvoiceStatusColors(InvoiceStatus.paid)
                val unpaid = getInvoiceStatusColors(InvoiceStatus.unpaid)
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Collected",
                    amount = "$18,450.00",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = paid.text,
                    badgeBgColor = paid.background
                )

                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Receivables",
                    amount = "$3,250.00",
                    icon = Icons.Outlined.ErrorOutline,
                    accentColor = unpaid.text,
                    badgeBgColor = unpaid.background
                )
            }
        }
    }
}

private fun formatMonthLabel(raw: String): String {
    if (raw.length < 7) return raw
    return try {
        val year = raw.substring(0, 4)
        val month = raw.substring(5, 7).toInt()
        val names = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        "${names[(month - 1).coerceIn(0, 11)]} ${year.takeLast(2)}"
    } catch (e: Exception) {
        raw
    }
}

