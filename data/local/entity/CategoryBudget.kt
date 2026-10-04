package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["month", "category"], unique = true)]
)
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val category: String,
    val monthlyLimit: Double,
    val colorHex: String = "#4CAF50"
)