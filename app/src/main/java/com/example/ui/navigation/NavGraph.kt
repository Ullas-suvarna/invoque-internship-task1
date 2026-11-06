package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.SyncState
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.budget.BudgetScreen
import com.example.ui.screens.categories.CategoryManagementScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.recurring.RecurringTransactionsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.transactions.AddEditTransactionScreen
import com.example.ui.screens.transactions.TransactionScreen
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.RecurringViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TransactionViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object AddTransaction : Screen("add_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: String) = "edit_transaction/$transactionId"
    }
    object Categories : Screen("categories")
    object Recurring : Screen("recurring")
}

enum class BottomTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    TRANSACTIONS("Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_transactions"),
    ANALYTICS("Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart, "tab_analytics"),
    BUDGET("Budget", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "tab_budget"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun AppNavGraph(
    repository: ExpenseRepository,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel = remember { AuthViewModel(repository.firebaseManager, repository.preferencesRepository) }
    val dashboardViewModel = remember { DashboardViewModel(repository) }
    val transactionViewModel = remember { TransactionViewModel(repository) }
    val analyticsViewModel = remember { AnalyticsViewModel(repository) }
    val budgetViewModel = remember { BudgetViewModel(repository) }
    val recurringViewModel = remember { RecurringViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    val syncState by repository.syncState.collectAsState()
    val guestUserId by authViewModel.guestUserId.collectAsState()

    val isLoggedIn = authViewModel.currentUser != null || !guestUserId.isNullOrBlank()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        BottomTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        BottomTab.HOME -> {
                            DashboardScreen(
                                dashboardViewModel = dashboardViewModel,
                                syncState = syncState,
                                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                                onNavigateToEditTransaction = { txId -> navController.navigate(Screen.EditTransaction.createRoute(txId)) },
                                onNavigateToTransactions = { selectedTab = BottomTab.TRANSACTIONS },
                                onNavigateToBudget = { selectedTab = BottomTab.BUDGET },
                                onSyncClick = { settingsViewModel.triggerSync() }
                            )
                        }
                        BottomTab.TRANSACTIONS -> {
                            TransactionScreen(
                                transactionViewModel = transactionViewModel,
                                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                                onNavigateToEditTransaction = { txId -> navController.navigate(Screen.EditTransaction.createRoute(txId)) }
                            )
                        }
                        BottomTab.ANALYTICS -> {
                            AnalyticsScreen(analyticsViewModel = analyticsViewModel)
                        }
                        BottomTab.BUDGET -> {
                            BudgetScreen(budgetViewModel = budgetViewModel)
                        }
                        BottomTab.SETTINGS -> {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                                onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                                onLogoutSuccess = {
                                    authViewModel.logout()
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        composable(Screen.AddTransaction.route) {
            AddEditTransactionScreen(
                transactionId = null,
                transactionViewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategoryManagement = { navController.navigate(Screen.Categories.route) }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getString("transactionId")
            AddEditTransactionScreen(
                transactionId = txId,
                transactionViewModel = transactionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategoryManagement = { navController.navigate(Screen.Categories.route) }
            )
        }

        composable(Screen.Categories.route) {
            CategoryManagementScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recurring.route) {
            RecurringTransactionsScreen(
                recurringViewModel = recurringViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
