package com.example.moneymanager.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.CategoryBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryBudgetProgress(
    val category: String,
    val limit: Double,
    val spent: Double,
    val colorHex: String
) {
    val fraction: Float
        get() = if (limit <= 0) 0f else (spent / limit).coerceIn(0.0, 1.0).toFloat()

    val isOverBudget: Boolean
        get() = spent > limit

    val remaining: Double
        get() = (limit - spent).coerceAtLeast(0.0)
}

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val budgetDao = db.categoryBudgetDao()
    private val expenseDao = db.expenseLineDao()

    private val currentMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _month = MutableStateFlow(currentMonth)
    val month: StateFlow<String> = _month.asStateFlow()

    private val _progress = MutableStateFlow<List<CategoryBudgetProgress>>(emptyList())
    val progress: StateFlow<List<CategoryBudgetProgress>> = _progress.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                budgetDao.observeByMonth(currentMonth),
                expenseDao.observeByMonth(currentMonth)
            ) { budgets, expenses ->
                budgets.map { b ->
                    val spent = expenses
                        .filter { it.item.equals(b.category, ignoreCase = true) }
                        .sumOf { it.realPay }
                    CategoryBudgetProgress(
                        category = b.category,
                        limit = b.monthlyLimit,
                        spent = spent,
                        colorHex = b.colorHex
                    )
                }
            }.collect { _progress.value = it }
        }
    }

    fun setLimit(category: String, limit: Double, colorHex: String = "#4CAF50") {
        viewModelScope.launch {
            budgetDao.upsert(
                CategoryBudget(
                    month = currentMonth,
                    category = category,
                    monthlyLimit = limit,
                    colorHex = colorHex
                )
            )
        }
    }

    fun removeBudget(category: String) {
        viewModelScope.launch {
            budgetDao.deleteByCategory(currentMonth, category)
        }
    }
}