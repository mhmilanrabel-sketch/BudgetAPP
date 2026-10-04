package com.example

import com.example.moneymanager.data.parser.PaySlipParser
import com.example.moneymanager.data.parser.PaySlipValidationException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PaySlipParserTest {

    private lateinit var parser: PaySlipParser

    private val realWorldSlipText = """
        Basic Salary                     192,455.33
        Vehicle Allowance                 48,113.83
        Exceptional Incentive              4,000.00
        Shift Compensation Allow.         24,000.00
        Gross Salary                     268,569.16
        Total For EPF                    192,455.33
        Total For ETF                    192,455.33
        Total For TAX                    268,569.16
        APIT                              11,342.45
        LUMPSUMP TAX                           0.00
        STAMP DUTY                             0.00
        EPF Employee Cont.                15,396.43
        Funeral Fund                         750.00
        Excess Mobile Phone Usage         11,678.22
        Meals -Deduction                     915.00
        Total Deductions                  40,082.10
        Net Salary                       228,487.06
        Cash Salary                            0.00
        Salary To Bank                   228,487.06
        EPF Employer Cont.                23,094.64
        ETF Employer Cont.                 5,773.66
        APIT Employer.                         0.00
        LUMPSUMP TAX Employer.                 0.00
        STAMP DUTY Employer.                  25.00
        Meals -Deduction- Rate 6.00 152.50
    """.trimIndent()

    @Before
    fun setup() {
        parser = PaySlipParser()
    }

    @Test
    fun parseText_realWorldSample_extractsAccurateFigures() {
        val slip = parser.parseText(realWorldSlipText, "2026-10")

        assertEquals("2026-10", slip.monthKey)
        assertEquals(192455.33, slip.basicSalary, 0.01)
        assertEquals(48113.83, slip.vehicleAllowance, 0.01)
        assertEquals(4000.00, slip.exceptionalIncentive, 0.01)
        assertEquals(24000.00, slip.shiftCompensation, 0.01)
        assertEquals(268569.16, slip.grossSalary, 0.01)
        assertEquals(11342.45, slip.apit, 0.01)
        assertEquals(15396.43, slip.epfEmployee, 0.01)
        assertEquals(11678.22, slip.excessMobile, 0.01)
        assertEquals(915.00, slip.mealsDeduction, 0.01)
        assertEquals(40082.10, slip.totalDeductions, 0.01)
        assertEquals(228487.06, slip.netSalary, 0.01)
        assertEquals(23094.64, slip.epfEmployer, 0.01)
        assertEquals(5773.66, slip.etfEmployer, 0.01)

        // Mathematical verification: Gross - Deductions = Net (within ±1 LKR)
        val calculatedNet = slip.grossSalary - slip.totalDeductions
        assertEquals(calculatedNet, slip.netSalary, 0.01)
    }

    @Test(expected = PaySlipValidationException::class)
    fun parseText_mathematicalMismatch_rejectsFile() {
        // Discrepancy intentional: Net salary altered by 100 LKR
        val corruptSlip = """
            Gross Salary                     268,569.16
            Total Deductions                  40,082.10
            Net Salary                       228,587.06
        """.trimIndent()

        parser.parseText(corruptSlip, "2026-10")
    }
}
