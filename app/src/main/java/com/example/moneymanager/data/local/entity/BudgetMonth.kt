package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_months")
data class BudgetMonth(
    @PrimaryKey
    val monthKey: String, // e.g. "2026-10"
    val openingBankBalance: Double = 0.0,
    val netSalaryFromPdf: Double = 0.0,
    val totalMandatoryRealPay: Double = 0.0,
    val totalOptionalRealPay: Double = 0.0,
    val totalRealPay: Double = 0.0,
    val totalNotPaid: Double = 0.0,
    val closingBalance: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val sheetBasicSalary: Double = 0.0,
    val sheetSalBalance: Double = 0.0,
    val sheetHandSave: Double = 0.0,
    val hasPdfImported: Boolean = false,
    val hasSheetImported: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
