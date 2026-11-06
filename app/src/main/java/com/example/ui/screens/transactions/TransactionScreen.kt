package com.example.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.DateRangeFilter
import com.example.ui.viewmodel.GroupingMode
import com.example.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit
) {
    val filterState by transactionViewModel.filterState.collectAsState()
    val groupedList by transactionViewModel.filteredGroupedTransactions.collectAsState()
    val categories by transactionViewModel.categories.collectAsState()
    val currencySymbol by transactionViewModel.currencySymbol.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("tx_screen_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("transactions_screen")
        ) {
            // Header & Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (filterState.typeFilter != "ALL" || filterState.categoryFilter != "ALL" || filterState.dateRangeFilter != DateRangeFilter.ALL)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { transactionViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by title or description...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { transactionViewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tx_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Filter Chips (All, Expense, Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.typeFilter == "ALL",
                        onClick = { transactionViewModel.setTypeFilter("ALL") },
                        label = { Text("All") },
                        modifier = Modifier.testTag("chip_all")
                    )
                    FilterChip(
                        selected = filterState.typeFilter == "EXPENSE",
                        onClick = { transactionViewModel.setTypeFilter("EXPENSE") },
                        label = { Text("Expenses") },
                        modifier = Modifier.testTag("chip_expense")
                    )
                    FilterChip(
                        selected = filterState.typeFilter == "INCOME",
                        onClick = { transactionViewModel.setTypeFilter("INCOME") },
                        label = { Text("Income") },
                        modifier = Modifier.testTag("chip_income")
                    )

                    // Active Category indicator if selected
                    if (filterState.categoryFilter != "ALL") {
                        FilterChip(
                            selected = true,
                            onClick = { transactionViewModel.setCategoryFilter("ALL") },
                            label = { Text("Cat: ${filterState.categoryFilter} ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Active Range indicator
                    if (filterState.dateRangeFilter != DateRangeFilter.ALL) {
                        FilterChip(
                            selected = true,
                            onClick = { transactionViewModel.setDateRangeFilter(DateRangeFilter.ALL) },
                            label = { Text("Date: ${filterState.dateRangeFilter.name} ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Grouped Transactions List
            if (groupedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search terms or filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedList.forEach { group ->
                        item(key = "header_${group.groupHeader}") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.groupHeader,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${group.items.size} item${if (group.items.size > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(group.items, key = { it.transactionId }) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                currencySymbol = currencySymbol,
                                onEdit = { onNavigateToEditTransaction(tx.transactionId) },
                                onDelete = { transactionToDelete = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Options Bottom Sheet
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Group Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        transactionViewModel.clearFilters()
                        showFilterSheet = false
                    }) {
                        Text("Reset All")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Range Options
                Text("Date Period", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeFilter.values().forEach { range ->
                        FilterChip(
                            selected = filterState.dateRangeFilter == range,
                            onClick = { transactionViewModel.setDateRangeFilter(range) },
                            label = { Text(range.name.replace("_", " ")) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grouping Mode
                Text("Grouping", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupingMode.values().forEach { mode ->
                        FilterChip(
                            selected = filterState.groupingMode == mode,
                            onClick = { transactionViewModel.setGroupingMode(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Filter
                Text("Filter by Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.categoryFilter == "ALL",
                        onClick = { transactionViewModel.setCategoryFilter("ALL") },
                        label = { Text("All Categories") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = filterState.categoryFilter == cat.name,
                            onClick = { transactionViewModel.setCategoryFilter(cat.name) },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete \"${tx.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionViewModel.deleteTransaction(tx.transactionId)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
