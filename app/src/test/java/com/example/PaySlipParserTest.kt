package com.example

import com.example.moneymanager.data.parser.PaySlipParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PaySlipParserTest {

    private val parser by lazy {
        PaySlipParser(RuntimeEnvironment.getApplication())
    }

    @Test
    fun parseFromText_validSriLankanPaySlip_extractsAllFieldsAccurately() {
        val payslipText = """
            SRI LANKA COMMERCIAL TECH (PVT) LTD
            Pay Slip for the month of October 2026
            
            Employee No: EMP-009842
            Basic Salary: 192,455.00
            Vehicle Allowance: 48,114.00
            Exceptional Incentive: 4,000.00
            Shift Compensation: 2,000.00
            Gross Salary: 246,569.00
            
            Deductions:
            APIT: 18,450.00
            EPF Employee 8%: 15,396.40
            Funeral Fund: 500.00
            Meals: 4,800.00
            Total Deductions: 39,146.40
            
            Net Salary: 207,422.60
            
            Employer Contributions:
            EPF Employer 12%: 23,094.60
            ETF Employer 3%: 5,773.65
        """.trimIndent()

        val parsed = parser.parseFromText(payslipText)

        assertNotNull(parsed)
        assertEquals("2026-10", parsed.monthKey)
        assertEquals(192455.00, parsed.basicSalary, 0.01)
        assertEquals(48114.00, parsed.vehicleAllowance, 0.01)
        assertEquals(4000.00, parsed.exceptionalIncentive, 0.01)
        assertEquals(2000.00, parsed.shiftCompensation, 0.01)
        assertEquals(246569.00, parsed.grossSalary, 0.01)
        assertEquals(18450.00, parsed.apit, 0.01)
        assertEquals(15396.40, parsed.epfEmployee, 0.01)
        assertEquals(500.00, parsed.funeralFund, 0.01)
        assertEquals(4800.00, parsed.mealsDeduction, 0.01)
        assertEquals(39146.40, parsed.totalDeductions, 0.01)
        assertEquals(207422.60, parsed.netSalary, 0.01)
        assertEquals(23094.60, parsed.epfEmployer, 0.01)
        assertEquals(5773.65, parsed.etfEmployer, 0.01)
    }

    @Test
    fun parseFromText_invalidMath_rejectsWithValidationError() {
        val tamperedText = """
            Basic Salary: 100,000.00
            Gross Salary: 100,000.00
            Total Deductions: 10,000.00
            Net Salary: 95,000.00
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parser.parseFromText(tamperedText)
        }
    }
}
