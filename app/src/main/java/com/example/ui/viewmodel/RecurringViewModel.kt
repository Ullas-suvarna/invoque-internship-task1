package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.RecurringFrequency
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val recurringList: StateFlow<List<RecurringTransactionEntity>> = repository.getRecurringTransactions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "₹"
    )

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        frequency: RecurringFrequency,
        startDate: Long,
        endDate: Long?,
        description: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addRecurringTransaction(
                title,
                amount,
                type,
                category,
                frequency,
                startDate,
                endDate,
                description
            )
            repository.processDueRecurringTransactions()
            onSuccess()
        }
    }

    fun deleteRecurring(recurringId: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(recurringId)
        }
    }

    fun triggerProcessDue() {
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }
}
