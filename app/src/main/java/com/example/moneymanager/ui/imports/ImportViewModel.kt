package com.example.moneymanager.ui.imports

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.pdf.SalarySlipParser
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.StringReader
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Success(
        val source: String,
        val monthKey: String,
        val detailLines: List<String>
    ) : ImportState
    data class Error(val message: String) : ImportState
}

class ImportViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val currentMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    fun reset() { _state.value = ImportState.Idle }

    fun importCsv(context: Context, uri: Uri) {
        _state.value = ImportState.Importing
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { parseCsvAndPersist(context, uri) }
                _state.value = result
            } catch (t: Throwable) {
                _state.value = ImportState.Error(t.message ?: "CSV import failed")
            }
        }
    }

    fun importPdf(context: Context, uri: Uri) {
        _state.value = ImportState.Importing
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { parsePdfAndPersist(context, uri) }
                _state.value = result
            } catch (t: Throwable) {
                _state.value = ImportState.Error(t.message ?: "PDF import failed")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Detects format among three possibilities:
    //   A) 2-column simple:   Item, Amount
    //   B) 4-column new:      Item, Amount, Fixed/Variable, Mandatory
    //   C) Legacy sheet:      Item, Amount, Not pay, Real pay
    // ─────────────────────────────────────────────────────────────
    private suspend fun parseCsvAndPersist(context: Context, uri: Uri): ImportState {
        val rows = readCsvRows(context, uri).filter { it.isNotEmpty() }

        val allCells = rows.take(30).flatMap { it }
            .map { clean(it).lowercase() }

        val hasNewFormat = allCells.any {
            it == "fixed" || it.startsWith("not fix") ||
            it == "mandatory" || it == "not mandatory"
        }
        val hasLegacy = allCells.any { it == "current bank rs" || it == "not pay" }
        val hasSimple = allCells.any { it == "item" } && allCells.any { it == "amount" }

        return when {
            hasNewFormat -> parseNewFormat(rows)
            hasLegacy    -> parseLegacyFormat(rows)
            hasSimple    -> parseSimpleFormat(rows)
            else         -> parseSimpleFormat(rows)
        }
    }

    // ── FORMAT A: 2 columns (Item, Amount) ───────────────────────
    private suspend fun parseSimpleFormat(rows: List<List<String>>): ImportState {
        var headerIdx = -1
        var itemCol = -1
        var amountCol = -1

        for ((i, row) in rows.withIndex()) {
            val lower = row.map { clean(it).lowercase() }
            val itemIdx = lower.indexOfFirst { it == "item" || it == "item name" }
            val amountIdx = lower.indexOfFirst { it == "amount" || it == "budget amount" }
            if (itemIdx >= 0 && amountIdx >= 0) {
                headerIdx = i
                itemCol = itemIdx
                amountCol = amountIdx
                break
            }
        }

        if (headerIdx < 0) {
            itemCol = 0
            amountCol = 1
        }

        val expenses = mutableListOf<ExpenseLine>()

        for (i in (headerIdx + 1) until rows.size) {
            val row = rows[i]
            val item = clean(row.getOrNull(itemCol))
            if (item.isBlank()) continue
            if (item.equals("Total", ignoreCase = true)) continue
            if (item.equals("ITEM", ignoreCase = true)) continue

            val amount = parseAmount(clean(row.getOrNull(amountCol)))

            expenses += ExpenseLine(
                monthKey = currentMonth,
                itemName = item,
                budgetAmount = amount,
                notPaidAmount = 0.0,
                realPayAmount = amount,
                isMandatory = isMandatoryByKeyword(item),
                category = "Variable"
            )
        }

        if (expenses.isEmpty()) {
            val preview = rows.take(3).joinToString("\n") { r ->
                r.mapIndexed { idx, cell -> "[$idx]='${cell.take(30)}'" }.joinToString("  ")
            }
            return ImportState.Error(
                "No items found. Detected ${rows.size} rows. Preview:\n$preview"
            )
        }

        return commit(expenses, "CSV (2-column)")
    }

    // ── FORMAT B: 4 columns (Item, Amount, Fixed, Mandatory) ─────
    private suspend fun parseNewFormat(rows: List<List<String>>): ImportState {
        val expenses = mutableListOf<ExpenseLine>()
        for (row in rows) {
            val item = clean(row.getOrNull(0))
            if (item.isBlank()) continue
            if (item.equals("ITEM", ignoreCase = true)) continue
            if (item.equals("Item name", ignoreCase = true)) continue

            val amount = parseAmount(clean(row.getOrNull(1)))
            val cat = clean(row.getOrNull(2))
            val mand = clean(row.getOrNull(3))

            val isFixed = cat.equals("Fixed", ignoreCase = true)
            val category = if (isFixed) "Fixed" else "Variable"
            val isMandatory = mand.replace(" ", "")
                .equals("Mandatory", ignoreCase = true)

            expenses += ExpenseLine(
                monthKey = currentMonth,
                itemName = item,
                budgetAmount = amount,
                notPaidAmount = 0.0,
                realPayAmount = amount,
                isMandatory = isMandatory,
                category = category
            )
        }

        if (expenses.isEmpty()) {
            return ImportState.Error("No items in 4-column CSV")
        }
        return commit(expenses, "CSV (4-column)")
    }

    // ── FORMAT C: legacy sheet (Item, Amount, Not pay, Real pay) ──
    private suspend fun parseLegacyFormat(rows: List<List<String>>): ImportState {
        var openingBalance = 0.0
        var totalRealPay = 0.0
        var totalNotPaid = 0.0
        val expenses = mutableListOf<ExpenseLine>()
        var inExpenseSection = false

        for (row in rows) {
            val col0 = clean(row.getOrNull(0))
            val col1 = clean(row.getOrNull(1))
            val col2 = clean(row.getOrNull(2))
            val col3 = clean(row.getOrNull(3))

            when {
                col0.equals("Current Bank Rs", ignoreCase = true) ->
                    openingBalance = parseAmount(col1)

                col0.equals("ITEM", ignoreCase = true) ->
                    inExpenseSection = true

                col0.equals("Total", ignoreCase = true) -> {
                    totalRealPay = parseAmount(col3)
                    totalNotPaid = parseAmount(col2)
                }

                inExpenseSection && col0.isNotBlank() &&
                !col0.equals("MONTHLY Fixed Expenses", ignoreCase = true) -> {
                    val amount = parseAmount(col1)
                    val notPaid = parseAmount(col2)
                    val realPay = if (col3.isNotBlank()) parseAmount(col3) else amount - notPaid
                    if (amount > 0 || realPay > 0) {
                        expenses += ExpenseLine(
                            monthKey = currentMonth,
                            itemName = col0,
                            budgetAmount = amount,
                            notPaidAmount = notPaid,
                            realPayAmount = realPay,
                            isMandatory = isMandatoryByKeyword(col0)
                        )
                    }
                }
            }
        }

        if (expenses.isEmpty()) {
            return ImportState.Error("No items in legacy-format CSV")
        }

        db.expenseLineDao().deleteForMonth(currentMonth)
        db.expenseLineDao().insertAll(expenses)

        val mandatoryTotal = expenses.filter { it.isMandatory }.sumOf { it.realPayAmount }
        val optionalTotal = expenses.filterNot { it.isMandatory }.sumOf { it.realPayAmount }
        val actualReal = if (totalRealPay > 0) totalRealPay else mandatoryTotal + optionalTotal
        val actualNotPaid = if (totalNotPaid > 0) totalNotPaid else expenses.sumOf { it.notPaidAmount }

        val existing = db.budgetMonthDao().getByMonthSync(currentMonth)
        val opening = if (openingBalance > 0) openingBalance else existing?.openingBankBalance ?: 0.0
        val summary = (existing ?: BudgetMonth(monthKey = currentMonth)).copy(
            openingBankBalance = opening,
            totalMandatoryRealPay = mandatoryTotal,
            totalOptionalRealPay = optionalTotal,
            totalRealPay = actualReal,
            totalNotPaid = actualNotPaid,
            closingBalance = opening + (existing?.netSalaryFromPdf ?: 0.0) - actualReal,
            hasSheetImported = true
        )
        db.budgetMonthDao().insertOrUpdate(summary)

        return ImportState.Success(
            source = "CSV (legacy)",
            monthKey = currentMonth,
            detailLines = listOf(
                "Expenses imported: ${expenses.size}",
                "Opening balance: Rs. ${"%,.2f".format(opening)}",
                "Total real pay: Rs. ${"%,.2f".format(actualReal)}",
                "Not paid: Rs. ${"%,.2f".format(actualNotPaid)}"
            )
        )
    }

    // ── Shared persistence for formats A and B ──────────────────
    private suspend fun commit(
        expenses: List<ExpenseLine>,
        sourceLabel: String
    ): ImportState {
        db.expenseLineDao().deleteForMonth(currentMonth)
        db.expenseLineDao().insertAll(expenses)

        val mandatoryTotal = expenses.filter { it.isMandatory }.sumOf { it.realPayAmount }
        val optionalTotal = expenses.filterNot { it.isMandatory }.sumOf { it.realPayAmount }
        val totalReal = mandatoryTotal + optionalTotal

        val existing = db.budgetMonthDao().getByMonthSync(currentMonth)
        val opening = existing?.openingBankBalance ?: 0.0
        val summary = (existing ?: BudgetMonth(monthKey = currentMonth)).copy(
            totalMandatoryRealPay = mandatoryTotal,
            totalOptionalRealPay = optionalTotal,
            totalRealPay = totalReal,
            totalNotPaid = 0.0,
            closingBalance = opening + (existing?.netSalaryFromPdf ?: 0.0) - totalReal,
            hasSheetImported = true
        )
        db.budgetMonthDao().insertOrUpdate(summary)

        return ImportState.Success(
            source = sourceLabel,
            monthKey = currentMonth,
            detailLines = listOf(
                "Imported: ${expenses.size} items",
                "Mandatory: ${expenses.count { it.isMandatory }} — Rs. ${"%,.2f".format(mandatoryTotal)}",
                "Optional: ${expenses.count { !it.isMandatory }} — Rs. ${"%,.2f".format(optionalTotal)}",
                "Grand total: Rs. ${"%,.2f".format(totalReal)}"
            )
        )
    }

    // ── PDF ─────────────────────────────────────────────────────
    private suspend fun parsePdfAndPersist(context: Context, uri: Uri): ImportState {
        val record = SalarySlipParser.parse(context, uri, currentMonth)
        db.salaryRecordDao().insertOrUpdate(record)

        val existing = db.budgetMonthDao().getByMonthSync(currentMonth)
        val opening = existing?.openingBankBalance ?: 0.0
        val mandatory = existing?.totalMandatoryRealPay ?: 0.0
        val optional = existing?.totalOptionalRealPay ?: 0.0
        val totalReal = existing?.totalRealPay ?: (mandatory + optional)
        val summary = (existing ?: BudgetMonth(monthKey = currentMonth)).copy(
            netSalaryFromPdf = record.netSalary,
            closingBalance = opening + record.netSalary - totalReal,
            hasPdfImported = true
        )
        db.budgetMonthDao().insertOrUpdate(summary)

        return ImportState.Success(
            source = "PDF",
            monthKey = currentMonth,
            detailLines = listOf(
                "Basic Salary: Rs. ${"%,.2f".format(record.basicSalary)}",
                "Gross Salary: Rs. ${"%,.2f".format(record.grossSalary)}",
                "Total Deductions: Rs. ${"%,.2f".format(record.totalDeductions)}",
                "Net Salary: Rs. ${"%,.2f".format(record.netSalary)}"
            )
        )
    }

    // ── helpers ─────────────────────────────────────────────────
    private fun clean(raw: String?): String =
        raw?.replace("\uFEFF", "")?.trim()?.trim('"')?.trim().orEmpty()

    private fun readCsvRows(context: Context, uri: Uri): List<List<String>> {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            val charset = detectCharset(bytes)
            val text = String(bytes, charset).removePrefix("\uFEFF")
            return CSVReaderBuilder(StringReader(text)).build().use { csv ->
                csv.readAll().map { it.toList() }
            }
        } ?: throw IllegalStateException("Could not open CSV file")
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()) return Charsets.UTF_8

        if (bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE

        if (bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() &&
            bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE

        val sample = bytes.take(400)
        val nullCount = sample.count { it == 0.toByte() }
        return if (nullCount > sample.size / 4) Charsets.UTF_16LE else Charsets.UTF_8
    }

    private fun parseAmount(raw: String): Double {
        if (raw.isBlank()) return 0.0
        val cleaned = raw
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("LKR", "", ignoreCase = true)
            .replace(",", "")
            .replace("\"", "")
            .replace("\u00A0", "")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun isMandatoryByKeyword(item: String): Boolean {
        val lower = item.lowercase()
        val keywords = listOf(
            "rent", "mortgage", "electric", "water", "gas", "cell phone",
            "wifi", "slt", "peo tv", "van", "fees", "school", "poli",
            "washing", "grocery", "sanga", "nipuna", "mint pay", "amma",
            "senaya", "maiyon", "teacher", "codex", "band", "sachini"
        )
        return keywords.any { lower.contains(it) }
    }
}