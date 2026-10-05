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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val month: String = "",

    // From PDF
    val basicSalary: Double = 0.0,
    val vehicleAllowance: Double = 0.0,
    val exceptionalIncentive: Double = 0.0,
    val shiftCompensation: Double = 0.0,
    val grossSalary: Double = 0.0,
    val apit: Double = 0.0,
    val epfEmployee: Double = 0.0,
    val funeralFund: Double = 0.0,
    val excessMobile: Double = 0.0,
    val mealsDeduction: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val totalForEpf: Double = 0.0,
    val totalForEtf: Double = 0.0,
    val totalForTax: Double = 0.0,
    val netSalary: Double = 0.0,
    val salaryToBank: Double = 0.0,

    // Budget
    val openingBalance: Double = 0.0,
    val mandatoryPay: Double = 0.0,
    val optionalPay: Double = 0.0,
    val totalRealPay: Double = 0.0,
    val notPaid: Double = 0.0,
    val closingBalance: Double = 0.0,

    // Savings
    val savingsTarget: Double = 0.0,

    // Meta
    val isOverspent: Boolean = false,
    val hasPdfImported: Boolean = false,
    val hasSheetImported: Boolean = false,
    val expenseCount: Int = 0,
    val loading: Boolean = true
) {
    val savingsActual: Double get() = closingBalance.coerceAtLeast(0.0)
    val savingsAchieved: Boolean
        get() = savingsTarget > 0 && savingsActual >= savingsTarget
    val savingsPercent: Int
        get() = if (savingsTarget <= 0) 0
                else ((savingsActual / savingsTarget) * 100).toInt()

    /** True when expenses exist but all have 0 amounts (incomplete data). */
    val expensesHaveNoAmounts: Boolean
        get() = expenseCount > 0 && totalRealPay == 0.0
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val defaultMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _selectedMonth = MutableStateFlow(defaultMonth)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val allMonths: StateFlow<List<String>> =
        db.budgetMonthDao().getAllMonths()
            .map { list ->
                val keys = list.map { it.monthKey }
                    .filterNot { it == "__TEMPLATE__" }
                    .sortedDescending()
                    .toMutableList()
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

            val mandatory = expenses.filter { it.isMandatory }
                .sumOf { it.realPayAmount }
            val optional = expenses.filterNot { it.isMandatory }
                .sumOf { it.realPayAmount }
            val totalReal = mandatory + optional
            val notPaid = expenses.sumOf { it.notPaidAmount }

            val finalMandatory = if (expenses.isNotEmpty()) mandatory
                                 else budget?.totalMandatoryRealPay ?: 0.0
            val finalOptional = if (expenses.isNotEmpty()) optional
                                else budget?.totalOptionalRealPay ?: 0.0
            val finalTotalReal = if (expenses.isNotEmpty()) totalReal
                                 else budget?.totalRealPay
                                      ?: (finalMandatory + finalOptional)
            val finalNotPaid = if (expenses.isNotEmpty()) notPaid
                               else budget?.totalNotPaid ?: 0.0

            val opening = budget?.openingBankBalance ?: 0.0
            val netSalary = salary?.netSalary ?: budget?.netSalaryFromPdf ?: 0.0
            val closing = opening + netSalary - finalTotalReal

            DashboardState(
                month = month,

                basicSalary = salary?.basicSalary ?: 0.0,
                vehicleAllowance = salary?.vehicleAllowance ?: 0.0,
                exceptionalIncentive = salary?.exceptionalIncentive ?: 0.0,
                shiftCompensation = salary?.shiftCompensation ?: 0.0,
                grossSalary = salary?.grossSalary ?: 0.0,
                apit = salary?.apit ?: 0.0,
                epfEmployee = salary?.epfEmployee ?: 0.0,
                funeralFund = salary?.funeralFund ?: 0.0,
                excessMobile = salary?.excessMobile ?: 0.0,
                mealsDeduction = salary?.mealsDeduction ?: 0.0,
                totalDeductions = salary?.totalDeductions ?: 0.0,
                totalForEpf = salary?.totalForEpf ?: 0.0,
                totalForEtf = salary?.totalForEtf ?: 0.0,
                totalForTax = salary?.totalForTax ?: 0.0,
                netSalary = netSalary,
                salaryToBank = salary?.salaryToBank ?: 0.0,

                openingBalance = opening,
                mandatoryPay = finalMandatory,
                optionalPay = finalOptional,
                totalRealPay = finalTotalReal,
                notPaid = finalNotPaid,
                closingBalance = closing,

                savingsTarget = budget?.savingsTarget ?: 0.0,

                isOverspent = closing < 0,
                hasPdfImported = salary != null,
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

    fun selectMonth(month: String) { _selectedMonth.value = month }

    /** Save a savings target for the currently selected month. */
    fun setSavingsTarget(target: Double) {
        viewModelScope.launch {
            val month = _selectedMonth.value
            val existing = db.budgetMonthDao().getByMonthSync(month)
            val row = (existing ?: BudgetMonth(monthKey = month))
                .copy(savingsTarget = target)
            db.budgetMonthDao().insertOrUpdate(row)
        }
    }
}