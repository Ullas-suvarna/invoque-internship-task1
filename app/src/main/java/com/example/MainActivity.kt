package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.FirebaseManager
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.AppThemeMode
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.ExpenseTrackerTheme
import com.example.util.NotificationUtils

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Android notification channels for budget and recurring alerts
        NotificationUtils.initNotificationChannels(this)

        val database = AppDatabase.getDatabase(this)
        val preferencesRepository = UserPreferencesRepository(this)
        val firebaseManager = FirebaseManager(
            context = this,
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            recurringDao = database.recurringTransactionDao(),
            userDao = database.userDao()
        )
        val expenseRepository = ExpenseRepository(
            context = this,
            database = database,
            firebaseManager = firebaseManager,
            preferencesRepository = preferencesRepository
        )

        setContent {
            val themeMode by expenseRepository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)

            ExpenseTrackerTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(repository = expenseRepository)
                }
            }
        }
    }
}
