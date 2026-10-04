package com.example.moneymanager.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.ui.components.MonthSelectorHeader
import kotlinx.coroutines.launch

enum class ExpensePaymentFilter {
    ALL,
    REAL_PAY,
    NOT_PAID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    repository: MoneyManagerRepository,
    selectedMonthKey: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expenses by repository.getExpensesForMonth(selectedMonthKey)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Mandatory, 2: Optional
    var paymentFilter by remember { mutableStateOf(ExpensePaymentFilter.ALL) }

    val filteredExpenses = expenses.filter { item ->
        val matchesTab = when (selectedTabIndex) {
            1 -> item.isMandatory
            2 -> !item.isMandatory
            else -> true
        }
        val matchesPayment = when (paymentFilter) {
            ExpensePaymentFilter.REAL_PAY -> item.realPayAmount > 0.0
            ExpensePaymentFilter.NOT_PAID -> item.notPaidAmount > 0.0
            ExpensePaymentFilter.ALL -> true
        }
        matchesTab && matchesPayment
    }

    val totalFilteredBudget = filteredExpenses.sumOf { it.budgetAmount }
    val totalFilteredRealPay = filteredExpenses.sumOf { it.realPayAmount }
    val totalFilteredNotPaid = filteredExpenses.sumOf { it.notPaidAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("expense_list_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Expense Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Classified as Mandatory vs Optional",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${filteredExpenses.size} items",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MonthSelectorHeader(
            currentMonthKey = selectedMonthKey,
            onMonthSelected = onMonthSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Tabs: All / Mandatory / Optional
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("All (${expenses.size})") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Mandatory (${expenses.count { it.isMandatory }})") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Optional (${expenses.count { !it.isMandatory }})") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payment status filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = paymentFilter == ExpensePaymentFilter.ALL,
                onClick = { paymentFilter = ExpensePaymentFilter.ALL },
                label = { Text("All Status") },
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = paymentFilter == ExpensePaymentFilter.REAL_PAY,
                onClick = { paymentFilter = ExpensePaymentFilter.REAL_PAY },
                label = { Text("Real Pay") },
                modifier = Modifier.testTag("filter_real_pay")
            )
            FilterChip(
                selected = paymentFilter == ExpensePaymentFilter.NOT_PAID,
                onClick = { paymentFilter = ExpensePaymentFilter.NOT_PAID },
                label = { Text("Unpaid / Carry") },
                modifier = Modifier.testTag("filter_not_paid")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses matching current filter for $selectedMonthKey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { item ->
                    ExpenseItemCard(
                        item = item,
                        onToggleMandatory = {
                            scope.launch {
                                repository.toggleExpenseMandatory(item.id, !item.isMandatory, selectedMonthKey)
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Summary bar at bottom
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("expense_summary_footer"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filtered Real Pay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(totalFilteredRealPay),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (totalFilteredNotPaid > 0.0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Unpaid (Carry)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatLkr(totalFilteredNotPaid),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemCard(
    item: ExpenseLine,
    onToggleMandatory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Category toggle badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (item.isMandatory) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    modifier = Modifier
                        .clickable { onToggleMandatory() }
                        .testTag("toggle_mandatory_${item.id}")
                ) {
                    Text(
                        text = if (item.isMandatory) "Mandatory ↺" else "Optional ↺",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isMandatory) Color(0xFF16A34A) else Color(0xFFB45309)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Budget Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(item.budgetAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.notPaidAmount > 0.0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Not Paid",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = CurrencyFormatter.formatLkr(item.notPaidAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Real Cash Pay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(item.realPayAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
