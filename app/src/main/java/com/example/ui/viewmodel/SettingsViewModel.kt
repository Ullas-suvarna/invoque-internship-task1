package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.AppThemeMode
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.SyncState
import com.example.util.ExportUtils
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = repository.syncState
    val lastSyncTime: StateFlow<Long?> = repository.lastSyncTime
    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "₹"
    )
    val currencyCode: StateFlow<String> = repository.currencyCode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "INR"
    )
    val themeMode: StateFlow<AppThemeMode> = repository.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppThemeMode.SYSTEM
    )
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val currentUser: FirebaseUser?
        get() = repository.firebaseManager.currentUser

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun setCurrency(currency: CurrencyInfo) {
        viewModelScope.launch {
            repository.preferencesRepository.setCurrency(currency.symbol, currency.code)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.preferencesRepository.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncNow()
        }
    }

    fun exportTransactionsCsv(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val transactions = repository.getAllTransactions().first()
                ExportUtils.saveAndExportCsv(context, transactions)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
        }
    }

    fun logout() {
        repository.firebaseManager.logout()
        viewModelScope.launch {
            repository.preferencesRepository.setGuestUserId(null)
        }
    }
}
