package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_summaries")
data class MonthlySummary(
    @PrimaryKey
    val monthKey: String,
    val warningsText: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
