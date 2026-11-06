package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val formatter = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.US))
    private val compactFormatter = DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.US))

    fun formatAmount(amount: Double, symbol: String = "₹"): String {
        return "$symbol ${formatter.format(amount)}"
    }

    fun formatCompactAmount(amount: Double, symbol: String = "₹"): String {
        return "$symbol ${compactFormatter.format(amount)}"
    }
}
