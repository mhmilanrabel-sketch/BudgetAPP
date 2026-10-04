package com.example

import com.example.moneymanager.data.parser.BudgetSheetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BudgetSheetParserTest {

    private val parser by lazy {
        BudgetSheetParser(RuntimeEnvironment.getApplication())
    }

    @Test
    fun parseCsv_twoSectionRealWorldData_parsesAccurately() {
        val realWorldCsv = """
Current Bank Rs,3500.00,,,,,,,,,,
Item,Amount,Not pay,Total real pay,,,,,,MONTHLY Fixed Expenses,Basic Salary,192455.00
House Rent,45000.00,0,45000.00,,,,,,Fixed,Vehicle Allowance,48114.00
Electricity Bill CEB,12500.00,0,12500.00,,,,,,Fixed,Exceptional Incentive,4000.00
SLT Fiber Internet,5800.00,0,5800.00,,,,,,Fixed,Shift Compensation,2000.00
Personal Loan Commercial Bank,28000.00,0,28000.00,,,,,,Fixed,Gross Salary,246569.00
Credit Card Payment,35000.00,15000.00,20000.00,,,,,,Fixed,APIT,18450.00
Car Insurance Premium,14000.00,14000.00,0.00,,,,,,Fixed,EPF,15396.40
Supermarket Groceries,38000.00,0,38000.00,,,,,,Fixed,Funeral Fund,500.00
Gym Membership,4500.00,0,4500.00,,,,,,Discretionary,Meals,4800.00
Dining Out & Weekend,12000.00,0,12000.00,,,,,,Discretionary,Total Deductions,39146.40
Online Subscriptions,3200.00,0,3200.00,,,,,,Discretionary,Sal,207422.60
,,,,,,,,,,Saving,25000.00
,,,,,,,,,,Hand Save,15000.00
Exspenses Total,198000.00,29000.00,169000.00,,,,,,Expenses Total,169000.00
        """.trimIndent()

        val sheet = parser.parse(realWorldCsv.byteInputStream(), isExcel = false, monthKeyFallback = "2026-10")

        assertNotNull(sheet)
        assertEquals(3500.00, sheet.openingBankBalance, 0.01)
        assertEquals(10, sheet.expenseItems.size)

        // Rent is mandatory
        val rent = sheet.expenseItems.first { it.itemName == "House Rent" }
        assertTrue(rent.isMandatory)
        assertEquals(45000.00, rent.totalRealPay, 0.01)

        // Credit Card payment partial pay
        val creditCard = sheet.expenseItems.first { it.itemName == "Credit Card Payment" }
        assertEquals(35000.00, creditCard.amount, 0.01)
        assertEquals(15000.00, creditCard.notPay, 0.01)
        assertEquals(20000.00, creditCard.totalRealPay, 0.01)

        // Right side values
        assertEquals(192455.00, sheet.salaryBreakdown["Basic Salary"] ?: 0.0, 0.01)
        assertEquals(246569.00, sheet.salaryBreakdown["Gross Salary"] ?: 0.0, 0.01)
        assertEquals(207422.60, sheet.salaryBreakdown["Sal"] ?: 0.0, 0.01)
        assertEquals(25000.00, sheet.salaryBreakdown["Saving"] ?: 0.0, 0.01)
        assertEquals(15000.00, sheet.salaryBreakdown["Hand Save"] ?: 0.0, 0.01)
    }
}
