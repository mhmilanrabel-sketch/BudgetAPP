package com.example

import com.example.moneymanager.domain.model.BudgetExpenseItem
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.example.moneymanager.domain.usecase.ReconcileMonthlyFinancesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationUseCaseTest {

    private val useCase = ReconcileMonthlyFinancesUseCase()

    @Test
    fun execute_reconcilesBalanceAndDetectsWarnings() {
        val slip = ParsedSalarySlip(
            monthKey = "2026-10",
            basicSalary = 192455.33,
            grossSalary = 268569.16,
            totalDeductions = 40082.10,
            netSalary = 228487.06
        )

        val expenses = listOf(
            BudgetExpenseItem("Rent/mortgage", 25000.0, 0.0, 25000.0, true),
            BudgetExpenseItem("Maiyon Van", 12000.0, 12000.0, 0.0, true),
            BudgetExpenseItem("Sachini", 110000.0, 100000.0, 10000.0, false)
        )

        val sheet = ParsedBudgetSheet(
            monthKey = "2026-10",
            openingBankBalance = 100000.0,
            expenses = expenses,
            salaryBreakdown = mapOf(
                "Basic Salary" to 192455.00,
                "Sal" to 240760.00
            ),
            totalExpensesColB = 147000.0,
            totalNotPaid = 112000.0,
            totalRealPay = 35000.0,
            savingAllocation = 50000.0,
            handSave = 40000.0,
            rawRowCount = 3
        )

        val result = useCase.execute(slip, sheet, "2026-10")

        assertEquals(228487.06, result.netSalary, 0.01)
        assertEquals(100000.00, result.openingBalance, 0.01)
        assertEquals(25000.00, result.mandatoryExpenses, 0.01)
        assertEquals(10000.00, result.optionalExpenses, 0.01)
        assertEquals(35000.00, result.totalRealPay, 0.01)

        // closingBalance = opening (100,000) + netSalary (228,487.06) - realPay (35,000) = 293,487.06
        val expectedClosing = 100000.0 + 228487.06 - 35000.0
        assertEquals(expectedClosing, result.closingBalance, 0.01)

        assertFalse(result.isOverspent)
        assertTrue(result.isSavingsTargetMet) // 293,487.06 >= 50,000
        assertTrue(result.hasCarryForwardUnpaid) // 112,000 unpaid

        // Check that unpaid warning was generated
        assertTrue(result.warnings.any { it.contains("Carry-forward") })
        // Check that Sal variance warning was generated
        assertTrue(result.warnings.any { it.contains("Income variance") })
    }
}
