package com.example.moneymanager.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.SalaryRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val month: String = "",
    val netSalary: Double = 0.0,
    val openingBalance: Double = 0.0,
    val totalRealPay: Double = 0.0,
    val mandatoryPay: Double = 0.0,
    val optionalPay: Double = 0.0,
    val notPaid: Double = 0.0,
    val closingBalance: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val isOverspent: Boolean = false,
    val hasPdfImported: Boolean = false,
    val hasSheetImported: Boolean = false,
    val expenseCount: Int = 0,
    val loading: Boolean = true
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val defaultMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _selectedMonth = MutableStateFlow(defaultMonth)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    /** All months that have data, newest first, always including current. */
    val allMonths: StateFlow<List<String>> =
        db.budgetMonthDao().getAllMonths()
            .map { list ->
                val keys = list.map { it.monthKey }.sortedDescending().toMutableList()
                if (!keys.contains(defaultMonth)) keys.add(0, defaultMonth)
                keys
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = listOf(defaultMonth)
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardState> = _selectedMonth.flatMapLatest { month ->
        combine(
            db.salaryRecordDao().getByMonth(month),
            db.budgetMonthDao().getByMonth(month),
            db.expenseLineDao().getExpensesForMonth(month)
        ) { salary: SalaryRecord?, budget: BudgetMonth?, expenses: List<ExpenseLine> ->

            val netSalary = salary?.netSalary ?: budget?.netSalaryFromPdf ?: 0.0
            val opening = budget?.openingBankBalance ?: 0.0
            val mandatory = budget?.totalMandatoryRealPay
                ?: expenses.filter { it.isMandatory }.sumOf { it.realPayAmount }
            val optional = budget?.totalOptionalRealPay
                ?: expenses.filterNot { it.isMandatory }.sumOf { it.realPayAmount }
            val totalReal = budget?.totalRealPay ?: (mandatory + optional)
            val notPaid = budget?.totalNotPaid ?: expenses.sumOf { it.notPaidAmount }
            val closing = budget?.closingBalance
                ?: (opening + netSalary - totalReal)

            DashboardState(
                month = month,
                netSalary = netSalary,
                openingBalance = opening,
                totalRealPay = totalReal,
                mandatoryPay = mandatory,
                optionalPay = optional,
                notPaid = notPaid,
                closingBalance = closing,
                savingsTarget = budget?.savingsTarget ?: 0.0,
                isOverspent = closing < 0,
                hasPdfImported = budget?.hasPdfImported ?: (salary != null),
                hasSheetImported = budget?.hasSheetImported ?: false,
                expenseCount = expenses.size,
                loading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState(month = defaultMonth)
    )

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }
}