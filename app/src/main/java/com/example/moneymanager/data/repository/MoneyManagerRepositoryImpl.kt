package com.example.moneymanager.data.repository

import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.MonthlySummary
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.domain.model.BudgetExpenseItem
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.example.moneymanager.domain.model.ReconciliationResult
import com.example.moneymanager.domain.usecase.ReconcileMonthlyFinancesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MoneyManagerRepositoryImpl(
    private val db: AppDatabase,
    private val reconcileUseCase: ReconcileMonthlyFinancesUseCase
) : MoneyManagerRepository {

    private val salaryDao = db.salaryRecordDao()
    private val expenseDao = db.expenseLineDao()
    private val monthDao = db.budgetMonthDao()
    private val summaryDao = db.monthlySummaryDao()

    override fun getAllMonths(): Flow<List<BudgetMonth>> = monthDao.getAllMonths()

    override fun getBudgetMonth(monthKey: String): Flow<BudgetMonth?> = monthDao.getByMonth(monthKey)

    override fun getSalaryRecord(monthKey: String): Flow<SalaryRecord?> = salaryDao.getByMonth(monthKey)

    override fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseLine>> =
        expenseDao.getExpensesForMonth(monthKey)

    override fun getMonthlySummary(monthKey: String): Flow<MonthlySummary?> =
        summaryDao.getByMonth(monthKey)

    override suspend fun saveSalarySlip(slip: ParsedSalarySlip): ReconciliationResult = withContext(Dispatchers.IO) {
        val record = SalaryRecord(
            monthKey = slip.monthKey,
            basicSalary = slip.basicSalary,
            vehicleAllowance = slip.vehicleAllowance,
            exceptionalIncentive = slip.exceptionalIncentive,
            shiftCompensation = slip.shiftCompensation,
            grossSalary = slip.grossSalary,
            totalForEpf = slip.totalForEpf,
            totalForEtf = slip.totalForEtf,
            totalForTax = slip.totalForTax,
            apit = slip.apit,
            lumpsumTax = slip.lumpsumTax,
            stampDuty = slip.stampDuty,
            epfEmployee = slip.epfEmployee,
            funeralFund = slip.funeralFund,
            excessMobile = slip.excessMobile,
            mealsDeduction = slip.mealsDeduction,
            totalDeductions = slip.totalDeductions,
            netSalary = slip.netSalary,
            cashSalary = slip.cashSalary,
            salaryToBank = slip.salaryToBank,
            epfEmployer = slip.epfEmployer,
            etfEmployer = slip.etfEmployer,
            stampDutyEmployer = slip.stampDutyEmployer,
            importedAt = System.currentTimeMillis()
        )
        salaryDao.insertOrUpdate(record)

        val currentMonth = monthDao.getByMonthSync(slip.monthKey)
        val updatedMonth = (currentMonth ?: BudgetMonth(monthKey = slip.monthKey)).copy(
            netSalaryFromPdf = slip.netSalary,
            hasPdfImported = true,
            updatedAt = System.currentTimeMillis()
        )
        monthDao.insertOrUpdate(updatedMonth)

        recalculateReconciliation(slip.monthKey)
    }

    override suspend fun saveBudgetSheet(sheet: ParsedBudgetSheet): ReconciliationResult = withContext(Dispatchers.IO) {
        // Delete previous expenses for this month to prevent duplication on re-import
        expenseDao.deleteForMonth(sheet.monthKey)

        val expenseEntities = sheet.expenses.map { item ->
            ExpenseLine(
                monthKey = sheet.monthKey,
                itemName = item.name,
                budgetAmount = item.amount,
                notPaidAmount = item.notPay,
                realPayAmount = item.realPay,
                isMandatory = item.isMandatory,
                category = if (item.isMandatory) "Mandatory" else "Optional"
            )
        }
        expenseDao.insertAll(expenseEntities)

        val currentMonth = monthDao.getByMonthSync(sheet.monthKey)
        val basicSalaryInSheet = sheet.salaryBreakdown["Basic Salary"] ?: 0.0
        val salBalance = sheet.salaryBreakdown["Sal"] ?: 0.0

        val updatedMonth = (currentMonth ?: BudgetMonth(monthKey = sheet.monthKey)).copy(
            openingBankBalance = sheet.openingBankBalance,
            totalMandatoryRealPay = sheet.expenses.filter { it.isMandatory }.sumOf { it.realPay },
            totalOptionalRealPay = sheet.expenses.filter { !it.isMandatory }.sumOf { it.realPay },
            totalRealPay = sheet.totalRealPay,
            totalNotPaid = sheet.totalNotPaid,
            savingsTarget = sheet.savingAllocation,
            sheetBasicSalary = basicSalaryInSheet,
            sheetSalBalance = salBalance,
            sheetHandSave = sheet.handSave,
            hasSheetImported = true,
            updatedAt = System.currentTimeMillis()
        )
        monthDao.insertOrUpdate(updatedMonth)

        recalculateReconciliation(sheet.monthKey)
    }

    override suspend fun toggleExpenseMandatory(id: Long, isMandatory: Boolean, monthKey: String): ReconciliationResult =
        withContext(Dispatchers.IO) {
            val category = if (isMandatory) "Mandatory" else "Optional"
            expenseDao.updateCategory(id, isMandatory, category)
            recalculateReconciliation(monthKey)
        }

    override suspend fun deleteMonthData(monthKey: String) = withContext(Dispatchers.IO) {
        salaryDao.deleteForMonth(monthKey)
        expenseDao.deleteForMonth(monthKey)
        summaryDao.deleteForMonth(monthKey)
        monthDao.deleteMonth(monthKey)
    }

    override suspend fun recalculateReconciliation(monthKey: String): ReconciliationResult = withContext(Dispatchers.IO) {
        val salaryRecord = salaryDao.getByMonthSync(monthKey)
        val expenseLines = expenseDao.getExpensesForMonthSync(monthKey)
        val budgetMonth = monthDao.getByMonthSync(monthKey)

        val slip = salaryRecord?.let {
            ParsedSalarySlip(
                monthKey = it.monthKey,
                basicSalary = it.basicSalary,
                vehicleAllowance = it.vehicleAllowance,
                exceptionalIncentive = it.exceptionalIncentive,
                shiftCompensation = it.shiftCompensation,
                grossSalary = it.grossSalary,
                totalForEpf = it.totalForEpf,
                totalForEtf = it.totalForEtf,
                totalForTax = it.totalForTax,
                apit = it.apit,
                lumpsumTax = it.lumpsumTax,
                stampDuty = it.stampDuty,
                epfEmployee = it.epfEmployee,
                funeralFund = it.funeralFund,
                excessMobile = it.excessMobile,
                mealsDeduction = it.mealsDeduction,
                totalDeductions = it.totalDeductions,
                netSalary = it.netSalary,
                cashSalary = it.cashSalary,
                salaryToBank = it.salaryToBank,
                epfEmployer = it.epfEmployer,
                etfEmployer = it.etfEmployer,
                stampDutyEmployer = it.stampDutyEmployer
            )
        }

        val sheet = budgetMonth?.let { bm ->
            val expenseItems = expenseLines.map { el ->
                BudgetExpenseItem(
                    name = el.itemName,
                    amount = el.budgetAmount,
                    notPay = el.notPaidAmount,
                    realPay = el.realPayAmount,
                    isMandatory = el.isMandatory
                )
            }
            val breakdown = mutableMapOf<String, Double>()
            if (bm.sheetBasicSalary > 0.0) breakdown["Basic Salary"] = bm.sheetBasicSalary
            if (bm.sheetSalBalance > 0.0) breakdown["Sal"] = bm.sheetSalBalance
            if (bm.sheetHandSave > 0.0) breakdown["Hand Save"] = bm.sheetHandSave
            if (bm.savingsTarget > 0.0) breakdown["Saving"] = bm.savingsTarget

            ParsedBudgetSheet(
                monthKey = bm.monthKey,
                openingBankBalance = bm.openingBankBalance,
                expenses = expenseItems,
                salaryBreakdown = breakdown,
                totalExpensesColB = expenseItems.sumOf { it.amount },
                totalNotPaid = expenseItems.sumOf { it.notPay },
                totalRealPay = expenseItems.sumOf { it.realPay },
                savingAllocation = bm.savingsTarget,
                handSave = bm.sheetHandSave,
                rawRowCount = expenseItems.size
            )
        }

        val result = reconcileUseCase.execute(slip, sheet, monthKey)

        // Update BudgetMonth entity with recalculated values
        if (budgetMonth != null) {
            val updated = budgetMonth.copy(
                openingBankBalance = result.openingBalance,
                netSalaryFromPdf = result.netSalary,
                totalMandatoryRealPay = result.mandatoryExpenses,
                totalOptionalRealPay = result.optionalExpenses,
                totalRealPay = result.totalRealPay,
                closingBalance = result.closingBalance,
                savingsTarget = result.savingsTarget,
                updatedAt = System.currentTimeMillis()
            )
            monthDao.insertOrUpdate(updated)
        }

        // Store summary with warnings
        val summary = MonthlySummary(
            monthKey = monthKey,
            warningsText = result.warnings.joinToString("\n"),
            notes = if (result.isOverspent) "Budget overspent" else "Budget balanced",
            updatedAt = System.currentTimeMillis()
        )
        summaryDao.insertOrUpdate(summary)

        result
    }
}
