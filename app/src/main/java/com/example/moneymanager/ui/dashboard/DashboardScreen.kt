package com.example.moneymanager.ui.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.ui.components.DiscrepancyBanner
import com.example.moneymanager.ui.components.FinancialMetricCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val reconciliation by viewModel.reconciliation.collectAsState()
    val allMonths by viewModel.allMonths.collectAsState()
    val monthData by viewModel.currentMonthData.collectAsState()
    val salaryRecord by viewModel.currentSalaryRecord.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Security badge & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MoneyManager LK",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Offline Secure Reconciler",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("hardware_security_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SQLCipher 256",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Month selector
        item {
            Text(
                text = "Reconciliation Month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val monthList = allMonths.map { it.monthKey }.distinct().toMutableList()
            if (!monthList.contains(selectedMonth)) {
                monthList.add(0, selectedMonth)
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(monthList) { monthKey ->
                    FilterChip(
                        selected = selectedMonth == monthKey,
                        onClick = { viewModel.selectMonth(monthKey) },
                        label = { Text(monthKey) },
                        modifier = Modifier.testTag("month_chip_$monthKey")
                    )
                }
            }
        }

        // Hero Card: Closing Balance & Formula
        item {
            val closingBal = reconciliation?.closingBalance ?: 0.0
            val isOverspent = reconciliation?.isOverspent == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_closing_balance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverspent) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Projected Closing Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverspent) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (isOverspent) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = "⚠️ OVERSPENT",
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = CurrencyFormatter.formatLkr(closingBal),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverspent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Sri Lankan Budget Equation explanation
                    Text(
                        text = "Opening Bank (${CurrencyFormatter.formatLkr(reconciliation?.openingBalance ?: 0.0)}) + " +
                                "Net Salary (${CurrencyFormatter.formatLkr(reconciliation?.netSalary ?: 0.0)}) - " +
                                "Total Real Pay (${CurrencyFormatter.formatLkr(reconciliation?.totalRealPay ?: 0.0)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverspent) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Import Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToImport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_import_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Files")
                }

                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_reconcile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconcile")
                }
            }
        }

        // Financial Metrics Grid
        item {
            Text(
                text = "Monthly Financial Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = "Opening Bank Rs",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.openingBalance ?: 0.0),
                    subtitle = "Cash on 1st of month",
                    modifier = Modifier.weight(1f).testTag("metric_opening_bank")
                )

                FinancialMetricCard(
                    title = "Net Salary (Take Home)",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.netSalary ?: 0.0),
                    subtitle = if (monthData?.hasPdfImported == true) "Verified from PDF" else "Pending PDF import",
                    modifier = Modifier.weight(1f).testTag("metric_net_salary")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = "Mandatory Real Pay",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.mandatoryRealPay ?: 0.0),
                    subtitle = "Rent, loans, bills, food",
                    modifier = Modifier.weight(1f).testTag("metric_mandatory_pay")
                )

                FinancialMetricCard(
                    title = "Optional Real Pay",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.optionalRealPay ?: 0.0),
                    subtitle = "Discretionary spending",
                    modifier = Modifier.weight(1f).testTag("metric_optional_pay")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = "Total Real Pay",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.totalRealPay ?: 0.0),
                    subtitle = "Actual outflow this month",
                    modifier = Modifier.weight(1f).testTag("metric_total_real_pay")
                )

                FinancialMetricCard(
                    title = "Not Paid (Carried Over)",
                    amount = CurrencyFormatter.formatLkr(reconciliation?.totalNotPaid ?: 0.0),
                    subtitle = "Unsettled obligations",
                    modifier = Modifier.weight(1f).testTag("metric_not_paid")
                )
            }
        }

        // Discrepancies and Warnings Section
        val discrepancies = reconciliation?.discrepancies ?: emptyList()
        if (discrepancies.isNotEmpty()) {
            item {
                Text(
                    text = "Reconciliation Warnings & Audits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(discrepancies) { discrepancy ->
                DiscrepancyBanner(discrepancy = discrepancy)
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All accounts reconciled cleanly. No discrepancies detected.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Quick Navigation to Details
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToExpenses,
                    modifier = Modifier.weight(1f).testTag("nav_to_expenses_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Expenses")
                }

                OutlinedButton(
                    onClick = onNavigateToSalary,
                    modifier = Modifier.weight(1f).testTag("nav_to_salary_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Salary")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
