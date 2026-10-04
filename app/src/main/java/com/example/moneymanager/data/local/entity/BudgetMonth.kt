package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_months")
data class BudgetMonth(
    @PrimaryKey val month: String,
    val openingBankBalance: Double,
    val totalAmount: Double,
    val totalNotPaid: Double,
    val totalRealPay: Double,
    val handSave: Double,
    val savingAllocation: Double,
    val otherAllocation: Double,
    val importedAt: Long = System.currentTimeMillis()
)