package com.example.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.TransactionType
import com.example.ui.components.BarChartItem
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.PieChartSlice
import com.example.ui.components.TrendPoint
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AnalyticsTimeRange {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Int,
    val count: Int,
    val color: Color
)

data class AnalyticsUiData(
    val selectedRange: AnalyticsTimeRange = AnalyticsTimeRange.MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Int = 0,
    val categorySlices: List<PieChartSlice> = emptyList(),
    val topSpendingCategories: List<CategorySpending> = emptyList(),
    val incomeExpenseBars: List<BarChartItem> = emptyList(),
    val spendingTrendPoints: List<TrendPoint> = emptyList(),
    val transactionCount: Int = 0,
    val currencySymbol: String = "₹"
)

class AnalyticsViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.MONTH)
    val selectedTimeRange: StateFlow<AnalyticsTimeRange> = _selectedTimeRange.asStateFlow()

    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "₹"
    )

    val analyticsData: StateFlow<AnalyticsUiData> = combine(
        repository.getAllTransactions(),
        _selectedTimeRange,
        repository.currencySymbol
    ) { transactions, timeRange, symbol ->
        computeAnalytics(transactions, timeRange, symbol)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsUiData()
    )

    fun setTimeRange(range: AnalyticsTimeRange) {
        _selectedTimeRange.value = range
    }

    private fun computeAnalytics(
        transactions: List<TransactionEntity>,
        timeRange: AnalyticsTimeRange,
        symbol: String
    ): AnalyticsUiData {
        val now = System.currentTimeMillis()
        val (start, end) = when (timeRange) {
            AnalyticsTimeRange.DAY -> Pair(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
            AnalyticsTimeRange.WEEK -> Pair(DateUtils.getStartOfWeek(), DateUtils.getEndOfWeek())
            AnalyticsTimeRange.MONTH -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfMonth())
            AnalyticsTimeRange.YEAR -> Pair(DateUtils.getStartOfYear(), DateUtils.getEndOfYear())
        }

        val periodTransactions = transactions.filter { it.date in start..end }
        val incomeList = periodTransactions.filter { it.type == TransactionType.INCOME.name }
        val expenseList = periodTransactions.filter { it.type == TransactionType.EXPENSE.name }

        val totalInc = incomeList.sumOf { it.amount }
        val totalExp = expenseList.sumOf { it.amount }
        val netSavings = totalInc - totalExp
        val savingsRate = if (totalInc > 0) ((netSavings / totalInc) * 100).toInt().coerceAtLeast(0) else 0

        // Category breakdown
        val categoryTotals = mutableMapOf<String, Double>()
        val categoryCounts = mutableMapOf<String, Int>()
        for (tx in expenseList) {
            categoryTotals[tx.category] = (categoryTotals[tx.category] ?: 0.0) + tx.amount
            categoryCounts[tx.category] = (categoryCounts[tx.category] ?: 0) + 1
        }

        val categorySlices = categoryTotals.entries.mapIndexed { index, entry ->
            val colorHex = CategoryIconHelper.availableColors.getOrElse(index % CategoryIconHelper.availableColors.size) { 0xFF0F766E }
            PieChartSlice(
                label = entry.key,
                value = entry.value,
                color = Color(colorHex)
            )
        }.sortedByDescending { it.value }

        val topSpending = categoryTotals.entries.sortedByDescending { it.value }.mapIndexed { index, entry ->
            val colorHex = CategoryIconHelper.availableColors.getOrElse(index % CategoryIconHelper.availableColors.size) { 0xFF0F766E }
            val pct = if (totalExp > 0) ((entry.value / totalExp) * 100).toInt() else 0
            CategorySpending(
                category = entry.key,
                amount = entry.value,
                percentage = pct,
                count = categoryCounts[entry.key] ?: 1,
                color = Color(colorHex)
            )
        }

        // Bar Chart and Line Chart generation based on timeRange intervals
        val (bars, trend) = generateIntervalData(periodTransactions, timeRange, start, end)

        return AnalyticsUiData(
            selectedRange = timeRange,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netSavings = netSavings,
            savingsRate = savingsRate,
            categorySlices = categorySlices,
            topSpendingCategories = topSpending,
            incomeExpenseBars = bars,
            spendingTrendPoints = trend,
            transactionCount = periodTransactions.size,
            currencySymbol = symbol
        )
    }

    private fun generateIntervalData(
        transactions: List<TransactionEntity>,
        timeRange: AnalyticsTimeRange,
        startTime: Long,
        endTime: Long
    ): Pair<List<BarChartItem>, List<TrendPoint>> {
        val bars = mutableListOf<BarChartItem>()
        val trend = mutableListOf<TrendPoint>()

        when (timeRange) {
            AnalyticsTimeRange.DAY -> {
                // Break into 4 blocks of 6 hours
                val blocks = listOf("00-06h", "06-12h", "12-18h", "18-24h")
                val cal = Calendar.getInstance()
                for (i in 0..3) {
                    val label = blocks[i]
                    val inc = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.INCOME.name && (cal.get(Calendar.HOUR_OF_DAY) in (i * 6) until ((i + 1) * 6))
                    }.sumOf { it.amount }

                    val exp = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.EXPENSE.name && (cal.get(Calendar.HOUR_OF_DAY) in (i * 6) until ((i + 1) * 6))
                    }.sumOf { it.amount }

                    bars.add(BarChartItem(label, inc, exp))
                    trend.add(TrendPoint(label, exp))
                }
            }
            AnalyticsTimeRange.WEEK -> {
                // 7 days
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val cal = Calendar.getInstance()
                for (d in 1..7) {
                    val label = dayNames[d - 1]
                    val inc = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.INCOME.name && cal.get(Calendar.DAY_OF_WEEK) == d
                    }.sumOf { it.amount }

                    val exp = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.EXPENSE.name && cal.get(Calendar.DAY_OF_WEEK) == d
                    }.sumOf { it.amount }

                    bars.add(BarChartItem(label, inc, exp))
                    trend.add(TrendPoint(label, exp))
                }
            }
            AnalyticsTimeRange.MONTH -> {
                // 4 weeks of the month
                val cal = Calendar.getInstance()
                for (w in 1..4) {
                    val label = "W$w"
                    val inc = transactions.filter {
                        cal.timeInMillis = it.date
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        it.type == TransactionType.INCOME.name && (day in ((w - 1) * 7 + 1)..minOf(w * 7, 31))
                    }.sumOf { it.amount }

                    val exp = transactions.filter {
                        cal.timeInMillis = it.date
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        it.type == TransactionType.EXPENSE.name && (day in ((w - 1) * 7 + 1)..minOf(w * 7, 31))
                    }.sumOf { it.amount }

                    bars.add(BarChartItem(label, inc, exp))
                    trend.add(TrendPoint(label, exp))
                }
            }
            AnalyticsTimeRange.YEAR -> {
                // 12 months
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val cal = Calendar.getInstance()
                for (m in 0..11) {
                    val label = monthNames[m]
                    val inc = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.INCOME.name && cal.get(Calendar.MONTH) == m
                    }.sumOf { it.amount }

                    val exp = transactions.filter {
                        cal.timeInMillis = it.date
                        it.type == TransactionType.EXPENSE.name && cal.get(Calendar.MONTH) == m
                    }.sumOf { it.amount }

                    bars.add(BarChartItem(label, inc, exp))
                    trend.add(TrendPoint(label, exp))
                }
            }
        }

        return Pair(bars, trend)
    }
}
