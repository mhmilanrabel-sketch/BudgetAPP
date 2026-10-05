package com.example.moneymanager.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.BudgetCategory
import com.example.moneymanager.data.local.entity.ExpenseLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryProgress(
    val category: BudgetCategory,
    val spent: Double,
    val matchedItems: List<String>
) {
    val fraction: Float
        get() = if (category.monthlyLimit <= 0) 0f
                else (spent / category.monthlyLimit).coerceIn(0.0, 1.0).toFloat()

    val isOverBudget: Boolean
        get() = spent > category.monthlyLimit

    val remaining: Double
        get() = (category.monthlyLimit - spent).coerceAtLeast(0.0)

    val percent: Int
        get() = if (category.monthlyLimit <= 0) 0
                else ((spent / category.monthlyLimit) * 100).toInt()
}

data class BudgetState(
    val month: String = "",
    val items: List<CategoryProgress> = emptyList(),
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val loading: Boolean = true
)

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val defaultMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _selectedMonth = MutableStateFlow(defaultMonth)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _editing = MutableStateFlow<BudgetCategory?>(null)
    val editing: StateFlow<BudgetCategory?> = _editing.asStateFlow()

    private val _dialogOpen = MutableStateFlow(false)
    val dialogOpen: StateFlow<Boolean> = _dialogOpen.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<BudgetState> = _selectedMonth.flatMapLatest { month ->
        combine(
            db.budgetCategoryDao().getAll(),
            db.expenseLineDao().getExpensesForMonth(month)
        ) { categories, expenses ->
            val progresses = categories.map { cat ->
                CategoryProgress(
                    category = cat,
                    spent = sumMatching(cat, expenses),
                    matchedItems = expenses.filter { matches(cat, it) }
                        .map { it.itemName }
                )
            }
            BudgetState(
                month = month,
                items = progresses,
                totalLimit = progresses.sumOf { it.category.monthlyLimit },
                totalSpent = progresses.sumOf { it.spent },
                loading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetState(month = defaultMonth)
    )

    fun selectMonth(month: String) { _selectedMonth.value = month }

    fun startEditing(cat: BudgetCategory?) {
        _editing.value = cat
        _dialogOpen.value = true
    }

    fun stopEditing() {
        _editing.value = null
        _dialogOpen.value = false
    }

    fun saveCategory(name: String, limit: Double, keywords: String, colorHex: String) {
        viewModelScope.launch {
            val existing = _editing.value
            db.budgetCategoryDao().upsert(
                BudgetCategory(
                    id = existing?.id ?: 0,
                    name = name.trim(),
                    keywordsCsv = keywords.trim(),
                    monthlyLimit = limit,
                    colorHex = colorHex
                )
            )
            stopEditing()
        }
    }

    fun deleteCategory(cat: BudgetCategory) {
        viewModelScope.launch {
            db.budgetCategoryDao().delete(cat)
            stopEditing()
        }
    }

    private fun matches(cat: BudgetCategory, line: ExpenseLine): Boolean {
        val keywords = cat.keywordsCsv
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        if (keywords.isEmpty()) return false
        val name = line.itemName.lowercase()
        return keywords.any { name.contains(it) }
    }

    private fun sumMatching(cat: BudgetCategory, expenses: List<ExpenseLine>): Double {
        return expenses.filter { matches(cat, it) }.sumOf { it.realPayAmount }
    }
}