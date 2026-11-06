package com.example.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.PieChartSlice
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiData(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val currentMonthExpenses: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val budgetProgressPercentage: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val expenseCategorySlices: List<PieChartSlice> = emptyList(),
    val currencySymbol: String = "₹"
)

class DashboardViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    init {
        // Automatically check and process any due recurring transactions
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    private val allTransactionsFlow = repository.getAllTransactions()
    private val overallBudgetFlow = repository.getOverallMonthlyBudget()
    private val currencySymbolFlow = repository.currencySymbol

    val uiData: StateFlow<DashboardUiData> = combine(
        allTransactionsFlow,
        overallBudgetFlow,
        currencySymbolFlow
    ) { transactions, overallBudget, symbol ->
        calculateDashboardData(transactions, overallBudget, symbol)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiData()
    )

    private fun calculateDashboardData(
        transactions: List<TransactionEntity>,
        overallBudget: BudgetEntity?,
        symbol: String
    ): DashboardUiData {
        var totalInc = 0.0
        var totalExp = 0.0
        var monthExp = 0.0
        var monthInc = 0.0

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        val categoryMap = mutableMapOf<String, Double>()

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME.name) {
                totalInc += tx.amount
                if (tx.date in startOfMonth..endOfMonth) {
                    monthInc += tx.amount
                }
            } else {
                totalExp += tx.amount
                if (tx.date in startOfMonth..endOfMonth) {
                    monthExp += tx.amount
                    val currentCatTotal = categoryMap[tx.category] ?: 0.0
                    categoryMap[tx.category] = currentCatTotal + tx.amount
                }
            }
        }

        val totalBal = totalInc - totalExp
        val budgetLimit = overallBudget?.limitAmount ?: 0.0
        val remaining = (budgetLimit - monthExp).coerceAtLeast(0.0)
        val budgetPct = if (budgetLimit > 0) ((monthExp / budgetLimit) * 100).toInt() else 0

        // Create Pie Chart slices
        val slices = categoryMap.entries.mapIndexed { index, entry ->
            val colorHex = CategoryIconHelper.availableColors.getOrElse(index % CategoryIconHelper.availableColors.size) { 0xFF0F766E }
            PieChartSlice(
                label = entry.key,
                value = entry.value,
                color = Color(colorHex)
            )
        }.sortedByDescending { it.value }

        val recent = transactions.take(6)

        return DashboardUiData(
            totalBalance = totalBal,
            totalIncome = totalInc,
            totalExpenses = totalExp,
            currentMonthExpenses = monthExp,
            currentMonthIncome = monthInc,
            monthlyBudget = budgetLimit,
            remainingBudget = remaining,
            budgetProgressPercentage = budgetPct,
            recentTransactions = recent,
            expenseCategorySlices = slices,
            currencySymbol = symbol
        )
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun refreshRecurring() {
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
        }
    }
}
