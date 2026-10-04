package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_lines",
    indices = [Index(value = ["month"])]
)
data class ExpenseLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val item: String,
    val amount: Double,
    val notPaid: Double,
    val realPay: Double,
    val isMandatory: Boolean
)