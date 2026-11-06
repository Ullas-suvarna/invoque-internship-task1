package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.FirebaseManager
import com.example.domain.model.AppThemeMode
import com.example.domain.model.RecurringFrequency
import com.example.domain.model.SyncState
import com.example.domain.model.TransactionType
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class ExpenseRepository(
    private val context: Context,
    private val database: AppDatabase,
    val firebaseManager: FirebaseManager,
    val preferencesRepository: UserPreferencesRepository
) {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringTransactionDao()
    private val userDao = database.userDao()

    val syncState: StateFlow<SyncState> = firebaseManager.syncState
    val lastSyncTime: StateFlow<Long?> = firebaseManager.lastSyncTime
    val currencySymbol: Flow<String> = preferencesRepository.currencySymbol
    val currencyCode: Flow<String> = preferencesRepository.currencyCode
    val themeMode: Flow<AppThemeMode> = preferencesRepository.themeMode
    val notificationsEnabled: Flow<Boolean> = preferencesRepository.notificationsEnabled

    fun getActiveUserId(): String {
        return firebaseManager.currentUser?.uid ?: "local_user"
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedGuestSampleDataIfEmpty()
        }
    }

    suspend fun seedGuestSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentTx = transactionDao.getAllTransactions("local_user").first()
        if (currentTx.isEmpty()) {
            transactionDao.insertTransactions(com.example.data.local.SampleData.getSampleTransactions("local_user"))
            budgetDao.insertBudgets(com.example.data.local.SampleData.getSampleBudgets("local_user"))
            recurringDao.insertRecurringList(com.example.data.local.SampleData.getSampleRecurring("local_user"))
        }
    }

    suspend fun loadSampleData() = withContext(Dispatchers.IO) {
        val uid = getActiveUserId()
        transactionDao.insertTransactions(com.example.data.local.SampleData.getSampleTransactions(uid))
        budgetDao.insertBudgets(com.example.data.local.SampleData.getSampleBudgets(uid))
        recurringDao.insertRecurringList(com.example.data.local.SampleData.getSampleRecurring(uid))
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions(getActiveUserId())
    }

    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(getActiveUserId(), startDate, endDate)
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: Long,
        description: String
    ) = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val tx = TransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            amount = amount,
            type = type.name,
            category = category.trim(),
            date = date,
            description = description.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        )
        transactionDao.insertTransaction(tx)

        // Check budget thresholds if expense
        if (type == TransactionType.EXPENSE) {
            checkBudgetAlerts(userId, category, amount)
        }

        // Trigger background sync if online
        triggerBackgroundSync(userId)
    }

    suspend fun updateTransaction(
        transactionId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        date: Long,
        description: String
    ) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getTransactionById(transactionId) ?: return@withContext
        val updated = existing.copy(
            title = title.trim(),
            amount = amount,
            type = type.name,
            category = category.trim(),
            date = date,
            description = description.trim(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        transactionDao.updateTransaction(updated)
        triggerBackgroundSync(existing.userId)
    }

    suspend fun deleteTransaction(transactionId: String) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getTransactionById(transactionId) ?: return@withContext
        transactionDao.softDeleteTransaction(transactionId)
        triggerBackgroundSync(existing.userId)
    }

    // Categories
    fun getCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getCategories(getActiveUserId())
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(getActiveUserId(), type.name)
    }

    suspend fun addCategory(
        name: String,
        type: TransactionType,
        iconName: String,
        colorHex: Long
    ) = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val category = CategoryEntity(
            categoryId = UUID.randomUUID().toString(),
            userId = userId,
            name = name.trim(),
            type = type.name,
            iconName = iconName,
            colorHex = colorHex,
            isCustom = true,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        categoryDao.insertCategory(category)
        triggerBackgroundSync(userId)
    }

    suspend fun canDeleteCategory(categoryName: String): Boolean = withContext(Dispatchers.IO) {
        val inUse = transactionDao.getTransactionsByCategory(getActiveUserId(), categoryName)
        inUse.isEmpty()
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        categoryDao.softDeleteCategory(categoryId)
        triggerBackgroundSync(getActiveUserId())
    }

    // Budgets
    fun getBudgetsForCurrentMonth(): Flow<List<BudgetEntity>> {
        val monthYear = DateUtils.getCurrentMonthYear()
        return budgetDao.getBudgetsForMonth(getActiveUserId(), monthYear)
    }

    fun getOverallMonthlyBudget(): Flow<BudgetEntity?> {
        val monthYear = DateUtils.getCurrentMonthYear()
        return budgetDao.getOverallMonthlyBudget(getActiveUserId(), monthYear)
    }

    suspend fun setMonthlyBudget(totalBudget: Double) = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val monthYear = DateUtils.getCurrentMonthYear()
        val existing = budgetDao.getCategoryBudget(userId, monthYear, "")
        val budget = BudgetEntity(
            budgetId = existing?.budgetId ?: UUID.randomUUID().toString(),
            userId = userId,
            monthYear = monthYear,
            category = "",
            limitAmount = totalBudget,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        budgetDao.insertBudget(budget)
        triggerBackgroundSync(userId)
    }

    suspend fun setCategoryBudget(category: String, limit: Double) = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val monthYear = DateUtils.getCurrentMonthYear()
        val existing = budgetDao.getCategoryBudget(userId, monthYear, category)
        val budget = BudgetEntity(
            budgetId = existing?.budgetId ?: UUID.randomUUID().toString(),
            userId = userId,
            monthYear = monthYear,
            category = category,
            limitAmount = limit,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        budgetDao.insertBudget(budget)
        triggerBackgroundSync(userId)
    }

    suspend fun deleteBudget(budgetId: String) = withContext(Dispatchers.IO) {
        budgetDao.softDeleteBudget(budgetId)
        triggerBackgroundSync(getActiveUserId())
    }

    // Recurring Transactions
    fun getRecurringTransactions(): Flow<List<RecurringTransactionEntity>> {
        return recurringDao.getRecurringTransactions(getActiveUserId())
    }

    suspend fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        frequency: RecurringFrequency,
        startDate: Long,
        endDate: Long?,
        description: String
    ) = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val rec = RecurringTransactionEntity(
            recurringId = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            amount = amount,
            type = type.name,
            category = category.trim(),
            frequency = frequency.name,
            startDate = startDate,
            endDate = endDate,
            lastRunDate = 0L,
            nextDueDate = startDate,
            isActive = true,
            description = description.trim(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        recurringDao.insertRecurring(rec)
        triggerBackgroundSync(userId)
    }

    suspend fun deleteRecurringTransaction(recurringId: String) = withContext(Dispatchers.IO) {
        recurringDao.softDeleteRecurring(recurringId)
        triggerBackgroundSync(getActiveUserId())
    }

    // Automatic processing of recurring transactions
    suspend fun processDueRecurringTransactions() = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val now = System.currentTimeMillis()
        val dueList = recurringDao.getDueRecurringTransactions(userId, now)
        val symbol = preferencesRepository.currencySymbol.first()

        for (rec in dueList) {
            // Create transaction record
            val tx = TransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                userId = userId,
                title = rec.title,
                amount = rec.amount,
                type = rec.type,
                category = rec.category,
                date = rec.nextDueDate,
                description = if (rec.description.isNotBlank()) "(Recurring) ${rec.description}" else "Recurring payment",
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            transactionDao.insertTransaction(tx)

            // Compute next due date
            val nextDate = computeNextDueDate(rec.nextDueDate, rec.frequency)
            val isExpired = rec.endDate != null && nextDate > rec.endDate

            val updatedRec = rec.copy(
                lastRunDate = rec.nextDueDate,
                nextDueDate = nextDate,
                isActive = !isExpired,
                updatedAt = now,
                isSynced = false
            )
            recurringDao.updateRecurring(updatedRec)

            if (preferencesRepository.notificationsEnabled.first()) {
                NotificationUtils.showRecurringProcessed(
                    context,
                    rec.title,
                    CurrencyUtils.formatAmount(rec.amount, symbol)
                )
            }
        }
    }

    private fun computeNextDueDate(current: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        when (frequency) {
            RecurringFrequency.DAILY.name -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY.name -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY.name -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY.name -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private suspend fun checkBudgetAlerts(userId: String, category: String, newExpenseAmount: Double) {
        if (!preferencesRepository.notificationsEnabled.first()) return

        val monthYear = DateUtils.getCurrentMonthYear()
        val symbol = preferencesRepository.currencySymbol.first()
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        // 1. Overall monthly budget check
        val overallBudget = budgetDao.getOverallMonthlyBudget(userId, monthYear).firstOrNull()
        if (overallBudget != null && overallBudget.limitAmount > 0) {
            val monthTransactions = transactionDao.getTransactionsBetween(userId, startOfMonth, endOfMonth).first()
            val totalExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val percentage = ((totalExpense / overallBudget.limitAmount) * 100).toInt()

            if (percentage >= 80) {
                NotificationUtils.showBudgetWarning(
                    context,
                    "Total Monthly Budget",
                    percentage,
                    CurrencyUtils.formatAmount(totalExpense, symbol),
                    CurrencyUtils.formatAmount(overallBudget.limitAmount, symbol)
                )
            }
        }

        // 2. Specific category budget check
        val catBudget = budgetDao.getCategoryBudget(userId, monthYear, category)
        if (catBudget != null && catBudget.limitAmount > 0) {
            val monthTransactions = transactionDao.getTransactionsBetween(userId, startOfMonth, endOfMonth).first()
            val catExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE.name && it.category.equals(category, ignoreCase = true) }.sumOf { it.amount }
            val percentage = ((catExpense / catBudget.limitAmount) * 100).toInt()

            if (percentage >= 80) {
                NotificationUtils.showBudgetWarning(
                    context,
                    category,
                    percentage,
                    CurrencyUtils.formatAmount(catExpense, symbol),
                    CurrencyUtils.formatAmount(catBudget.limitAmount, symbol)
                )
            }
        }
    }

    private fun triggerBackgroundSync(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (firebaseManager.isNetworkAvailable() && userId.isNotBlank() && userId != "local_user") {
                firebaseManager.syncWithCloud(userId)
            }
        }
    }

    suspend fun syncNow(): Boolean {
        return firebaseManager.syncWithCloud(getActiveUserId())
    }
}
