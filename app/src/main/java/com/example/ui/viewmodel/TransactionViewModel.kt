package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class DateRangeFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}

enum class GroupingMode {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class TransactionFilterState(
    val searchQuery: String = "",
    val typeFilter: String = "ALL", // ALL, EXPENSE, INCOME
    val categoryFilter: String = "ALL",
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val groupingMode: GroupingMode = GroupingMode.DAILY
)

data class GroupedTransactions(
    val groupHeader: String,
    val items: List<TransactionEntity>
)

class TransactionViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "₹"
    )

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredGroupedTransactions: StateFlow<List<GroupedTransactions>> = combine(
        repository.getAllTransactions(),
        _filterState
    ) { transactions, filters ->
        filterAndGroupTransactions(transactions, filters)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setTypeFilter(type: String) {
        _filterState.value = _filterState.value.copy(typeFilter = type)
    }

    fun setCategoryFilter(category: String) {
        _filterState.value = _filterState.value.copy(categoryFilter = category)
    }

    fun setDateRangeFilter(range: DateRangeFilter) {
        _filterState.value = _filterState.value.copy(dateRangeFilter = range)
    }

    fun setGroupingMode(mode: GroupingMode) {
        _filterState.value = _filterState.value.copy(groupingMode = mode)
    }

    fun clearFilters() {
        _filterState.value = TransactionFilterState()
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: Long,
        description: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addTransaction(title, amount, type, category, date, description)
            onSuccess()
        }
    }

    fun updateTransaction(
        transactionId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: Long,
        description: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateTransaction(transactionId, title, amount, type, category, date, description)
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    private fun filterAndGroupTransactions(
        transactions: List<TransactionEntity>,
        filters: TransactionFilterState
    ): List<GroupedTransactions> {
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.getStartOfDay(now)
        val endOfToday = DateUtils.getEndOfDay(now)
        val startOfWeek = DateUtils.getStartOfWeek()
        val endOfWeek = DateUtils.getEndOfWeek()
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val startOfYear = DateUtils.getStartOfYear()
        val endOfYear = DateUtils.getEndOfYear()

        val filtered = transactions.filter { tx ->
            // Search filter
            val matchesSearch = filters.searchQuery.isBlank() ||
                    tx.title.contains(filters.searchQuery, ignoreCase = true) ||
                    tx.description.contains(filters.searchQuery, ignoreCase = true)

            // Type filter
            val matchesType = filters.typeFilter == "ALL" || tx.type == filters.typeFilter

            // Category filter
            val matchesCategory = filters.categoryFilter == "ALL" || tx.category.equals(filters.categoryFilter, ignoreCase = true)

            // Date Range filter
            val matchesDate = when (filters.dateRangeFilter) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> tx.date in startOfToday..endOfToday
                DateRangeFilter.THIS_WEEK -> tx.date in startOfWeek..endOfWeek
                DateRangeFilter.THIS_MONTH -> tx.date in startOfMonth..endOfMonth
                DateRangeFilter.THIS_YEAR -> tx.date in startOfYear..endOfYear
            }

            matchesSearch && matchesType && matchesCategory && matchesDate
        }

        return when (filters.groupingMode) {
            GroupingMode.DAILY -> {
                filtered.groupBy { DateUtils.formatDate(it.date) }
                    .map { GroupedTransactions(groupHeader = it.key, items = it.value) }
            }
            GroupingMode.WEEKLY -> {
                filtered.groupBy {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                    "Week ${cal.get(java.util.Calendar.WEEK_OF_YEAR)}, ${cal.get(java.util.Calendar.YEAR)}"
                }.map { GroupedTransactions(groupHeader = it.key, items = it.value) }
            }
            GroupingMode.MONTHLY -> {
                filtered.groupBy { DateUtils.formatMonthYear(it.date) }
                    .map { GroupedTransactions(groupHeader = it.key, items = it.value) }
            }
        }
    }
}
