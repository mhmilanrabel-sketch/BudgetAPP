package com.example.moneymanager.data.parser

import android.content.Context
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.domain.model.BudgetExpenseItem
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.opencsv.CSVReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream

class BudgetSheetParser(private val context: Context) {

    private val mandatoryKeywords = mutableSetOf(
        "insurance", "rent", "loan", "card", "bill", "electricity", "water", "ceb",
        "dialog", "mobitel", "sl telecom", "slt", "wifi", "medical", "hospital",
        "school", "classes", "tuition", "medicine", "mother", "father", "fixed",
        "apit", "epf", "tax", "petrol", "fuel"
    )

    fun addMandatoryKeyword(keyword: String) {
        mandatoryKeywords.add(keyword.lowercase(Locale.ENGLISH).trim())
    }

    fun getMandatoryKeywords(): Set<String> = mandatoryKeywords.toSet()

    fun parse(inputStream: InputStream, isExcel: Boolean, monthKeyFallback: String = ""): ParsedBudgetSheet {
        val rows: List<List<String>> = if (isExcel) {
            parseXlsxStreaming(inputStream)
        } else {
            parseCsv(inputStream)
        }

        return processGridRows(rows, monthKeyFallback)
    }

    private fun parseCsv(inputStream: InputStream): List<List<String>> {
        val reader = CSVReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val rows = mutableListOf<List<String>>()
        var line: Array<String>?
        while (reader.readNext().also { line = it } != null) {
            rows.add(line!!.toList())
        }
        reader.close()
        return rows
    }

    private fun parseXlsxStreaming(inputStream: InputStream): List<List<String>> {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        while (entry != null) {
            if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                sharedStrings.addAll(parseSharedStringsXml(zip))
            } else if (entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true)) {
                sheetBytes = zip.readBytes()
            }
            entry = zip.nextEntry
        }

        if (sheetBytes != null) {
            return parseSheetXml(sheetBytes.inputStream(), sharedStrings)
        }
        return emptyList()
    }

    private fun parseSharedStringsXml(stream: InputStream): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("t", ignoreCase = true)) {
                        insideT = true
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("t", ignoreCase = true)) {
                        insideT = false
                        list.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var currentCellRef = ""
        var cellType = ""
        var currentVal = StringBuilder()
        var insideV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("row", ignoreCase = true)) {
                        currentRow = mutableListOf()
                    } else if (name.equals("c", ignoreCase = true)) {
                        currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        currentVal.setLength(0)
                    } else if (name.equals("v", ignoreCase = true)) {
                        insideV = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        currentVal.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("v", ignoreCase = true)) {
                        insideV = false
                    } else if (name.equals("c", ignoreCase = true)) {
                        val raw = currentVal.toString().trim()
                        val value = if (cellType == "s") {
                            val idx = raw.toIntOrNull() ?: -1
                            if (idx in sharedStrings.indices) sharedStrings[idx] else raw
                        } else {
                            raw
                        }
                        val colIdx = colRefToIndex(currentCellRef)
                        while (currentRow.size < colIdx) {
                            currentRow.add("")
                        }
                        currentRow.add(value)
                    } else if (name.equals("row", ignoreCase = true)) {
                        rows.add(currentRow)
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun colRefToIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else {
                break
            }
        }
        return maxOf(0, col - 1)
    }

    private fun processGridRows(rows: List<List<String>>, fallbackMonthKey: String): ParsedBudgetSheet {
        var openingBankBalance = 0.0
        val expenseItems = mutableListOf<BudgetExpenseItem>()
        val salaryBreakdown = mutableMapOf<String, Double>()
        var detectedMonthKey = fallbackMonthKey

        for (row in rows) {
            if (row.isEmpty()) continue

            // 1. Detect "Current Bank Rs" (Row 1 opening balance)
            val fullRowText = row.joinToString(" ")
            if (fullRowText.contains("Current Bank", ignoreCase = true)) {
                for (cell in row) {
                    val amt = CurrencyFormatter.parseAmount(cell)
                    if (amt > 0.0) {
                        openingBankBalance = amt
                        break
                    }
                }
            }

            // Detect Month if present
            if (detectedMonthKey.isBlank()) {
                val match = Regex("\\b(20[2-3][0-9])[-/](0[1-9]|1[0-2])\\b").find(fullRowText)
                if (match != null) {
                    detectedMonthKey = match.value.replace('/', '-')
                }
            }

            // 2. Parse Left Section (Cols A to D: Item, Amount, Not pay, Total real pay)
            parseLeftSectionRow(row, expenseItems)

            // 3. Parse Right Section (Cols K to N: Monthly Fixed Expenses & Salary)
            parseRightSectionRow(row, salaryBreakdown)
        }

        if (detectedMonthKey.isBlank()) {
            val now = java.time.LocalDate.now()
            val m = if (now.monthValue < 10) "0${now.monthValue}" else "${now.monthValue}"
            detectedMonthKey = "${now.year}-$m"
        }

        val totalAmount = expenseItems.sumOf { it.amount }
        val totalNotPaid = expenseItems.sumOf { it.notPay }
        val totalRealPay = expenseItems.sumOf { it.totalRealPay }

        val basicSalary = salaryBreakdown["Basic Salary"] ?: salaryBreakdown["basic"] ?: 0.0
        val savingsTarget = salaryBreakdown["Saving"] ?: salaryBreakdown["saving"] ?: 0.0
        val salBalance = salaryBreakdown["Sal"] ?: salaryBreakdown["sal"] ?: 0.0
        val handSave = salaryBreakdown["Hand Save"] ?: salaryBreakdown["hand save"] ?: 0.0

        return ParsedBudgetSheet(
            monthKey = detectedMonthKey,
            openingBankBalance = openingBankBalance,
            expenseItems = expenseItems,
            totalExpenseAmount = totalAmount,
            totalNotPaid = totalNotPaid,
            totalRealPay = totalRealPay,
            salaryBreakdown = salaryBreakdown,
            savingsTarget = savingsTarget,
            basicSalary = basicSalary,
            salBalance = salBalance,
            handSave = handSave
        )
    }

    private fun parseLeftSectionRow(row: List<String>, expenseItems: MutableList<BudgetExpenseItem>) {
        if (row.size < 2) return
        val itemCol = row[0].trim()
        val amountCol = row.getOrNull(1)?.trim() ?: ""
        val notPayCol = row.getOrNull(2)?.trim() ?: ""
        val realPayCol = row.getOrNull(3)?.trim() ?: ""

        if (isIgnoredHeaderOrTotal(itemCol)) return

        val amount = CurrencyFormatter.parseAmount(amountCol)
        val notPay = CurrencyFormatter.parseAmount(notPayCol)
        var realPay = CurrencyFormatter.parseAmount(realPayCol)

        // Compute realPay if formula was not evaluated: realPay = amount - notPay
        if (realPay == 0.0 && amount > 0.0 && notPay >= 0.0) {
            realPay = amount - notPay
        }

        if (itemCol.isNotBlank() && (amount > 0.0 || notPay > 0.0 || realPay > 0.0)) {
            val isMandatory = isMandatoryExpense(itemCol)
            val category = if (isMandatory) "Mandatory" else "Optional"
            expenseItems.add(
                BudgetExpenseItem(
                    itemName = itemCol,
                    amount = amount,
                    notPay = notPay,
                    totalRealPay = realPay,
                    isMandatory = isMandatory,
                    category = category
                )
            )
        }
    }

    private fun parseRightSectionRow(row: List<String>, salaryBreakdown: MutableMap<String, Double>) {
        for (i in 4 until row.size) {
            val cellText = row[i].trim()
            if (isSalaryLabel(cellText)) {
                for (j in (i + 1) until minOf(row.size, i + 5)) {
                    val candidate = row[j].trim()
                    if (candidate.isNotBlank()) {
                        val nextCell = row.getOrNull(j + 1)?.trim() ?: ""
                        val combined = if (nextCell.matches(Regex("^\\d{3}(\\.\\d{1,2})?$"))) {
                            candidate + nextCell
                        } else {
                            candidate
                        }
                        val amt = CurrencyFormatter.parseAmount(combined)
                        if (amt > 0.0 || candidate == "0" || candidate == "0.00") {
                            normalizeAndAddSalaryField(cellText, amt, salaryBreakdown)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isSalaryLabel(text: String): Boolean {
        val lower = text.lowercase(Locale.ENGLISH)
        return lower.contains("basic salary") ||
                lower.contains("gross salary") ||
                lower.contains("vehicle allowance") ||
                lower.contains("shift compensation") ||
                lower.contains("exceptional incentive") ||
                lower.contains("apit") ||
                lower.contains("epf") ||
                lower.contains("total deductions") ||
                lower == "sal" ||
                lower == "saving" ||
                lower == "hand save" ||
                lower == "exspenses total" ||
                lower == "other"
    }

    private fun normalizeAndAddSalaryField(
        rawLabel: String,
        amount: Double,
        map: MutableMap<String, Double>
    ) {
        val lower = rawLabel.lowercase(Locale.ENGLISH).trim()
        val standardLabel = when {
            lower.contains("basic salary") -> "Basic Salary"
            lower.contains("gross salary") -> "Gross Salary"
            lower.contains("vehicle allowance") -> "Vehicle Allowance"
            lower.contains("shift compensation") -> "Shift Compensation"
            lower.contains("exceptional incentive") -> "Exceptional Incentive"
            lower.contains("apit") -> "APIT"
            lower.contains("epf") -> "EPF"
            lower.contains("total deductions") -> "Total Deductions"
            lower == "sal" -> "Sal"
            lower == "saving" -> "Saving"
            lower == "hand save" -> "Hand Save"
            lower == "exspenses total" -> "Expenses Total"
            lower == "other" -> "Other"
            else -> rawLabel.trim()
        }
        map[standardLabel] = amount
    }

    private fun isMandatoryExpense(itemName: String): Boolean {
        val lower = itemName.lowercase(Locale.ENGLISH)
        return mandatoryKeywords.any { lower.contains(it) }
    }

    private fun isIgnoredHeaderOrTotal(itemCol: String): Boolean {
        val lower = itemCol.lowercase(Locale.ENGLISH)
        return lower.contains("item") ||
                lower.contains("current bank") ||
                lower.contains("total") ||
                lower.contains("exspenses total") ||
                lower.contains("balance") ||
                lower.contains("sub total")
    }
}
