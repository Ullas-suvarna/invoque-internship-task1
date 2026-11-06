package com.example.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.BudgetProgressCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetItemUi
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel
) {
    val budgetData by budgetViewModel.budgetUiData.collectAsState()
    val categories by budgetViewModel.expenseCategories.collectAsState()
    val currencySymbol by budgetViewModel.currencySymbol.collectAsState()

    var showSetMonthlyBudgetDialog by remember { mutableStateOf(false) }
    var showAddCategoryBudgetDialog by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<BudgetItemUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryBudgetDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("budget_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("budget_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Monthly Budget Header & Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Total Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showSetMonthlyBudgetDialog = true }) {
                        Text(
                            text = if (budgetData.overallBudget == null) "+ Set Budget" else "Edit Budget",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (budgetData.overallBudget != null) {
                val overall = budgetData.overallBudget!!
                item {
                    BudgetProgressCard(
                        categoryOrTitle = "Total Monthly Limit",
                        spentAmount = overall.spentAmount,
                        limitAmount = overall.limitAmount,
                        currencySymbol = currencySymbol
                    )

                    // Threshold alert notices
                    if (overall.percentage >= 80) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (overall.percentage >= 100) ExpenseRed.copy(alpha = 0.12f)
                                else WarningAmber.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (overall.percentage >= 100) ExpenseRed else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        overall.percentage >= 100 -> "Alert: You have exceeded your total monthly budget!"
                                        overall.percentage >= 90 -> "Warning: 90% of your total monthly budget consumed."
                                        else -> "Caution: 80% of your total monthly budget consumed."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (overall.percentage >= 100) ExpenseRed else WarningAmber
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSetMonthlyBudgetDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Total Monthly Budget Set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Set an overall spending limit for this month to receive smart threshold warnings at 80%, 90% & 100%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showSetMonthlyBudgetDialog = true }) {
                                Text("Set Monthly Budget")
                            }
                        }
                    }
                }
            }

            // Category Budgets Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showAddCategoryBudgetDialog = true }) {
                        Text("+ Add Category Budget", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (budgetData.categoryBudgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No category budgets configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(budgetData.categoryBudgets, key = { it.budgetId }) { item ->
                    Column {
                        BudgetProgressCard(
                            categoryOrTitle = item.categoryOrTotal,
                            spentAmount = item.spentAmount,
                            limitAmount = item.limitAmount,
                            currencySymbol = currencySymbol
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { budgetToDelete = item }) {
                                Text("Remove", color = ExpenseRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Set Monthly Budget Dialog
    if (showSetMonthlyBudgetDialog) {
        var inputAmount by remember {
            mutableStateOf(budgetData.overallBudget?.limitAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
        }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetMonthlyBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter overall spending limit for this month in $currencySymbol:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                                inputAmount = it
                                errorMsg = null
                            }
                        },
                        placeholder = { Text("e.g. 25000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input")
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMsg = "Please enter a valid amount greater than 0"
                            return@Button
                        }
                        budgetViewModel.setOverallMonthlyBudget(amount)
                        showSetMonthlyBudgetDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetMonthlyBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Category Budget Dialog
    if (showAddCategoryBudgetDialog) {
        var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Food") }
        var inputAmount by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCategoryBudgetDialog = false },
            title = { Text("Set Category Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose Category:", style = MaterialTheme.typography.labelMedium)

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCat,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCat = cat.name
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Limit Amount ($currencySymbol):", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                                inputAmount = it
                                errorMsg = null
                            }
                        },
                        placeholder = { Text("e.g. 5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("category_budget_input")
                    )

                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMsg = "Please enter a valid amount greater than 0"
                            return@Button
                        }
                        budgetViewModel.setCategoryBudget(selectedCat, amount)
                        showAddCategoryBudgetDialog = false
                    }
                ) {
                    Text("Set Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (budgetToDelete != null) {
        val b = budgetToDelete!!
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete budget for \"${b.categoryOrTotal}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        budgetViewModel.deleteBudget(b.budgetId)
                        budgetToDelete = null
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
