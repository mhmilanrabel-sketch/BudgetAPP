package com.example

import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.domain.model.DiscrepancySeverity
import com.example.moneymanager.domain.usecase.ReconcileMonthlyFinancesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconciliationUseCaseTest {

    private val useCase = ReconcileMonthlyFinancesUseCase()

    @Test
    fun execute_balancedSriLankanBudget_computesCorrectClosingBalance() {
        val budgetMonth = BudgetMonth(
            monthKey = "2026-10",
            openingBankBalance = 3500.00,
            sheetBasicSalary = 192455.00,
            savingsTarget = 25000.00
        )

        val salaryRecord = SalaryRecord(
            monthKey = "2026-10",
            basicSalary = 192455.00,
            grossSalary = 246569.00,
            totalDeductions = 39146.40,
            netSalary = 207422.60
        )

        val expenses = listOf(
            ExpenseLine(id = 1, monthKey = "2026-10", itemName = "Rent", budgetAmount = 45000.0, notPaidAmount = 0.0, realPayAmount = 45000.0, isMandatory = true),
            ExpenseLine(id = 2, monthKey = "2026-10", itemName = "CEB Electricity", budgetAmount = 12500.0, notPaidAmount = 0.0, realPayAmount = 12500.0, isMandatory = true),
            ExpenseLine(id = 3, monthKey = "2026-10", itemName = "Credit Card", budgetAmount = 35000.0, notPaidAmount = 15000.0, realPayAmount = 20000.0, isMandatory = true),
            ExpenseLine(id = 4, monthKey = "2026-10", itemName = "Dining Out", budgetAmount = 10000.0, notPaidAmount = 0.0, realPayAmount = 10000.0, isMandatory = false)
        )

        val result = useCase.execute("2026-10", salaryRecord, budgetMonth, expenses)

        assertNotNull(result)
        assertEquals(3500.00, result.openingBalance, 0.01)
        assertEquals(207422.60, result.netSalary, 0.01)
        assertEquals(87500.00, result.totalRealPay, 0.01)
        assertEquals(15000.00, result.totalNotPaid, 0.01)
        assertEquals(77500.00, result.mandatoryRealPay, 0.01)
        assertEquals(10000.00, result.optionalRealPay, 0.01)

        // 3,500.00 + 207,422.60 - 87,500.00 = 123,422.60
        assertEquals(123422.60, result.closingBalance, 0.01)
        assertFalse(result.isOverspent)
    }

    @Test
    fun execute_overspentBudget_detectsErrorSeverityDeficit() {
        val budgetMonth = BudgetMonth(
            monthKey = "2026-10",
            openingBankBalance = 1000.00,
            sheetBasicSalary = 100000.00
        )
        val salaryRecord = SalaryRecord(
            monthKey = "2026-10",
            basicSalary = 100000.00,
            grossSalary = 100000.00,
            totalDeductions = 10000.00,
            netSalary = 90000.00
        )
        val expenses = listOf(
            ExpenseLine(id = 1, monthKey = "2026-10", itemName = "Major Obligation", budgetAmount = 150000.0, notPaidAmount = 0.0, realPayAmount = 150000.0, isMandatory = true)
        )

        val result = useCase.execute("2026-10", salaryRecord, budgetMonth, expenses)

        // 1000 + 90000 - 150000 = -59000
        assertTrue(result.isOverspent)
        assertEquals(59000.00, result.overspentAmount, 0.01)
        assertTrue(result.discrepancies.any { it.severity == DiscrepancySeverity.ERROR })
    }
}
