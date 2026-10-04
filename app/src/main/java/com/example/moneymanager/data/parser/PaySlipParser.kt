package com.example.moneymanager.data.parser

import android.content.Context
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

class PaySlipParser(private val context: Context? = null) {

    companion object {
        private const val TAG = "PaySlipParser"
        private const val TOLERANCE = 1.0 // ±1 LKR tolerance
    }

    /**
     * Parses an InputStream representing a Salary Slip PDF.
     * Uses PDFTextStripper with sortByPosition = true for two-column layout.
     * Validates that (gross - deductions) == net within 1 LKR.
     * Throws IllegalArgumentException on invalid format or mathematical mismatch.
     */
    fun parse(inputStream: InputStream, targetMonthKey: String? = null): ParsedSalarySlip {
        val document: PDDocument = try {
            PDDocument.load(inputStream)
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not read PDF document. Ensure the file is a valid PDF.", e)
        }

        try {
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }
            val text = stripper.getText(document)

            if (text.isBlank()) {
                throw EmptyPdfTextException("PDF contains no extractable text. It may be a scanned image or photo.")
            }

            return parseText(text, targetMonthKey)
        } finally {
            try {
                document.close()
            } catch (ignored: Exception) {}
        }
    }

    fun parseText(text: String, targetMonthKey: String? = null): ParsedSalarySlip {
        val lines = text.lines()
        val allExtracted = mutableMapOf<String, Double>()

        // Helper regex matching: <Label>[\s:\-]*([\d,]+(?:\.\d{1,2})?)
        fun extractAmount(labelPattern: String): Double? {
            val regex = Pattern.compile(
                "$labelPattern[\\s:\\-]*([\\d,]+(?:\\.\\d{1,2})?)",
                Pattern.CASE_INSENSITIVE
            )
            for (line in lines) {
                val matcher = regex.matcher(line)
                if (matcher.find()) {
                    val amountStr = matcher.group(1)?.replace(",", "")?.trim()
                    val value = amountStr?.toDoubleOrNull()
                    if (value != null) {
                        return value
                    }
                }
            }
            return null
        }

        // Also extract generic line items for breakdown screen
        val genericLineRegex = Pattern.compile(
            "^\\s*([A-Za-z0-9\\.\\-\\(\\)\\s/]+?)\\s{2,}([\\d,]+(?:\\.\\d{1,2})?)\\s*$"
        )
        for (line in lines) {
            val m = genericLineRegex.matcher(line)
            if (m.find()) {
                val label = m.group(1)?.trim() ?: ""
                val value = m.group(2)?.replace(",", "")?.toDoubleOrNull()
                if (label.isNotBlank() && value != null) {
                    allExtracted[label] = value
                }
            }
        }

        val basicSalary = extractAmount("Basic\\s*Salary") ?: 0.0
        val vehicleAllowance = extractAmount("Vehicle\\s*Allowance") ?: 0.0
        val exceptionalIncentive = extractAmount("Exceptional\\s*Incentive") ?: 0.0
        val shiftCompensation = extractAmount("Shift\\s*Compensation(?:\\s*Allow\\.?)?") ?: 0.0
        val grossSalary = extractAmount("Gross\\s*Salary") ?: 0.0

        val totalForEpf = extractAmount("Total\\s*For\\s*EPF") ?: basicSalary
        val totalForEtf = extractAmount("Total\\s*For\\s*ETF") ?: basicSalary
        val totalForTax = extractAmount("Total\\s*For\\s*TAX") ?: grossSalary

        val apit = extractAmount("APIT(?!\\s*Employer)") ?: 0.0
        val lumpsumTax = extractAmount("LUMPSUMP?\\s*TAX(?!\\s*Employer)") ?: 0.0
        val stampDuty = extractAmount("STAMP\\s*DUTY(?!\\s*Employer)") ?: 0.0
        val epfEmployee = extractAmount("EPF\\s*Employee\\s*Cont\\.?(?:ribution)?") ?: 0.0
        val funeralFund = extractAmount("Funeral\\s*Fund") ?: 0.0
        val excessMobile = extractAmount("Excess\\s*Mobile(?:\\s*Phone\\s*Usage)?") ?: 0.0
        val mealsDeduction = extractAmount("Meals\\s*-\\s*Deduction(?!-\\s*Rate)") ?: 0.0
        val totalDeductions = extractAmount("Total\\s*Deductions?") ?: 0.0

        val netSalary = extractAmount("Net\\s*Salary") ?: 0.0
        val cashSalary = extractAmount("Cash\\s*Salary") ?: 0.0
        val salaryToBank = extractAmount("Salary\\s*To\\s*Bank") ?: netSalary

        val epfEmployer = extractAmount("EPF\\s*Employer\\s*Cont\\.?(?:ribution)?") ?: 0.0
        val etfEmployer = extractAmount("ETF\\s*Employer\\s*Cont\\.?(?:ribution)?") ?: 0.0
        val stampDutyEmployer = extractAmount("STAMP\\s*DUTY\\s*Employer\\.?") ?: 0.0

        // Month detection if not specified
        val monthKey = targetMonthKey ?: detectMonthKey(text) ?: CurrencyFormatter.getCurrentMonthKey()

        // Strict Validation: grossSalary - totalDeductions must equal netSalary (±1 LKR)
        if (grossSalary > 0.0 || netSalary > 0.0 || totalDeductions > 0.0) {
            val calculatedNet = grossSalary - totalDeductions
            val diff = abs(calculatedNet - netSalary)
            if (diff > TOLERANCE) {
                throw PaySlipValidationException(
                    "Mathematical verification failed: Gross salary minus deductions does not equal Net salary. " +
                            "Discrepancy exceeds ±1.0 LKR limit. File rejected for financial accuracy."
                )
            }
        } else {
            throw IllegalArgumentException("Could not extract gross or net salary values from salary slip.")
        }

        return ParsedSalarySlip(
            monthKey = monthKey,
            basicSalary = basicSalary,
            vehicleAllowance = vehicleAllowance,
            exceptionalIncentive = exceptionalIncentive,
            shiftCompensation = shiftCompensation,
            grossSalary = grossSalary,
            totalForEpf = totalForEpf,
            totalForEtf = totalForEtf,
            totalForTax = totalForTax,
            apit = apit,
            lumpsumTax = lumpsumTax,
            stampDuty = stampDuty,
            epfEmployee = epfEmployee,
            funeralFund = funeralFund,
            excessMobile = excessMobile,
            mealsDeduction = mealsDeduction,
            totalDeductions = totalDeductions,
            netSalary = netSalary,
            cashSalary = cashSalary,
            salaryToBank = salaryToBank,
            epfEmployer = epfEmployer,
            etfEmployer = etfEmployer,
            stampDutyEmployer = stampDutyEmployer,
            allLineItems = allExtracted,
            rawTextExtracted = true
        )
    }

    private fun detectMonthKey(text: String): String? {
        val months = listOf(
            "january" to "01", "february" to "02", "march" to "03", "april" to "04",
            "may" to "05", "june" to "06", "july" to "07", "august" to "08",
            "september" to "09", "october" to "10", "november" to "11", "december" to "12",
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "jun" to "06", "jul" to "07", "aug" to "08", "sep" to "09",
            "oct" to "10", "nov" to "11", "dec" to "12"
        )
        val yearPattern = Pattern.compile("(20[2-3][0-9])")
        val yearMatcher = yearPattern.matcher(text)
        val year = if (yearMatcher.find()) yearMatcher.group(1) else null

        val lower = text.lowercase(Locale.ENGLISH)
        for ((mName, mNum) in months) {
            if (lower.contains(mName) && year != null) {
                return "$year-$mNum"
            }
        }
        return null
    }
}

class EmptyPdfTextException(message: String) : Exception(message)
class PaySlipValidationException(message: String) : IllegalArgumentException(message)
