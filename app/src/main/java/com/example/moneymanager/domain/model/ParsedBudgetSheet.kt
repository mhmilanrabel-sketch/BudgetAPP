package com.example.moneymanager.domain.model

data class BudgetExpenseItem(
    val name: String,
    val amount: Double,
    val notPay: Double,
    val realPay: Double,
    val isMandatory: Boolean
)

data class ParsedBudgetSheet(
    val monthKey: String,
    val openingBankBalance: Double,
    val expenses: List<BudgetExpenseItem>,
    val salaryBreakdown: Map<String, Double>,
    val totalExpensesColB: Double,
    val totalNotPaid: Double,
    val totalRealPay: Double,
    val savingAllocation: Double,
    val handSave: Double,
    val rawRowCount: Int
)
