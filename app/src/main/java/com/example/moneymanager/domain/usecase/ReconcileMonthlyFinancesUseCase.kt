package com.example.moneymanager.domain.usecase

import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.example.moneymanager.domain.model.ReconciliationResult
import kotlin.math.abs

class ReconcileMonthlyFinancesUseCase {

    companion object {
        private const val TOLERANCE = 1.0 // ±1 LKR
    }

    fun execute(
        slip: ParsedSalarySlip?,
        sheet: ParsedBudgetSheet?,
        monthKey: String
    ): ReconciliationResult {
        val netSalary = slip?.netSalary ?: 0.0
        val openingBalance = sheet?.openingBankBalance ?: 0.0

        val mandatoryExpenses = sheet?.expenses
            ?.filter { it.isMandatory }
            ?.sumOf { it.realPay } ?: 0.0

        val optionalExpenses = sheet?.expenses
            ?.filter { !it.isMandatory }
            ?.sumOf { it.realPay } ?: 0.0

        val totalRealPay = sheet?.totalRealPay ?: (mandatoryExpenses + optionalExpenses)
        val closingBalance = openingBalance + netSalary - totalRealPay
        val savingsTarget = sheet?.savingAllocation ?: 0.0
        val totalNotPaid = sheet?.totalNotPaid ?: 0.0

        val warnings = mutableListOf<String>()

        // 1. slip.basicSalary != sheet.salaryBreakdown["Basic Salary"] (±1 LKR)
        if (slip != null && sheet != null && sheet.salaryBreakdown.containsKey("Basic Salary")) {
            val sheetBasic = sheet.salaryBreakdown["Basic Salary"] ?: 0.0
            val diff = abs(slip.basicSalary - sheetBasic)
            if (diff > TOLERANCE) {
                warnings.add(
                    "Basic salary mismatch: PDF slip is ${CurrencyFormatter.formatLkr(slip.basicSalary)}, but Budget sheet lists ${CurrencyFormatter.formatLkr(sheetBasic)}."
                )
            }
        }

        // 2. sheet.totalRealPay != (mandatory + optional) (±1 LKR)
        val computedExpensesSum = mandatoryExpenses + optionalExpenses
        if (abs(totalRealPay - computedExpensesSum) > TOLERANCE) {
            warnings.add(
                "Expense calculation discrepancy: Total real pay (${CurrencyFormatter.formatLkr(totalRealPay)}) does not match sum of mandatory and optional items (${CurrencyFormatter.formatLkr(computedExpensesSum)})."
            )
        }

        // 3. closingBalance < 0 -> Overspent
        val isOverspent = closingBalance < 0.0
        if (isOverspent) {
            val overspentAmt = abs(closingBalance)
            warnings.add("⚠️ Overspent by ${CurrencyFormatter.formatLkr(overspentAmt)}")
        }

        // 4. sheet.totalNotPaid > 0 -> Unpaid / carry forward
        val hasCarryForward = totalNotPaid > 0.0
        if (hasCarryForward) {
            warnings.add("Carry-forward: ${CurrencyFormatter.formatLkr(totalNotPaid)} unpaid")
        }

        // 5. closingBalance < savingsTarget -> Savings target not met
        val isSavingsTargetMet = closingBalance >= savingsTarget
        if (savingsTarget > 0.0 && closingBalance < savingsTarget) {
            warnings.add("Savings target ${CurrencyFormatter.formatLkr(savingsTarget)} not met")
        }

        // 6. Net salary differs from sheet "Sal" or "Hand Save" figure by > 1 LKR
        var netMatchesSheetSal = true
        if (slip != null && sheet != null) {
            val sheetSal = sheet.salaryBreakdown["Sal"]
            val sheetHandSave = sheet.salaryBreakdown["Hand Save"]

            if (sheetSal != null && sheetSal > 0.0 && abs(slip.netSalary - sheetSal) > TOLERANCE) {
                warnings.add(
                    "Income variance: Slip Net Salary (${CurrencyFormatter.formatLkr(slip.netSalary)}) differs from Budget Sheet 'Sal' (${CurrencyFormatter.formatLkr(sheetSal)})."
                )
                netMatchesSheetSal = false
            } else if (sheetHandSave != null && sheetHandSave > 0.0 && abs(slip.netSalary - sheetHandSave) > TOLERANCE) {
                warnings.add(
                    "Notice: Net salary (${CurrencyFormatter.formatLkr(slip.netSalary)}) differs from 'Hand Save' target (${CurrencyFormatter.formatLkr(sheetHandSave)})."
                )
            }
        }

        return ReconciliationResult(
            monthKey = monthKey,
            netSalary = netSalary,
            openingBalance = openingBalance,
            mandatoryExpenses = mandatoryExpenses,
            optionalExpenses = optionalExpenses,
            totalRealPay = totalRealPay,
            closingBalance = closingBalance,
            savingsTarget = savingsTarget,
            warnings = warnings,
            isOverspent = isOverspent,
            isSavingsTargetMet = isSavingsTargetMet,
            hasCarryForwardUnpaid = hasCarryForward,
            netSalaryMatchesHandSave = netMatchesSheetSal
        )
    }
}
