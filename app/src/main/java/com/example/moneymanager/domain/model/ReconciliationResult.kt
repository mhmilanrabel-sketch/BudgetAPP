package com.example.moneymanager.domain.model

data class Discrepancy(
    val title: String,
    val description: String,
    val severity: DiscrepancySeverity
)

enum class DiscrepancySeverity {
    WARNING,
    ERROR,
    INFO
}

data class ReconciliationResult(
    val monthKey: String,
    val openingBalance: Double,
    val netSalary: Double,
    val totalRealPay: Double,
    val totalNotPaid: Double,
    val mandatoryRealPay: Double,
    val optionalRealPay: Double,
    val closingBalance: Double,
    val savingsTarget: Double,
    val isOverspent: Boolean,
    val overspentAmount: Double,
    val discrepancies: List<Discrepancy>
)
