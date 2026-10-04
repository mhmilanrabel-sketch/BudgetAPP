package com.example.moneymanager.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CurrencyFormatter {

    private val lkLocale = Locale("en", "LK")

    fun formatLkr(amount: Double): String {
        val symbols = DecimalFormatSymbols(lkLocale).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        return "Rs. ${df.format(amount)}"
    }

    fun parseAmount(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val clean = text
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("LKR", "", ignoreCase = true)
            .replace(",", "")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatMonth(monthKey: String): String {
        return try {
            val ym = YearMonth.parse(monthKey, DateTimeFormatter.ofPattern("yyyy-MM"))
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            monthKey
        }
    }

    fun getCurrentMonthKey(): String {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
}
