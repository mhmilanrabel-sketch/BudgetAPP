package com.example.moneymanager.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.repository.MoneyManagerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: MoneyManagerRepository
) : ViewModel() {

    val allMonths: StateFlow<List<BudgetMonth>> = repository.getAllMonths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMonth(monthKey: String) {
        viewModelScope.launch {
            repository.deleteMonthData(monthKey)
        }
    }

    companion object {
        fun provideFactory(repository: MoneyManagerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
