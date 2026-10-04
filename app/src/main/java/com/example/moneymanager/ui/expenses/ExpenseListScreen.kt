package com.example.moneymanager.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.repository.MoneyManagerRepository
import kotlinx.coroutines.launch

@Composable
fun ExpenseListScreen(
    repository: MoneyManagerRepository,
    selectedMonthKey: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allMonths by repository.getAllMonths().collectAsState(initial = emptyList())
    val expenses by repository.getExpensesForMonth(selectedMonthKey).collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Items", "Mandatory", "Optional", "Not Paid (Carried)")

    val filteredExpenses = when (selectedTabIndex) {
        1 -> expenses.filter { it.isMandatory }
        2 -> expenses.filter { !it.isMandatory }
        3 -> expenses.filter { it.notPaidAmount > 0.0 }
        else -> expenses
    }

    val totalRealOutflow = filteredExpenses.sumOf { it.realPayAmount }
    val totalNotPaid = filteredExpenses.sumOf { it.notPaidAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("expense_list_screen")
    ) {
        Text(
            text = "Expense Breakdown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Month Selector
        val monthList = allMonths.map { it.monthKey }.distinct().toMutableList()
        if (!monthList.contains(selectedMonthKey)) {
            monthList.add(0, selectedMonthKey)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(monthList) { mKey ->
                FilterChip(
                    selected = selectedMonthKey == mKey,
                    onClick = { onMonthSelected(mKey) },
                    label = { Text(mKey) }
                )
            }
        }

        // Summary banner for current tab view
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Real Pay", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyFormatter.formatLkr(totalRealOutflow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Unpaid / Carried Over", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = CurrencyFormatter.formatLkr(totalNotPaid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalNotPaid > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expense lines recorded for $selectedMonthKey.\nImport a budget CSV/XLSX to view itemized expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredExpenses, key = { it.id }) { item ->
                    ExpenseItemCard(
                        expense = item,
                        onToggleMandatory = { isMandatory ->
                            coroutineScope.launch {
                                repository.updateExpenseCategory(
                                    expenseId = item.id,
                                    isMandatory = isMandatory,
                                    category = if (isMandatory) "Mandatory" else "Optional",
                                    monthKey = selectedMonthKey
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseLine,
    onToggleMandatory: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.itemName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (expense.isMandatory) "Fixed / Mandatory" else "Discretionary / Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expense.isMandatory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mandatory", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = expense.isMandatory,
                        onCheckedChange = onToggleMandatory,
                        modifier = Modifier.testTag("switch_mandatory_${expense.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Bill Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = CurrencyFormatter.formatLkr(expense.budgetAmount), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Not Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatLkr(expense.notPaidAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (expense.notPaidAmount > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Real Cash Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatLkr(expense.realPayAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
