package com.example

import com.example.moneymanager.data.parser.BudgetSheetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class BudgetSheetParserTest {

    private lateinit var parser: BudgetSheetParser

    private val realWorldCsv = """
        Current Bank Rs,"Rs. 240,760.00",,,,,,,,,
        Item,Amount,Not pay,Total real pay,,,,,,MONTHLY Fixed Expenses,Basic Salary,192,455.00
        Rent/mortgage,"Rs. 25,000.00",,,,,,,,,Exceptional Incentive,4,000.00
        Electric Chinna home,"Rs. 9,276.92",,,,,,,,,Vehicle Allowance,48,114.00
        Water Chinna,"Rs. 5,570.24",,,,,,,,,Shift compensation,24,000.00
        Cell phone / Wifi,"Rs. 1,801.00",,,,,,,,,Gross Salary,268,569.00
        SLT Peo Tv / phone,"Rs. 9,383.76",,,,,,,,,APIT,11,342.00
        Senaya Van,"Rs. 7,000.00",,,,,,,,,EPF,15,396.00
        Geetha Van,"Rs. 6,000.00",,,,,,,,,Excess Mobile,0.00
        Maiyon Van,"Rs. 12,000.00","Rs. 12,000.00",,,,,,,,Meals,1,071.00
        Maiyon Fees,"Rs. 20,500.00",,,,,,,,,Total Deductions,27,809.00
        Sanga,"Rs. 30,000.00",,,,,,,,,Sal,240,760.00
        Mint Pay,"Rs. 13,825.98",,,,,,,,,Exspenses Total,200,158.00
        Nipuna,"Rs. 30,000.00",,,,,,,,,Hand Save,40,602.00
        Sachini,"Rs. 110,000.00","Rs. 100,000.00","Rs. 10,000.00",,,,,,Saving,25,000.00
        Poli(08),"Rs. 7,500.00",,,,,,,,,Other,15,602.00
        Washing M,"Rs. 60,600.00","Rs. 51,900.00","Rs. 8,700.00"
        School Annual Fees,"Rs. 12,000.00","Rs. 6,000.00","Rs. 6,000.00"
        Amma,"Rs. 10,000.00","Rs. 10,000.00",
        Ramani Aunty Grocery,"Rs. 45,000.00","Rs. 27,705.00","Rs. 17,295.00"
        Smk,"Rs. 8,300.00","Rs. 8,300.00",
        Total,"Rs. 443,107.90","Rs. 215,905.00","Rs. 41,995.00"
    """.trimIndent()

    @Before
    fun setup() {
        parser = BudgetSheetParser()
    }

    @Test
    fun parseCsv_twoSectionRealWorldData_parsesAccurately() {
        val stream = ByteArrayInputStream(realWorldCsv.toByteArray())
        val sheet = parser.parseCsv(stream, "2026-10")

        // 1. Opening balance detection from "Current Bank Rs"
        assertEquals(240760.00, sheet.openingBankBalance, 0.01)

        // 2. Right section salary values
        assertEquals(192455.00, sheet.salaryBreakdown["Basic Salary"] ?: 0.0, 0.01)
        assertEquals(268569.00, sheet.salaryBreakdown["Gross Salary"] ?: 0.0, 0.01)
        assertEquals(240760.00, sheet.salaryBreakdown["Sal"] ?: 0.0, 0.01)
        assertEquals(25000.00, sheet.savingAllocation, 0.01)
        assertEquals(40602.00, sheet.handSave, 0.01)

        // 3. Left section expenses
        val rent = sheet.expenses.find { it.name.contains("Rent/mortgage") }
        assertNotNull(rent)
        assertEquals(25000.00, rent!!.realPay, 0.01)
        assertTrue(rent.isMandatory)

        val maiyonVan = sheet.expenses.find { it.name.contains("Maiyon Van") }
        assertNotNull(maiyonVan)
        assertEquals(12000.00, maiyonVan!!.notPay, 0.01)
        assertEquals(0.00, maiyonVan.realPay, 0.01)
        assertTrue(maiyonVan.isMandatory)

        val sachini = sheet.expenses.find { it.name.contains("Sachini") }
        assertNotNull(sachini)
        assertEquals(10000.00, sachini!!.realPay, 0.01)
        assertEquals(100000.00, sachini.notPay, 0.01)
        assertFalse(sachini.isMandatory) // Sachini is classified as optional
    }
}
