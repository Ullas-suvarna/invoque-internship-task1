package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.TransactionType
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetItemUi(
    val budgetId: String,
    val categoryOrTotal: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentage: Int,
    val isOverall: Boolean
)

data class BudgetScreenUiData(
    val overallBudget: BudgetItemUi? = null,
    val categoryBudgets: List<BudgetItemUi> = emptyList(),
    val totalSpentInMonth: Double = 0.0,
    val currencySymbol: String = "₹"
)

class BudgetViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "₹"
    )

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType(TransactionType.EXPENSE).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val budgetUiData: StateFlow<BudgetScreenUiData> = combine(
        repository.getBudgetsForCurrentMonth(),
        repository.getAllTransactions(),
        repository.currencySymbol
    ) { budgets, transactions, symbol ->
        buildBudgetUiData(budgets, transactions, symbol)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetScreenUiData()
    )

    private fun buildBudgetUiData(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        symbol: String
    ): BudgetScreenUiData {
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE.name && it.date in startOfMonth..endOfMonth
        }
        val totalSpent = monthExpenses.sumOf { it.amount }

        var overallItem: BudgetItemUi? = null
        val catBudgetItems = mutableListOf<BudgetItemUi>()

        for (budget in budgets) {
            if (budget.category.isBlank()) {
                val rem = (budget.limitAmount - totalSpent).coerceAtLeast(0.0)
                val pct = if (budget.limitAmount > 0) ((totalSpent / budget.limitAmount) * 100).toInt() else 0
                overallItem = BudgetItemUi(
                    budgetId = budget.budgetId,
                    categoryOrTotal = "Total Monthly Budget",
                    limitAmount = budget.limitAmount,
                    spentAmount = totalSpent,
                    remainingAmount = rem,
                    percentage = pct,
                    isOverall = true
                )
            } else {
                val catSpent = monthExpenses.filter { it.category.equals(budget.category, ignoreCase = true) }.sumOf { it.amount }
                val rem = (budget.limitAmount - catSpent).coerceAtLeast(0.0)
                val pct = if (budget.limitAmount > 0) ((catSpent / budget.limitAmount) * 100).toInt() else 0
                catBudgetItems.add(
                    BudgetItemUi(
                        budgetId = budget.budgetId,
                        categoryOrTotal = budget.category,
                        limitAmount = budget.limitAmount,
                        spentAmount = catSpent,
                        remainingAmount = rem,
                        percentage = pct,
                        isOverall = false
                    )
                )
            }
        }

        return BudgetScreenUiData(
            overallBudget = overallItem,
            categoryBudgets = catBudgetItems,
            totalSpentInMonth = totalSpent,
            currencySymbol = symbol
        )
    }

    fun setOverallMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(amount)
        }
    }

    fun setCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.setCategoryBudget(category, amount)
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
        }
    }
}
