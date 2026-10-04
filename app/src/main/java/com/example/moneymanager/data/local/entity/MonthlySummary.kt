package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_summaries")
data class MonthlySummary(
    @PrimaryKey val month: String,
    val netSalary: Double,
    val openingBalance: Double,
    val totalRealExpenses: Double,
    val mandatoryExpenses: Double,
    val optionalExpenses: Double,
    val closingBalance: Double,
    val savingsAllocated: Double,
    val savingsAchieved: Boolean,
    val warnings: String,
    val computedAt: Long = System.currentTimeMillis()
)