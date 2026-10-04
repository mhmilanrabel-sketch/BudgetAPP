package com.example.moneymanager.data.repository

import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.MonthlySummary
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.example.moneymanager.domain.model.ReconciliationResult
import kotlinx.coroutines.flow.Flow

interface MoneyManagerRepository {
    fun getAllMonths(): Flow<List<BudgetMonth>>
    fun getBudgetMonth(monthKey: String): Flow<BudgetMonth?>
    fun getSalaryRecord(monthKey: String): Flow<SalaryRecord?>
    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseLine>>
    fun getMonthlySummary(monthKey: String): Flow<MonthlySummary?>

    suspend fun saveSalarySlip(slip: ParsedSalarySlip): ReconciliationResult
    suspend fun saveBudgetSheet(sheet: ParsedBudgetSheet): ReconciliationResult
    suspend fun toggleExpenseMandatory(id: Long, isMandatory: Boolean, monthKey: String): ReconciliationResult
    suspend fun deleteMonthData(monthKey: String)
    suspend fun recalculateReconciliation(monthKey: String): ReconciliationResult
}
