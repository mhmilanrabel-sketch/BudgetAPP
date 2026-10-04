package com.example.moneymanager.data.parser

import android.content.Context
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.domain.model.BudgetExpenseItem
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.opencsv.CSVReader
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class BudgetSheetParser(private val context: Context? = null) {

    companion object {
        private const val TAG = "BudgetSheetParser"

        val DEFAULT_MANDATORY_KEYWORDS = listOf(
            "rent", "mortgage", "electric", "water", "gas", "cell phone", "peo tv",
            "van", "fees", "school", "poli", "washing", "grocery", "sanga",
            "nipuna", "mint pay", "amma"
        )
    }

    fun parseCsv(
        inputStream: InputStream,
        targetMonthKey: String? = null,
        customKeywords: List<String>? = null
    ): ParsedBudgetSheet {
        val reader = CSVReader(InputStreamReader(inputStream))
        val rawRows = reader.readAll().map { it.toList() }
        reader.close()
        return parseGrid(rawRows, targetMonthKey, customKeywords ?: getSavedMandatoryKeywords())
    }

    fun parseXlsx(
        inputStream: InputStream,
        targetMonthKey: String? = null,
        customKeywords: List<String>? = null
    ): ParsedBudgetSheet {
        val grid = readXlsxToGrid(inputStream)
        return parseGrid(grid, targetMonthKey, customKeywords ?: getSavedMandatoryKeywords())
    }

    fun parseInputStreamAuto(
        inputStream: InputStream,
        isXlsx: Boolean,
        targetMonthKey: String? = null,
        customKeywords: List<String>? = null
    ): ParsedBudgetSheet {
        return if (isXlsx) {
            parseXlsx(inputStream, targetMonthKey, customKeywords)
        } else {
            parseCsv(inputStream, targetMonthKey, customKeywords)
        }
    }

    /**
     * Common core parsing logic for both CSV and XLSX grid.
     */
    fun parseGrid(
        grid: List<List<String>>,
        targetMonthKey: String?,
        mandatoryKeywords: List<String>
    ): ParsedBudgetSheet {
        if (grid.isEmpty()) {
            throw IllegalArgumentException("The sheet is empty.")
        }

        var openingBalance = 0.0
        val expenses = mutableListOf<BudgetExpenseItem>()
        val salaryBreakdown = mutableMapOf<String, Double>()

        // 1. Scan for Opening Balance (e.g. row with "Current Bank Rs" in Col A or B)
        for (row in grid) {
            val colA = row.getOrNull(0)?.trim() ?: ""
            val colB = row.getOrNull(1)?.trim() ?: ""
            if (colA.contains("Current Bank", ignoreCase = true)) {
                openingBalance = CurrencyFormatter.parseAmount(colB)
                break
            }
        }

        // 2. Scan Left Section (Expenses: Col A=Item, Col B=Amount, Col C=Not Pay, Col D=Total Real Pay)
        // and Right Section (Salary: Col K (index 10) = Label, Col N (index 13 or adjacent) = Value)
        for (row in grid) {
            val colA = row.getOrNull(0)?.trim() ?: ""
            val colB = row.getOrNull(1)?.trim() ?: ""
            val colC = row.getOrNull(2)?.trim() ?: ""
            val colD = row.getOrNull(3)?.trim() ?: ""

            // Left side expense row check
            if (colA.isNotBlank() &&
                !colA.equals("Current Bank Rs", ignoreCase = true) &&
                !colA.equals("Item", ignoreCase = true) &&
                !colA.equals("Description", ignoreCase = true) &&
                !colA.equals("Expenses", ignoreCase = true) &&
                !colA.equals("Total", ignoreCase = true)
            ) {
                val budgetAmt = CurrencyFormatter.parseAmount(colB)
                val notPayAmt = CurrencyFormatter.parseAmount(colC)
                val explicitRealPay = if (colD.isNotBlank()) CurrencyFormatter.parseAmount(colD) else null

                // If real pay is explicitly given in col D, use it; otherwise budgetAmt - notPayAmt
                val realPayAmt = explicitRealPay ?: (budgetAmt - notPayAmt).coerceAtLeast(0.0)

                if (budgetAmt > 0.0 || notPayAmt > 0.0 || realPayAmt > 0.0) {
                    val isMandatory = isItemMandatory(colA, mandatoryKeywords)
                    expenses.add(
                        BudgetExpenseItem(
                            name = colA,
                            amount = budgetAmt,
                            notPay = notPayAmt,
                            realPay = realPayAmt,
                            isMandatory = isMandatory
                        )
                    )
                }
            }

            // Right side salary scan: Col K is index 10, Col N is index 13
            // Also search cells in row if offset varies slightly
            parseRightSectionRow(row, salaryBreakdown)
        }

        val totalExpensesColB = expenses.sumOf { it.amount }
        val totalNotPaid = expenses.sumOf { it.notPay }
        val totalRealPay = expenses.sumOf { it.realPay }

        val savingAllocation = salaryBreakdown["Saving"] ?: salaryBreakdown["Savings"] ?: 0.0
        val handSave = salaryBreakdown["Hand Save"] ?: salaryBreakdown["HandSave"] ?: 0.0

        val monthKey = targetMonthKey ?: CurrencyFormatter.getCurrentMonthKey()

        return ParsedBudgetSheet(
            monthKey = monthKey,
            openingBankBalance = openingBalance,
            expenses = expenses,
            salaryBreakdown = salaryBreakdown,
            totalExpensesColB = totalExpensesColB,
            totalNotPaid = totalNotPaid,
            totalRealPay = totalRealPay,
            savingAllocation = savingAllocation,
            handSave = handSave,
            rawRowCount = grid.size
        )
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

    private fun listOfOrNull(row: List<String>, vararg indices: Int): Double? {
        for (idx in indices) {
            val cell = row.getOrNull(idx)?.trim()
            if (!cell.isNullOrBlank()) {
                val amt = CurrencyFormatter.parseAmount(cell)
                if (amt > 0.0 || cell == "0" || cell == "0.00") return amt
            }
        }
        return null
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
            lower.contains("exceptional incentive") -> "Exceptional Incentive"
            lower.contains("vehicle allowance") -> "Vehicle Allowance"
            lower.contains("shift compensation") -> "Shift compensation"
            lower.contains("gross salary") -> "Gross Salary"
            lower == "apit" || lower.startsWith("apit") -> "APIT"
            lower == "epf" || lower.startsWith("epf") -> "EPF"
            lower.contains("excess mobile") -> "Excess Mobile"
            lower.contains("meals") -> "Meals"
            lower.contains("total deductions") -> "Total Deductions"
            lower == "sal" -> "Sal"
            lower.contains("exspenses total") || lower.contains("expenses total") -> "Exspenses Total"
            lower.contains("hand save") -> "Hand Save"
            lower.contains("saving") -> "Saving"
            lower.contains("other") -> "Other"
            else -> rawLabel
        }
        map[standardLabel] = amount
    }

    fun isItemMandatory(name: String, keywords: List<String>): Boolean {
        val lower = name.lowercase(Locale.ENGLISH)
        return keywords.any { lower.contains(it.lowercase(Locale.ENGLISH).trim()) }
    }

    fun getSavedMandatoryKeywords(): List<String> {
        val prefs = context?.getSharedPreferences("moneymanager_settings", Context.MODE_PRIVATE)
        val saved = prefs?.getString("mandatory_keywords", null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            DEFAULT_MANDATORY_KEYWORDS
        }
    }

    fun saveMandatoryKeywords(keywords: List<String>) {
        val prefs = context?.getSharedPreferences("moneymanager_settings", Context.MODE_PRIVATE)
        prefs?.edit()?.putString("mandatory_keywords", keywords.joinToString(","))?.apply()
    }

    /**
     * Pure on-device XLSX parsing without heavy external AWT dependencies.
     * Extracts sharedStrings.xml and sheet1.xml from the ZIP container.
     */
    private fun readXlsxToGrid(inputStream: InputStream): List<List<String>> {
        val bytes = inputStream.readBytes()
        var sharedStrings = listOf<String>()
        var sheetBytes: ByteArray? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings = parseSharedStrings(zis)
                    }
                    entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                            (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && sheetBytes == null) -> {
                        sheetBytes = zis.readBytes()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetBytes == null) {
            throw IllegalArgumentException("Invalid Excel file: No worksheet found inside archive.")
        }

        return parseSheetXml(ByteArrayInputStream(sheetBytes!!), sharedStrings)
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")
        var event = parser.eventType
        var currentText = StringBuilder()
        var insideText = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        insideText = true
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideText) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        insideText = false
                    } else if (parser.name == "si") {
                        list.add(currentText.toString())
                        currentText.setLength(0)
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val grid = mutableListOf<MutableList<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")
        var event = parser.eventType

        var currentRow = mutableMapOf<Int, String>()
        var currentCellRef = ""
        var currentCellType = ""
        var currentValue = StringBuilder()
        var insideValue = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentValue.setLength(0)
                        }
                        "v", "t" -> {
                            insideValue = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideValue) {
                        currentValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v", "t" -> {
                            insideValue = false
                        }
                        "c" -> {
                            val colIdx = columnRefToIndex(currentCellRef)
                            val rawVal = currentValue.toString().trim()
                            val cellContent = if (currentCellType == "s") {
                                val stringIndex = rawVal.toIntOrNull()
                                if (stringIndex != null && stringIndex in sharedStrings.indices) {
                                    sharedStrings[stringIndex]
                                } else {
                                    rawVal
                                }
                            } else {
                                rawVal
                            }
                            if (colIdx >= 0) {
                                currentRow[colIdx] = cellContent
                            }
                        }
                        "row" -> {
                            val maxCol = (currentRow.keys.maxOrNull() ?: -1) + 1
                            val rowList = MutableList(maxOf(maxCol, 15)) { "" }
                            for ((idx, v) in currentRow) {
                                if (idx in rowList.indices) {
                                    rowList[idx] = v
                                }
                            }
                            grid.add(rowList)
                        }
                    }
                }
            }
            event = parser.next()
        }
        return grid
    }

    private fun columnRefToIndex(cellRef: String): Int {
        val colPart = cellRef.takeWhile { it.isLetter() }.uppercase(Locale.ENGLISH)
        if (colPart.isEmpty()) return -1
        var index = 0
        for (ch in colPart) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }
}
