package com.invoicebuilder.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.invoicebuilder.mobile.ui.components.AppLogo
import com.invoicebuilder.mobile.ui.screens.clients.ClientListScreen
import com.invoicebuilder.mobile.ui.screens.invoices.InvoiceFormScreen
import com.invoicebuilder.mobile.ui.screens.invoices.InvoiceListScreen
import com.invoicebuilder.mobile.ui.screens.items.ItemListScreen
import com.invoicebuilder.mobile.ui.screens.reports.ReportsScreen
import com.invoicebuilder.mobile.ui.screens.settings.SettingsScreen
import com.invoicebuilder.mobile.ui.theme.InvoiceBuilderTheme
import com.invoicebuilder.mobile.ui.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            InvoiceBuilderTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    var isCreatingInvoice by remember { mutableStateOf(false) }

    if (isCreatingInvoice) {
        val formViewModel: InvoiceFormViewModel = hiltViewModel()
        InvoiceFormScreen(
            viewModel = formViewModel,
            onNavigateBack = { isCreatingInvoice = false }
        )
    } else {
        MainAppScaffold(
            onCreateInvoice = { isCreatingInvoice = true }
        )
    }
}

private data class NavTab(
    val title: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navTabs = listOf(
    NavTab(
        title = "Invoices",
        subtitle = "Billing & Quotations",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    ),
    NavTab(
        title = "Clients",
        subtitle = "Directory & CRM",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    NavTab(
        title = "Items",
        subtitle = "Products & Services",
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    ),
    NavTab(
        title = "Reports",
        subtitle = "Financial Analytics",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    NavTab(
        title = "Settings",
        subtitle = "System Preferences",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    onCreateInvoice: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val activeTab = navTabs[selectedTab]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            AppLogo(
                                size = 34.dp,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                        Column {
                            Text(
                                text = activeTab.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeTab.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                navTabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "main_tab_content"
        ) { tabIndex ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (tabIndex) {
                    0 -> {
                        val invoiceListViewModel: InvoiceListViewModel = hiltViewModel()
                        InvoiceListScreen(
                            viewModel = invoiceListViewModel,
                            onCreateNewInvoice = onCreateInvoice
                        )
                    }
                    1 -> {
                        val clientViewModel: ClientViewModel = hiltViewModel()
                        ClientListScreen(viewModel = clientViewModel)
                    }
                    2 -> {
                        val itemViewModel: ItemViewModel = hiltViewModel()
                        ItemListScreen(viewModel = itemViewModel)
                    }
                    3 -> {
                        val reportsViewModel: ReportsViewModel = hiltViewModel()
                        ReportsScreen(viewModel = reportsViewModel)
                    }
                    else -> {
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
