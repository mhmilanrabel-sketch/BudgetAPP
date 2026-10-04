package com.example.moneymanager.domain.usecase

import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.domain.model.Discrepancy
import com.example.moneymanager.domain.model.DiscrepancySeverity
import com.example.moneymanager.domain.model.ReconciliationResult
import kotlin.math.abs

class ReconcileMonthlyFinancesUseCase {

    fun execute(
        monthKey: String,
        salaryRecord: SalaryRecord?,
        budgetMonth: BudgetMonth?,
        expenses: List<ExpenseLine>
    ): ReconciliationResult {
        val discrepancies = mutableListOf<Discrepancy>()

        val openingBalance = budgetMonth?.openingBankBalance ?: 0.0
        val netSalary = salaryRecord?.netSalary ?: budgetMonth?.netSalaryFromPdf ?: 0.0

        val totalRealPay = expenses.sumOf { it.realPayAmount }
        val totalNotPaid = expenses.sumOf { it.notPaidAmount }
        val mandatoryRealPay = expenses.filter { it.isMandatory }.sumOf { it.realPayAmount }
        val optionalRealPay = expenses.filter { !it.isMandatory }.sumOf { it.realPayAmount }

        val closingBalance = openingBalance + netSalary - totalRealPay
        val isOverspent = closingBalance < 0.0
        val overspentAmount = if (isOverspent) abs(closingBalance) else 0.0

        if (isOverspent) {
            discrepancies.add(
                Discrepancy(
                    title = "Overspent Deficit Alert",
                    description = "Projected closing balance is negative (${CurrencyFormatter.formatLkr(closingBalance)}). Real spending exceeds available opening bank balance + net salary by ${CurrencyFormatter.formatLkr(overspentAmount)}.",
                    severity = DiscrepancySeverity.ERROR
                )
            )
        }

        // Basic salary comparison between PDF & Sheet
        if (salaryRecord != null && budgetMonth != null && budgetMonth.sheetBasicSalary > 0.0) {
            val diff = abs(salaryRecord.basicSalary - budgetMonth.sheetBasicSalary)
            if (diff > 1.0) {
                discrepancies.add(
                    Discrepancy(
                        title = "Basic Salary Variance",
                        description = "Payslip Basic Salary (${CurrencyFormatter.formatLkr(salaryRecord.basicSalary)}) differs from Budget Sheet Basic Salary (${CurrencyFormatter.formatLkr(budgetMonth.sheetBasicSalary)}) by ${CurrencyFormatter.formatLkr(diff)}.",
                        severity = DiscrepancySeverity.WARNING
                    )
                )
            }
        }

        // Check if carried forward not-paid expenses exist
        if (totalNotPaid > 0.0) {
            discrepancies.add(
                Discrepancy(
                    title = "Carried Forward Obligations",
                    description = "A total of ${CurrencyFormatter.formatLkr(totalNotPaid)} in expenses were marked as 'Not pay' and carried forward.",
                    severity = DiscrepancySeverity.INFO
                )
            )
        }

        // Savings target evaluation
        val savingsTarget = budgetMonth?.savingsTarget ?: 0.0
        if (savingsTarget > 0.0 && closingBalance < savingsTarget) {
            val shortfall = savingsTarget - closingBalance
            discrepancies.add(
                Discrepancy(
                    title = "Savings Target Shortfall",
                    description = "Target savings of ${CurrencyFormatter.formatLkr(savingsTarget)} will fall short by ${CurrencyFormatter.formatLkr(shortfall)} based on current real pay outflows.",
                    severity = DiscrepancySeverity.WARNING
                )
            )
        }

        return ReconciliationResult(
            monthKey = monthKey,
            openingBalance = openingBalance,
            netSalary = netSalary,
            totalRealPay = totalRealPay,
            totalNotPaid = totalNotPaid,
            mandatoryRealPay = mandatoryRealPay,
            optionalRealPay = optionalRealPay,
            closingBalance = closingBalance,
            savingsTarget = savingsTarget,
            isOverspent = isOverspent,
            overspentAmount = overspentAmount,
            discrepancies = discrepancies
        )
    }
}
