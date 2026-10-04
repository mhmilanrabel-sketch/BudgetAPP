package com.example.moneymanager.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val df = DecimalFormat("#,##0.00", symbols)

    fun formatLkr(amount: Double): String {
        return "Rs. ${df.format(amount)}"
    }

    fun formatLkrNoPrefix(amount: Double): String {
        return df.format(amount)
    }

    fun parseAmount(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val sanitized = raw.replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("LKR", "", ignoreCase = true)
            .replace(",", "")
            .replace("(", "-")
            .replace(")", "")
            .trim()
        return sanitized.toDoubleOrNull() ?: 0.0
    }
}
