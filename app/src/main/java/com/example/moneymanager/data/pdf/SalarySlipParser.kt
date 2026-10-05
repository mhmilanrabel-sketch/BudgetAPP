package com.example.moneymanager.data.pdf

import android.content.Context
import android.net.Uri
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object SalarySlipParser {

    /**
     * Reads a salary slip PDF from the given Uri and returns a filled
     * SalaryRecord. Requires the caller to have already called
     * PDFBoxResourceLoader.init(context) once per process.
     */
    fun parse(context: Context, uri: Uri, monthKey: String): SalaryRecord {
        PDFBoxResourceLoader.init(context)

        val text: String = context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                PDFTextStripper().apply {
                    sortByPosition = true
                }.getText(doc)
            }
        } ?: throw IllegalStateException("Could not open PDF")

        return parseText(text, monthKey)
    }

    /**
     * Parses the full text extracted from a payslip PDF.
     * Exposed separately so unit tests can feed a String directly.
     */
    fun parseText(text: String, monthKey: String): SalaryRecord {
        val record = SalaryRecord(
            monthKey             = monthKey,
            basicSalary          = find(text, "Basic Salary"),
            vehicleAllowance     = find(text, "Vehicle Allowance"),
            exceptionalIncentive = find(text, "Exceptional Incentive"),
            shiftCompensation    = find(text, "Shift Compensation"),
            grossSalary          = find(text, "Gross Salary"),
            totalForEpf          = find(text, "Total For EPF"),
            totalForEtf          = find(text, "Total For ETF"),
            totalForTax          = find(text, "Total For TAX"),
            apit                 = find(text, "APIT"),
            lumpsumTax           = find(text, "LUMPSUMP TAX"),
            stampDuty            = find(text, "STAMP DUTY"),
            epfEmployee          = find(text, "EPF Employee Cont"),
            funeralFund          = find(text, "Funeral Fund"),
            excessMobile         = find(text, "Excess Mobile Phone Usage"),
            mealsDeduction       = find(text, "Meals -Deduction"),
            totalDeductions      = find(text, "Total Deductions"),
            netSalary            = find(text, "Net Salary"),
            cashSalary           = find(text, "Cash Salary"),
            salaryToBank         = find(text, "Salary To Bank"),
            epfEmployer          = find(text, "EPF Employer Cont"),
            etfEmployer          = find(text, "ETF Employer Cont"),
            stampDutyEmployer    = find(text, "STAMP DUTY Employer"),
            rawText              = text
        )

        require(record.netSalary > 0 || record.grossSalary > 0) {
            "This does not look like a salary slip — no Net Salary or Gross Salary found."
        }

        return record
    }

    // "192,455.33" → 192455.33 ; returns 0.0 if not found
    private val NUM = """([\d,]+(?:\.\d{1,2})?)"""

    private fun find(text: String, label: String): Double {
        // Match: "Label  123,456.78"  or  "Label: 123,456.78"  or  "Label - 123.45"
        val pattern = Regex(
            Regex.escape(label) + """[\s:\-\.]*$NUM""",
            RegexOption.IGNORE_CASE
        )
        val m = pattern.find(text) ?: return 0.0
        val raw = m.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull() ?: 0.0
    }
}