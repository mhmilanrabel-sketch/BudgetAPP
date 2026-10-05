package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_categories")
data class BudgetCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Comma-separated keywords. Any expense whose item name contains
     *  any of these (case-insensitive substring) counts toward this category. */
    val keywordsCsv: String,
    val monthlyLimit: Double,
    val colorHex: String = "#4CAF50"
)