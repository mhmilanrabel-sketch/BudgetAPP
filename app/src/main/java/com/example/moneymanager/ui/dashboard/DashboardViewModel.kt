package com.example.moneymanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.domain.model.ReconciliationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardViewModel(
    private val repository: MoneyManagerRepository
) : ViewModel() {

    private val now = LocalDate.now()
    private val currentMonthKey = "${now.year}-${if (now.monthValue < 10) "0${now.monthValue}" else "${now.monthValue}"}"

    private val _selectedMonth = MutableStateFlow(currentMonthKey)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _reconciliation = MutableStateFlow<ReconciliationResult?>(null)
    val reconciliation: StateFlow<ReconciliationResult?> = _reconciliation.asStateFlow()

    val allMonths: StateFlow<List<BudgetMonth>> = repository.getAllMonths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentMonthData: StateFlow<BudgetMonth?> = _selectedMonth.flatMapLatest { key ->
        repository.getBudgetMonth(key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentSalaryRecord: StateFlow<SalaryRecord?> = _selectedMonth.flatMapLatest { key ->
        repository.getSalaryRecord(key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refresh()
    }

    fun selectMonth(monthKey: String) {
        _selectedMonth.value = monthKey
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val result = repository.reconcile(_selectedMonth.value)
            _reconciliation.value = result
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
