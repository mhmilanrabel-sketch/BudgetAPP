package com.example.moneymanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.MonthlySummary
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.data.repository.MoneyManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val selectedMonthKey: String = CurrencyFormatter.getCurrentMonthKey(),
    val budgetMonth: BudgetMonth? = null,
    val salaryRecord: SalaryRecord? = null,
    val monthlySummary: MonthlySummary? = null,
    val isLoading: Boolean = false,
    val warnings: List<String> = emptyList()
)

class DashboardViewModel(
    private val repository: MoneyManagerRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(CurrencyFormatter.getCurrentMonthKey())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val budgetMonth: StateFlow<BudgetMonth?> = _selectedMonth
        .flatMapLatest { monthKey -> repository.getBudgetMonth(monthKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val salaryRecord: StateFlow<SalaryRecord?> = _selectedMonth
        .flatMapLatest { monthKey -> repository.getSalaryRecord(monthKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthlySummary: StateFlow<MonthlySummary?> = _selectedMonth
        .flatMapLatest { monthKey -> repository.getMonthlySummary(monthKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectMonth(monthKey: String) {
        _selectedMonth.value = monthKey
        viewModelScope.launch {
            repository.recalculateReconciliation(monthKey)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.recalculateReconciliation(_selectedMonth.value)
        }
    }

    companion object {
        fun provideFactory(repository: MoneyManagerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
