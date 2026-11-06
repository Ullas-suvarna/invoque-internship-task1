package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String = "Other",
    val date: Long = System.currentTimeMillis(), // timestamp millis
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val categoryId: String = UUID.randomUUID().toString(),
    val userId: String = "", // empty for global defaults
    val name: String = "",
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val iconName: String = "Category",
    val colorHex: Long = 0xFF2196F3,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val budgetId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val monthYear: String = "", // e.g. "2026-11"
    val category: String = "", // empty for total monthly budget, or category name
    val limitAmount: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey
    val recurringId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String = "Other",
    val frequency: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val lastRunDate: Long = 0L,
    val nextDueDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
