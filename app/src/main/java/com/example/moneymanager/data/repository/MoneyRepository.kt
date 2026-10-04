package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetMonth
import com.example.data.local.entity.ExpenseLine
import com.example.data.local.entity.MonthlySummary
import com.example.data.local.entity.SalaryRecord
import kotlinx.coroutines.flow.Flow

class MoneyRepository private constructor(private val db: AppDatabase) {

    fun observeSalary(month: String): Flow<SalaryRecord?> =
        db.salaryRecordDao().observeByMonth(month)

    suspend fun saveSalary(record: SalaryRecord) =
        db.salaryRecordDao().insert(record)

    fun observeExpenses(month: String): Flow<List<ExpenseLine>> =
        db.expenseLineDao().observeByMonth(month)

    suspend fun saveExpenses(lines: List<ExpenseLine>) {
        val month = lines.firstOrNull()?.month ?: return
        db.expenseLineDao().deleteByMonth(month)
        db.expenseLineDao().insertAll(lines)
    }

    fun observeBudget(month: String): Flow<BudgetMonth?> =
        db.budgetMonthDao().observeByMonth(month)

    suspend fun saveBudget(budget: BudgetMonth) =
        db.budgetMonthDao().upsert(budget)

    fun observeSummary(month: String): Flow<MonthlySummary?> =
        db.monthlySummaryDao().observeByMonth(month)

    suspend fun saveSummary(summary: MonthlySummary) =
        db.monthlySummaryDao().upsert(summary)

    companion object {
        @Volatile private var INSTANCE: MoneyRepository? = null

        fun getInstance(context: Context): MoneyRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MoneyRepository(
                    AppDatabase.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
    }
}