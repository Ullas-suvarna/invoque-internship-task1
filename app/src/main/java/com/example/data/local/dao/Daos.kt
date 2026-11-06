package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetween(userId: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :categoryName AND isDeleted = 0")
    suspend fun getTransactionsByCategory(userId: String, categoryName: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE transactionId = :transactionId")
    suspend fun softDeleteTransaction(transactionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE transactionId = :transactionId")
    suspend fun hardDeleteTransaction(transactionId: String)

    @Query("UPDATE transactions SET isSynced = 1 WHERE transactionId = :transactionId")
    suspend fun markTransactionSynced(transactionId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE (userId = '' OR userId = :userId) AND isDeleted = 0 ORDER BY name ASC")
    fun getCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (userId = '' OR userId = :userId) AND type = :type AND isDeleted = 0 ORDER BY name ASC")
    fun getCategoriesByType(userId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE categoryId = :categoryId")
    suspend fun softDeleteCategory(categoryId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("UPDATE categories SET isSynced = 1 WHERE categoryId = :categoryId")
    suspend fun markCategorySynced(categoryId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND isDeleted = 0")
    fun getBudgetsForMonth(userId: String, monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND category = '' AND isDeleted = 0 LIMIT 1")
    fun getOverallMonthlyBudget(userId: String, monthYear: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear AND category = :category AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryBudget(userId: String, monthYear: String, category: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("UPDATE budgets SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE budgetId = :budgetId")
    suspend fun softDeleteBudget(budgetId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("UPDATE budgets SET isSynced = 1 WHERE budgetId = :budgetId")
    suspend fun markBudgetSynced(budgetId: String)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY nextDueDate ASC")
    fun getRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isActive = 1 AND isDeleted = 0 AND nextDueDate <= :currentTime")
    suspend fun getDueRecurringTransactions(userId: String, currentTime: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(list: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE recurringId = :recurringId")
    suspend fun softDeleteRecurring(recurringId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recurring_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedRecurring(): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET isSynced = 1 WHERE recurringId = :recurringId")
    suspend fun markRecurringSynced(recurringId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUser(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}
