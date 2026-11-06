package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    PENDING,
    ERROR,
    OFFLINE
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)

val SupportedCurrencies = listOf(
    CurrencyInfo("INR", "₹", "Indian Rupee (INR)"),
    CurrencyInfo("USD", "$", "US Dollar (USD)"),
    CurrencyInfo("EUR", "€", "Euro (EUR)"),
    CurrencyInfo("GBP", "£", "British Pound (GBP)"),
    CurrencyInfo("AED", "د.إ", "UAE Dirham (AED)"),
    CurrencyInfo("JPY", "¥", "Japanese Yen (JPY)"),
    CurrencyInfo("CAD", "C$", "Canadian Dollar (CAD)"),
    CurrencyInfo("AUD", "A$", "Australian Dollar (AUD)"),
    CurrencyInfo("SGD", "S$", "Singapore Dollar (SGD)")
)
