package com.example.moneymanager.data.parser

import android.content.Context
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

class PaySlipParser(private val context: Context) {

    fun parse(inputStream: InputStream): ParsedSalarySlip {
        val document = PDDocument.load(inputStream)
        val fullText: String
        try {
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            fullText = stripper.getText(document)
        } finally {
            document.close()
        }

        if (fullText.isBlank() || fullText.length < 50) {
            throw IllegalStateException("PDF contains no extractable text. May be scanned or image-based.")
        }

        return extractFinancialFields(fullText)
    }

    fun parseFromText(text: String, isOcr: Boolean = false): ParsedSalarySlip {
        return extractFinancialFields(text, isOcr)
    }

    private fun extractFinancialFields(text: String, isOcr: Boolean = false): ParsedSalarySlip {
        val lines = text.lines()

        val monthKey = extractMonthKey(text)

        val basicSalary = extractAmount(text, "(?i)Basic\\s+Salary", "Basic\\s*:\\s*")
        val vehicleAllowance = extractAmount(text, "(?i)Vehicle\\s+Allowance")
        val exceptionalIncentive = extractAmount(text, "(?i)Exceptional\\s+Incentive")
        val shiftCompensation = extractAmount(text, "(?i)Shift\\s+Compensation")
        val grossSalary = extractAmount(text, "(?i)Gross\\s+Salary", "(?i)Total\\s+Earnings")

        val totalForEpf = extractAmount(text, "(?i)Total\\s+for\\s+EPF", "(?i)Total\\s+for\\s+E\\.P\\.F")
        val totalForEtf = extractAmount(text, "(?i)Total\\s+for\\s+ETF", "(?i)Total\\s+for\\s+E\\.T\\.F")
        val totalForTax = extractAmount(text, "(?i)Total\\s+for\\s+Tax")

        val apit = extractAmount(text, "(?i)\\bAPIT\\b", "(?i)Advance\\s+Personal\\s+Income\\s+Tax")
        val lumpsumTax = extractAmount(text, "(?i)Lump\\s*sum\\s+tax", "(?i)Lumpsum\\s+Tax")
        val stampDuty = extractAmount(text, "(?i)Stamp\\s+Duty")
        val epfEmployee = extractAmount(text, "(?i)E\\.?P\\.?F\\.?\\s+Employee", "(?i)EPF\\s+8%", "(?i)Employee\\s+EPF")
        val funeralFund = extractAmount(text, "(?i)Funeral\\s+Fund")
        val excessMobile = extractAmount(text, "(?i)Excess\\s+Mobile", "(?i)Mobile\\s+Deduction")
        val mealsDeduction = extractAmount(text, "(?i)Meals")
        val totalDeductions = extractAmount(text, "(?i)Total\\s+Deductions")

        val netSalary = extractAmount(text, "(?i)Net\\s+Salary", "(?i)Take\\s+Home\\s+Pay", "(?i)Net\\s+Pay")
        val cashSalary = extractAmount(text, "(?i)Cash\\s+Salary")
        val salaryToBank = extractAmount(text, "(?i)Salary\\s+to\\s+Bank", "(?i)Bank\\s+Transfer")

        val epfEmployer = extractAmount(text, "(?i)E\\.?P\\.?F\\.?\\s+Employer", "(?i)EPF\\s+12%")
        val etfEmployer = extractAmount(text, "(?i)E\\.?T\\.?F\\.?\\s+Employer", "(?i)ETF\\s+3%")
        val stampDutyEmployer = extractAmount(text, "(?i)Stamp\\s+Duty\\s+Employer")

        // Strict Validation: gross - deductions must equal net (±1 LKR)
        val calculatedNet = grossSalary - totalDeductions
        if (grossSalary > 0.0 && totalDeductions > 0.0 && netSalary > 0.0) {
            val variance = abs(calculatedNet - netSalary)
            if (variance > 1.0) {
                throw IllegalArgumentException(
                    "Payslip integrity validation failed: Gross (${CurrencyFormatter.formatLkr(grossSalary)}) - " +
                            "Deductions (${CurrencyFormatter.formatLkr(totalDeductions)}) = ${CurrencyFormatter.formatLkr(calculatedNet)}, " +
                            "but Net Salary is ${CurrencyFormatter.formatLkr(netSalary)} (Variance: ${CurrencyFormatter.formatLkr(variance)})."
                )
            }
        }

        return ParsedSalarySlip(
            monthKey = monthKey,
            basicSalary = basicSalary,
            vehicleAllowance = vehicleAllowance,
            exceptionalIncentive = exceptionalIncentive,
            shiftCompensation = shiftCompensation,
            grossSalary = grossSalary,
            totalForEpf = if (totalForEpf > 0.0) totalForEpf else basicSalary,
            totalForEtf = if (totalForEtf > 0.0) totalForEtf else basicSalary,
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
            salaryToBank = if (salaryToBank > 0.0) salaryToBank else netSalary,
            epfEmployer = epfEmployer,
            etfEmployer = etfEmployer,
            stampDutyEmployer = stampDutyEmployer,
            rawText = text,
            isOcrFallback = isOcr
        )
    }

    private fun extractAmount(text: String, vararg patterns: String): Double {
        for (patternStr in patterns) {
            val regex = Pattern.compile(
                "$patternStr[:\\s]*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = regex.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)
                val parsed = CurrencyFormatter.parseAmount(match)
                if (parsed > 0.0) return parsed
            }
        }

        val lines = text.lines()
        for (patternStr in patterns) {
            val r = Regex(patternStr, RegexOption.IGNORE_CASE)
            for (i in lines.indices) {
                if (r.containsMatchIn(lines[i])) {
                    val candidate = lines[i].replace(r, "").trim()
                    val amt = CurrencyFormatter.parseAmount(candidate)
                    if (amt > 0.0) return amt

                    if (i + 1 < lines.size) {
                        val nextAmt = CurrencyFormatter.parseAmount(lines[i + 1].trim())
                        if (nextAmt > 0.0) return nextAmt
                    }
                }
            }
        }
        return 0.0
    }

    private fun extractMonthKey(text: String): String {
        val monthNames = listOf(
            "January" to "01", "February" to "02", "March" to "03", "April" to "04",
            "May" to "05", "June" to "06", "July" to "07", "August" to "08",
            "September" to "09", "October" to "10", "November" to "11", "December" to "12",
            "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
            "Jun" to "06", "Jul" to "07", "Aug" to "08", "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
        )

        val yearRegex = Regex("\\b(20[2-3][0-9])\\b")
        val yearMatch = yearRegex.find(text)
        val year = yearMatch?.value ?: java.time.LocalDate.now().year.toString()

        for ((mName, mNum) in monthNames) {
            if (text.contains(mName, ignoreCase = true)) {
                return "$year-$mNum"
            }
        }

        val dateMatch = Regex("\\b(20[2-3][0-9])[-/](0[1-9]|1[0-2])\\b").find(text)
        if (dateMatch != null) {
            return dateMatch.value.replace('/', '-')
        }

        val now = java.time.LocalDate.now()
        val currentMonth = if (now.monthValue < 10) "0${now.monthValue}" else "${now.monthValue}"
        return "${now.year}-$currentMonth"
    }
}
