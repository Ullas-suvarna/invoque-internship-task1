package com.example.ui.screens.recurring

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.domain.model.RecurringFrequency
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.RecurringViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    recurringViewModel: RecurringViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val recurringList by recurringViewModel.recurringList.collectAsState()
    val categories by recurringViewModel.categories.collectAsState()
    val currencySymbol by recurringViewModel.currencySymbol.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { recurringViewModel.triggerProcessDue() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Process Due Now")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Schedule")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scheduled transactions are automatically recorded when due without duplicates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (recurringList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No recurring schedules yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the + button below to set up recurring salaries, rent, subscriptions, or bills.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recurringList, key = { it.recurringId }) { rec ->
                    val isIncome = rec.type == TransactionType.INCOME.name
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            if (isIncome) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(rec.category),
                                        contentDescription = rec.category,
                                        tint = if (isIncome) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = rec.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = rec.frequency,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Next: ${DateUtils.formatDate(rec.nextDueDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatAmount(rec.amount, currencySymbol)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) IncomeGreen else ExpenseRed
                                )
                                IconButton(onClick = { itemToDelete = rec }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Recurring Schedule Dialog
    if (showAddDialog) {
        var recTitle by remember { mutableStateOf("") }
        var recAmountText by remember { mutableStateOf("") }
        var recType by remember { mutableStateOf(TransactionType.EXPENSE) }
        var recCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Bills") }
        var recFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
        var recStartDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var recDesc by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var freqDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Recurring Schedule") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = if (recType == TransactionType.EXPENSE) 0 else 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = recType == TransactionType.EXPENSE,
                            onClick = { recType = TransactionType.EXPENSE },
                            text = { Text("Expense", color = if (recType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        Tab(
                            selected = recType == TransactionType.INCOME,
                            onClick = { recType = TransactionType.INCOME },
                            text = { Text("Income", color = if (recType == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }

                    OutlinedTextField(
                        value = recTitle,
                        onValueChange = { recTitle = it; errorMsg = null },
                        label = { Text("Title (e.g. Netflix, Apartment Rent)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recAmountText,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
                                recAmountText = it
                                errorMsg = null
                            }
                        },
                        label = { Text("Amount ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Frequency selector
                    ExposedDropdownMenuBox(
                        expanded = freqDropdownExpanded,
                        onExpandedChange = { freqDropdownExpanded = !freqDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = recFrequency.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = freqDropdownExpanded,
                            onDismissRequest = { freqDropdownExpanded = false }
                        ) {
                            RecurringFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.name) },
                                    onClick = {
                                        recFrequency = freq
                                        freqDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Start Date Button
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = recStartDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    recStartDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Date: ${DateUtils.formatDate(recStartDate)}")
                    }

                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = recAmountText.toDoubleOrNull()
                        if (recTitle.isBlank()) {
                            errorMsg = "Please enter a title"
                            return@Button
                        }
                        if (amount == null || amount <= 0) {
                            errorMsg = "Please enter an amount greater than 0"
                            return@Button
                        }

                        recurringViewModel.addRecurringTransaction(
                            title = recTitle,
                            amount = amount,
                            type = recType,
                            category = recCategory,
                            frequency = recFrequency,
                            startDate = recStartDate,
                            endDate = null,
                            description = recDesc,
                            onSuccess = { showAddDialog = false }
                        )
                    }
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Recurring Schedule") },
            text = { Text("Are you sure you want to cancel the recurring schedule for \"${item.title}\"? Past recorded transactions will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recurringViewModel.deleteRecurring(item.recurringId)
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
