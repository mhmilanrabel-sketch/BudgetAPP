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
            rawText = slip.rawText
        )
        salaryDao.insertOrUpdate(record)

        val existingMonth = monthDao.getByMonthSync(slip.monthKey)
        val updatedMonth = existingMonth?.copy(
            netSalaryFromPdf = slip.netSalary,
            hasPdfImported = true,
            updatedAt = System.currentTimeMillis()
        ) ?: BudgetMonth(
            monthKey = slip.monthKey,
            netSalaryFromPdf = slip.netSalary,
            hasPdfImported = true
        )
        monthDao.insertOrUpdate(updatedMonth)

        reconcile(slip.monthKey)
    }

    override suspend fun saveBudgetSheet(sheet: ParsedBudgetSheet): ReconciliationResult = withContext(Dispatchers.IO) {
        // Replace existing expense lines for month
        expenseDao.deleteForMonth(sheet.monthKey)
        val entities = sheet.expenseItems.map { item ->
            ExpenseLine(
                monthKey = sheet.monthKey,
                itemName = item.itemName,
                budgetAmount = item.amount,
                notPaidAmount = item.notPay,
                realPayAmount = item.totalRealPay,
                isMandatory = item.isMandatory,
                category = item.category
            )
        }
        expenseDao.insertAll(entities)

        val existingMonth = monthDao.getByMonthSync(sheet.monthKey)
        val mandatoryRealPay = sheet.expenseItems.filter { it.isMandatory }.sumOf { it.totalRealPay }
        val optionalRealPay = sheet.expenseItems.filter { !it.isMandatory }.sumOf { it.totalRealPay }

        val updatedMonth = (existingMonth ?: BudgetMonth(monthKey = sheet.monthKey)).copy(
            openingBankBalance = sheet.openingBankBalance,
            totalMandatoryRealPay = mandatoryRealPay,
            totalOptionalRealPay = optionalRealPay,
            totalRealPay = sheet.totalRealPay,
            totalNotPaid = sheet.totalNotPaid,
            savingsTarget = sheet.savingsTarget,
            sheetBasicSalary = sheet.basicSalary,
            sheetSalBalance = sheet.salBalance,
            sheetHandSave = sheet.handSave,
            hasSheetImported = true,
            updatedAt = System.currentTimeMillis()
        )
        monthDao.insertOrUpdate(updatedMonth)

        reconcile(sheet.monthKey)
    }

    override suspend fun updateExpenseCategory(
        expenseId: Long,
        isMandatory: Boolean,
        category: String,
        monthKey: String
    ): ReconciliationResult = withContext(Dispatchers.IO) {
        expenseDao.updateCategory(expenseId, isMandatory, category)
        reconcile(monthKey)
    }

    override suspend fun reconcile(monthKey: String): ReconciliationResult = withContext(Dispatchers.IO) {
        val salaryRecord = salaryDao.getByMonthSync(monthKey)
        val budgetMonth = monthDao.getByMonthSync(monthKey)
        val expenses = expenseDao.getExpensesForMonthSync(monthKey)

        val result = reconcileUseCase.execute(monthKey, salaryRecord, budgetMonth, expenses)

        // Update budget month totals & closing balance
        budgetMonth?.let {
            val updated = it.copy(
                closingBalance = result.closingBalance,
                totalMandatoryRealPay = result.mandatoryRealPay,
                totalOptionalRealPay = result.optionalRealPay,
                totalRealPay = result.totalRealPay,
                totalNotPaid = result.totalNotPaid,
                updatedAt = System.currentTimeMillis()
            )
            monthDao.insertOrUpdate(updated)
        }

        // Cache summary warnings
        val warningsText = result.discrepancies.joinToString("\n") { "[${it.severity}] ${it.title}: ${it.description}" }
        summaryDao.insertOrUpdate(
            MonthlySummary(
                monthKey = monthKey,
                warningsText = warningsText,
                notes = if (result.isOverspent) "⚠️ OVERSPENT BY ${result.overspentAmount}" else "Reconciled OK"
            )
        )

        result
    }

    override suspend fun deleteMonthData(monthKey: String) = withContext(Dispatchers.IO) {
        salaryDao.deleteForMonth(monthKey)
        expenseDao.deleteForMonth(monthKey)
        summaryDao.deleteForMonth(monthKey)
        monthDao.deleteMonth(monthKey)
    }
}
