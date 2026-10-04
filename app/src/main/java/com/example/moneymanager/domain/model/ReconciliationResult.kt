package com.example.moneymanager.domain.model

data class ReconciliationResult(
    val monthKey: String,
    val netSalary: Double,
    val openingBalance: Double,
    val mandatoryExpenses: Double,
    val optionalExpenses: Double,
    val totalRealPay: Double,
    val closingBalance: Double,
    val savingsTarget: Double,
    val warnings: List<String>,
    val isOverspent: Boolean,
    val isSavingsTargetMet: Boolean,
    val hasCarryForwardUnpaid: Boolean,
    val salaryDiscrepancy: Double? = null,
    val netSalaryMatchesHandSave: Boolean = true
)
