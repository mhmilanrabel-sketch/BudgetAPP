package com.example.moneymanager.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.ExpenseLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PaymentStatus { PAID, PARTIAL, NOT_PAID }

data class ExpensesState(
    val month: String = "",
    val mandatory: List<ExpenseLine> = emptyList(),
    val optional: List<ExpenseLine> = emptyList(),
    val mandatoryTotal: Double = 0.0,
    val optionalTotal: Double = 0.0,
    val notPaidTotal: Double = 0.0,
    val paidTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val loading: Boolean = true
)

data class CarryForwardItem(
    val fromMonth: String,
    val itemName: String,
    val unpaidAmount: Double,
    val isMandatory: Boolean,
    val category: String
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val TEMPLATE_MONTH = "__TEMPLATE__"
        const val CARRIED_CATEGORY_TAG = "Carried"
    }

    private val db = AppDatabase.getInstance(app)

    private val defaultMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _selectedMonth = MutableStateFlow(defaultMonth)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _carryForward = MutableStateFlow<List<CarryForwardItem>>(emptyList())
    val carryForward: StateFlow<List<CarryForwardItem>> = _carryForward.asStateFlow()

    private val seededThisSession = mutableSetOf<String>()

    val allMonths: StateFlow<List<String>> =
        db.budgetMonthDao().getAllMonths()
            .map { list ->
                val keys = list.map { it.monthKey }
                    .filterNot { it == TEMPLATE_MONTH }
                    .sortedDescending()
                    .toMutableList()
                if (!keys.contains(defaultMonth)) keys.add(0, defaultMonth)
                keys
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(defaultMonth))

    val templateCount: StateFlow<Int> =
        db.expenseLineDao().getExpensesForMonth(TEMPLATE_MONTH)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ExpensesState> = _selectedMonth.flatMapLatest { month ->
        db.expenseLineDao().getExpensesForMonth(month).map { lines ->
            val mandatory = lines.filter { it.isMandatory }
            val optional = lines.filterNot { it.isMandatory }
            ExpensesState(
                month = month,
                mandatory = mandatory,
                optional = optional,
                mandatoryTotal = mandatory.sumOf { it.realPayAmount },
                optionalTotal = optional.sumOf { it.realPayAmount },
                notPaidTotal = lines.sumOf { it.notPaidAmount },
                paidTotal = lines.sumOf { it.realPayAmount },
                grandTotal = lines.sumOf { it.budgetAmount },
                loading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
              ExpensesState(month = defaultMonth))

    fun selectMonth(month: String) {
        _selectedMonth.value = month
        refreshCarryForward(month)
    }

    // ── TEMPLATE ──────────────────────────────────────────────
    fun ensureSeeded(month: String) {
        if (seededThisSession.contains(month)) return
        seededThisSession.add(month)
        viewModelScope.launch {
            val existing = db.expenseLineDao().getExpensesForMonthSync(month)
            if (existing.isNotEmpty()) return@launch
            val template = db.expenseLineDao().getExpensesForMonthSync(TEMPLATE_MONTH)
            if (template.isEmpty()) return@launch
            val seeded = template.map { it.copy(id = 0, monthKey = month) }
            db.expenseLineDao().insertAll(seeded)
        }
    }

    fun saveCurrentAsTemplate(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val current = db.expenseLineDao().getExpensesForMonthSync(_selectedMonth.value)
            db.expenseLineDao().deleteForMonth(TEMPLATE_MONTH)
            if (current.isNotEmpty()) {
                val templated = current.map { it.copy(id = 0, monthKey = TEMPLATE_MONTH) }
                db.expenseLineDao().insertAll(templated)
            }
            onDone(current.size)
        }
    }

    fun clearTemplate() {
        viewModelScope.launch {
            db.expenseLineDao().deleteForMonth(TEMPLATE_MONTH)
        }
    }

    // ── CARRY FORWARD ─────────────────────────────────────────
    private fun refreshCarryForward(currentMonth: String) {
        viewModelScope.launch {
            val prev = findPreviousMonthWithData(currentMonth)
            if (prev == null) {
                _carryForward.value = emptyList()
                return@launch
            }
            val prevLines = db.expenseLineDao().getExpensesForMonthSync(prev)
            val currentLines = db.expenseLineDao().getExpensesForMonthSync(currentMonth)

            // A previously-carried item is tracked by category = "Carried" + itemName prefix
            val alreadyBrought = currentLines
                .filter { it.category == CARRIED_CATEGORY_TAG }
                .map { it.itemName.lowercase() }
                .toSet()

            val items = prevLines
                .filter { it.notPaidAmount > 0.0 }
                .filterNot { it.category == CARRIED_CATEGORY_TAG }
                .filterNot { alreadyBrought.contains("${it.itemName} (from $prev)".lowercase()) }
                .map {
                    CarryForwardItem(
                        fromMonth = prev,
                        itemName = it.itemName,
                        unpaidAmount = it.notPaidAmount,
                        isMandatory = it.isMandatory,
                        category = it.category
                    )
                }
            _carryForward.value = items
        }
    }

    private suspend fun findPreviousMonthWithData(currentMonth: String): String? {
        val all = db.budgetMonthDao().getAllMonths()
        // Can't use Flow directly here; use a sync-friendly query
        // We'll grab months from expense table
        val allLines = mutableListOf<String>()
        // Cheap approach: check months from budget_month table via a public method
        // Not available, so use a different path: check last 6 months explicitly
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val cur = fmt.parse(currentMonth) ?: return null
        val cal = java.util.Calendar.getInstance()
        cal.time = cur
        for (i in 1..6) {
            cal.add(java.util.Calendar.MONTH, -1)
            val m = fmt.format(cal.time)
            val rows = db.expenseLineDao().getExpensesForMonthSync(m)
            if (rows.isNotEmpty()) return m
        }
        return null
    }

    /** Add a specific carried item as a new line in the current month. */
    fun addCarryForwardItem(item: CarryForwardItem, onDone: () -> Unit) {
        viewModelScope.launch {
            val newLine = ExpenseLine(
                monthKey = _selectedMonth.value,
                itemName = "${item.itemName} (from ${item.fromMonth})",
                budgetAmount = item.unpaidAmount,
                notPaidAmount = 0.0,
                realPayAmount = item.unpaidAmount,
                isMandatory = item.isMandatory,
                category = CARRIED_CATEGORY_TAG
            )
            db.expenseLineDao().insertAll(listOf(newLine))
            _carryForward.value = _carryForward.value.filterNot { it == item }
            onDone()
        }
    }

    fun addAllCarryForward(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val items = _carryForward.value
            if (items.isEmpty()) { onDone(0); return@launch }
            val newLines = items.map {
                ExpenseLine(
                    monthKey = _selectedMonth.value,
                    itemName = "${it.itemName} (from ${it.fromMonth})",
                    budgetAmount = it.unpaidAmount,
                    notPaidAmount = 0.0,
                    realPayAmount = it.unpaidAmount,
                    isMandatory = it.isMandatory,
                    category = CARRIED_CATEGORY_TAG
                )
            }
            db.expenseLineDao().insertAll(newLines)
            _carryForward.value = emptyList()
            onDone(newLines.size)
        }
    }

    fun dismissCarryForward() {
        _carryForward.value = emptyList()
    }

    // ── MANUAL OPS ────────────────────────────────────────────
    fun addOrUpdate(line: ExpenseLine) {
        viewModelScope.launch {
            db.expenseLineDao().insertAll(listOf(line))
        }
    }

    fun delete(line: ExpenseLine) {
        viewModelScope.launch {
            val all = db.expenseLineDao().getExpensesForMonthSync(line.monthKey)
            val remaining = all.filter { it.id != line.id }
            db.expenseLineDao().deleteForMonth(line.monthKey)
            if (remaining.isNotEmpty()) db.expenseLineDao().insertAll(remaining)
        }
    }

    // ── CLIPBOARD PASTE ───────────────────────────────────────
    fun pasteFromClipboard(text: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val rows = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val toInsert = mutableListOf<ExpenseLine>()
            for (row in rows) {
                val cols = row.split('\t', ',')
                    .map { it.trim().trim('"') }
                    .filter { it.isNotEmpty() }
                if (cols.isEmpty()) continue
                if (cols[0].equals("ITEM", ignoreCase = true)) continue
                if (cols[0].equals("Item name", ignoreCase = true)) continue

                val item = cols[0]
                val amount = cols.getOrNull(1)?.let { parseAmount(it) } ?: 0.0
                val category = cols.getOrNull(2)?.let {
                    if (it.equals("Fixed", true)) "Fixed" else "Variable"
                } ?: "Variable"
                val mandatory = cols.getOrNull(3)?.replace(" ", "")
                    ?.equals("Mandatory", ignoreCase = true)
                    ?: isMandatoryByKeyword(item)

                toInsert += ExpenseLine(
                    monthKey = _selectedMonth.value,
                    itemName = item,
                    budgetAmount = amount,
                    notPaidAmount = 0.0,
                    realPayAmount = amount,
                    isMandatory = mandatory,
                    category = category
                )
            }
            if (toInsert.isNotEmpty()) {
                val existing = db.expenseLineDao()
                    .getExpensesForMonthSync(_selectedMonth.value)
                val existingNames = existing.map { it.itemName.lowercase() }.toSet()
                val filtered = toInsert.filterNot {
                    existingNames.contains(it.itemName.lowercase())
                }
                db.expenseLineDao().insertAll(filtered)
                onDone(filtered.size)
            } else onDone(0)
        }
    }

    private fun parseAmount(raw: String): Double {
        val cleaned = raw
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("LKR", "", ignoreCase = true)
            .replace(",", "")
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