package com.example.data.local

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.RecurringFrequency
import com.example.domain.model.TransactionType
import com.example.util.DateUtils
import java.util.Calendar
import java.util.UUID

object SampleData {

    fun getSampleTransactions(userId: String = "local_user"): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Helper for days offset
        fun timeAgo(daysAgo: Int, hour: Int = 14, min: Int = 30): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = now
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, min)
            return c.timeInMillis
        }

        val startOfMonth = DateUtils.getStartOfMonth()

        return listOf(
            // --- INCOMES ---
            TransactionEntity(
                transactionId = "sample_inc_1",
                userId = userId,
                title = "Monthly Salary - Tech Corp",
                amount = 75000.0,
                type = TransactionType.INCOME.name,
                category = "Salary",
                date = startOfMonth + 3600000L * 10,
                description = "Direct monthly salary deposit",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_inc_2",
                userId = userId,
                title = "Freelance Mobile App UI",
                amount = 18500.0,
                type = TransactionType.INCOME.name,
                category = "Freelancing",
                date = timeAgo(3, 16, 0),
                description = "Milestone 2 payment received",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_inc_3",
                userId = userId,
                title = "Mutual Fund Dividend",
                amount = 4200.0,
                type = TransactionType.INCOME.name,
                category = "Investment Returns",
                date = timeAgo(7, 11, 45),
                description = "Quarterly investment payout",
                isSynced = false
            ),

            // --- EXPENSES ---
            TransactionEntity(
                transactionId = "sample_exp_1",
                userId = userId,
                title = "Apartment Rent",
                amount = 22000.0,
                type = TransactionType.EXPENSE.name,
                category = "Rent",
                date = startOfMonth + 3600000L * 24 * 2,
                description = "Monthly house rent transfer",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_2",
                userId = userId,
                title = "Supermarket Grocery & Fruits",
                amount = 3450.0,
                type = TransactionType.EXPENSE.name,
                category = "Food",
                date = timeAgo(0, 18, 20), // Today
                description = "Weekly organic groceries & vegetables",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_3",
                userId = userId,
                title = "Electricity & Water Bill",
                amount = 2800.0,
                type = TransactionType.EXPENSE.name,
                category = "Bills",
                date = timeAgo(1, 10, 15), // Yesterday
                description = "Utility bill payment via UPI",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_4",
                userId = userId,
                title = "Dinner with Friends",
                amount = 1650.0,
                type = TransactionType.EXPENSE.name,
                category = "Food",
                date = timeAgo(1, 20, 45), // Yesterday
                description = "Italian restaurant weekend dinner",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_5",
                userId = userId,
                title = "High-speed Fiber Internet",
                amount = 999.0,
                type = TransactionType.EXPENSE.name,
                category = "Bills",
                date = timeAgo(4, 9, 30),
                description = "Monthly 200Mbps broadband",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_6",
                userId = userId,
                title = "Casual Jacket & Sneakers",
                amount = 4200.0,
                type = TransactionType.EXPENSE.name,
                category = "Shopping",
                date = timeAgo(5, 17, 10),
                description = "Weekend shopping at city center",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_7",
                userId = userId,
                title = "Fuel & Metro Card Recharge",
                amount = 1500.0,
                type = TransactionType.EXPENSE.name,
                category = "Transportation",
                date = timeAgo(2, 8, 30),
                description = "Vehicle fuel + commute card",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_8",
                userId = userId,
                title = "Cinema Tickets & Snacks",
                amount = 850.0,
                type = TransactionType.EXPENSE.name,
                category = "Entertainment",
                date = timeAgo(6, 19, 0),
                description = "Weekend movie screening",
                isSynced = false
            ),
            TransactionEntity(
                transactionId = "sample_exp_9",
                userId = userId,
                title = "Prescriptions & Vitamins",
                amount = 1120.0,
                type = TransactionType.EXPENSE.name,
                category = "Healthcare",
                date = timeAgo(8, 12, 15),
                description = "Monthly pharmacy refills",
                isSynced = false
            )
        )
    }

    fun getSampleBudgets(userId: String = "local_user"): List<BudgetEntity> {
        val monthYear = DateUtils.getCurrentMonthYear()
        return listOf(
            // Total monthly budget
            BudgetEntity(
                budgetId = "sample_budget_total",
                userId = userId,
                monthYear = monthYear,
                category = "",
                limitAmount = 45000.0,
                isSynced = false
            ),
            // Category budgets
            BudgetEntity(
                budgetId = "sample_budget_food",
                userId = userId,
                monthYear = monthYear,
                category = "Food",
                limitAmount = 10000.0,
                isSynced = false
            ),
            BudgetEntity(
                budgetId = "sample_budget_shopping",
                userId = userId,
                monthYear = monthYear,
                category = "Shopping",
                limitAmount = 6000.0,
                isSynced = false
            ),
            BudgetEntity(
                budgetId = "sample_budget_bills",
                userId = userId,
                monthYear = monthYear,
                category = "Bills",
                limitAmount = 5000.0,
                isSynced = false
            )
        )
    }

    fun getSampleRecurring(userId: String = "local_user"): List<RecurringTransactionEntity> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 12)
        }
        return listOf(
            RecurringTransactionEntity(
                recurringId = "sample_rec_rent",
                userId = userId,
                title = "Apartment Rent",
                amount = 22000.0,
                type = TransactionType.EXPENSE.name,
                category = "Rent",
                frequency = RecurringFrequency.MONTHLY.name,
                startDate = DateUtils.getStartOfMonth(),
                nextDueDate = cal.timeInMillis,
                isActive = true,
                description = "Recurring monthly rent payment",
                isSynced = false
            ),
            RecurringTransactionEntity(
                recurringId = "sample_rec_internet",
                userId = userId,
                title = "Fiber Broadband",
                amount = 999.0,
                type = TransactionType.EXPENSE.name,
                category = "Bills",
                frequency = RecurringFrequency.MONTHLY.name,
                startDate = DateUtils.getStartOfMonth(),
                nextDueDate = now + 86400000L * 5,
                isActive = true,
                description = "Monthly internet renewal",
                isSynced = false
            ),
            RecurringTransactionEntity(
                recurringId = "sample_rec_salary",
                userId = userId,
                title = "Tech Corp Salary",
                amount = 75000.0,
                type = TransactionType.INCOME.name,
                category = "Salary",
                frequency = RecurringFrequency.MONTHLY.name,
                startDate = DateUtils.getStartOfMonth(),
                nextDueDate = DateUtils.getEndOfMonth() + 86400000L,
                isActive = true,
                description = "Recurring payroll salary",
                isSynced = false
            )
        )
    }
}
