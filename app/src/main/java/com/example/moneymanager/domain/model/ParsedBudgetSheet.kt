package com.example.moneymanager.domain.model

data class BudgetExpenseItem(
    val itemName: String,
    val amount: Double,
    val notPay: Double,
    val totalRealPay: Double,
    val isMandatory: Boolean,
    val category: String
)

data class ParsedBudgetSheet(
    val monthKey: String,
    val openingBankBalance: Double, // "Current Bank Rs"
    val expenseItems: List<BudgetExpenseItem>,
    val totalExpenseAmount: Double,
    val totalNotPaid: Double,
    val totalRealPay: Double,
    val salaryBreakdown: Map<String, Double>,
    val savingsTarget: Double,
    val basicSalary: Double,
    val salBalance: Double,
    val handSave: Double
)
