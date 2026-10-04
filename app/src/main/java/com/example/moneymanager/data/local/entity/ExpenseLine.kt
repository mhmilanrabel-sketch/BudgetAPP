package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_lines",
    indices = [Index(value = ["monthKey"])]
)
data class ExpenseLine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val monthKey: String,
    val itemName: String,
    val budgetAmount: Double,
    val notPaidAmount: Double,
    val realPayAmount: Double,
    val isMandatory: Boolean,
    val category: String = if (isMandatory) "Mandatory" else "Optional"
)
