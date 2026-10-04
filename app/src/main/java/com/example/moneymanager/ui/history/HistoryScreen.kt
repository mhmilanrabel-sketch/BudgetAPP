package com.example.moneymanager.ui.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.ui.components.OfflineSecurityBadge

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onSelectMonth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    var monthToDelete by remember { mutableStateOf<String?>(null) }

    if (monthToDelete != null) {
        AlertDialog(
            onDismissRequest = { monthToDelete = null },
            title = { Text("Delete Financial Records") },
            text = { Text("Are you sure you want to delete all salary slip and budget records for $monthToDelete? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        monthToDelete?.let { viewModel.deleteMonth(it) }
                        monthToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { monthToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("history_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Archives",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Historical Reconciliation Records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OfflineSecurityBadge()
            }
        }

        if (months.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No monthly records found yet. Import documents to start building your financial archive.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(months, key = { it.monthKey }) { month ->
                HistoryMonthCard(
                    month = month,
                    onSelect = { onSelectMonth(month.monthKey) },
                    onDelete = { monthToDelete = month.monthKey }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryMonthCard(
    month: BudgetMonth,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverspent = month.closingBalance < 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_card_${month.monthKey}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = CurrencyFormatter.formatMonth(month.monthKey),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = month.monthKey,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOverspent) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (isOverspent) "Overspent" else "Balanced",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverspent) Color(0xFFDC2626) else Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_month_${month.monthKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Month",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Net Salary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(month.netSalaryFromPdf),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Real Outflow",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(month.totalRealPay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Closing Bank",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatLkr(month.closingBalance),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverspent) MaterialTheme.colorScheme.error else Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}
