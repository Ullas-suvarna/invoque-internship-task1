package com.example.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppThemeMode
import com.example.domain.model.SupportedCurrencies
import com.example.domain.model.SyncState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val syncState by settingsViewModel.syncState.collectAsState()
    val lastSyncTime by settingsViewModel.lastSyncTime.collectAsState()
    val currencySymbol by settingsViewModel.currencySymbol.collectAsState()
    val currencyCode by settingsViewModel.currencyCode.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val isExporting by settingsViewModel.isExporting.collectAsState()

    val currentUser = settingsViewModel.currentUser

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = currentUser?.displayName?.ifBlank { "Personal Finance User" } ?: "Personal Finance User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUser?.email ?: "Offline / Guest Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cloud Sync Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (syncState) {
                                        SyncState.SYNCED -> Icons.Default.CloudDone
                                        SyncState.SYNCING -> Icons.Default.Sync
                                        else -> Icons.Default.CloudOff
                                    },
                                    contentDescription = null,
                                    tint = when (syncState) {
                                        SyncState.SYNCED -> IncomeGreen
                                        SyncState.SYNCING -> MaterialTheme.colorScheme.primary
                                        else -> WarningAmber
                                    },
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Cloud Synchronization",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (syncState) {
                                            SyncState.SYNCED -> "Status: Synced with Firebase"
                                            SyncState.SYNCING -> "Status: Syncing in progress..."
                                            SyncState.PENDING -> "Status: Pending sync"
                                            SyncState.OFFLINE -> "Status: Offline mode"
                                            SyncState.ERROR -> "Status: Sync failed / retry"
                                            else -> "Status: Ready"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (syncState) {
                                            SyncState.SYNCED -> IncomeGreen
                                            SyncState.ERROR -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            Button(
                                onClick = { settingsViewModel.triggerSync() },
                                shape = RoundedCornerShape(10.dp),
                                enabled = syncState != SyncState.SYNCING,
                                modifier = Modifier.testTag("sync_now_button")
                            ) {
                                if (syncState == SyncState.SYNCING) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text("Sync Now")
                                }
                            }
                        }

                        if (lastSyncTime != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Last synced: ${DateUtils.formatDate(lastSyncTime!!)} at ${DateUtils.formatTime(lastSyncTime!!)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Preferences Section
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Currency Selector Row
                        SettingsClickableRow(
                            icon = Icons.Default.Payments,
                            title = "Default Currency",
                            subtitle = "$currencyCode ($currencySymbol)",
                            onClick = { showCurrencyDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Theme Selector Row
                        SettingsClickableRow(
                            icon = Icons.Default.DarkMode,
                            title = "Theme Mode",
                            subtitle = when (themeMode) {
                                AppThemeMode.LIGHT -> "Light Theme"
                                AppThemeMode.DARK -> "Dark Theme"
                                AppThemeMode.SYSTEM -> "System Default"
                            },
                            onClick = { showThemeDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Notifications Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Budget & Due Alerts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("Notify when 80%, 90% or 100% budget reached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
                            )
                        }
                    }
                }
            }

            // Management & Tools Section
            item {
                Text(
                    text = "Management & Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Default.Category,
                            title = "Expense & Income Categories",
                            subtitle = "Customize categories, icons, and colors",
                            onClick = onNavigateToCategories
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsClickableRow(
                            icon = Icons.Default.Repeat,
                            title = "Recurring Schedules",
                            subtitle = "Automate bills, salaries, and subscriptions",
                            onClick = onNavigateToRecurring
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // CSV Export
                        SettingsClickableRow(
                            icon = Icons.Default.FileDownload,
                            title = "Export to CSV",
                            subtitle = "Download or share your entire financial history",
                            onClick = { settingsViewModel.exportTransactionsCsv(context) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Load Demo / Sample Data
                        SettingsClickableRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "Load Demo Financial Data",
                            subtitle = "Populate realistic sample income, expenses, and budgets",
                            onClick = {
                                settingsViewModel.loadSampleData()
                                Toast.makeText(context, "Sample financial data loaded successfully!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Account & Logout Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsClickableRow(
                        icon = Icons.Default.Logout,
                        title = "Log Out",
                        subtitle = "End current session and clear local credentials",
                        onClick = { showLogoutDialog = true },
                        iconTint = ExpenseRed,
                        titleColor = ExpenseRed
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SupportedCurrencies.forEach { curr ->
                        val isSelected = currencyCode == curr.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settingsViewModel.setCurrency(curr)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsViewModel.setCurrency(curr)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = curr.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppThemeMode.values().forEach { mode ->
                        val isSelected = themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settingsViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsViewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.SYSTEM -> "System Default"
                                    AppThemeMode.LIGHT -> "Light Theme"
                                    AppThemeMode.DARK -> "Dark Theme"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out of Expense Tracker?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        settingsViewModel.logout()
                        onLogoutSuccess()
                    }
                ) {
                    Text("Log Out", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
