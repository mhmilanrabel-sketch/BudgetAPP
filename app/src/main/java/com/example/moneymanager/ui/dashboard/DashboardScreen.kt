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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.ui.components.FinancialMetricRow
import com.example.moneymanager.ui.components.MonthSelectorHeader
import com.example.moneymanager.ui.components.OfflineSecurityBadge
import com.example.moneymanager.ui.components.WarningBanner

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val budgetMonth by viewModel.budgetMonth.collectAsStateWithLifecycle()
    val salaryRecord by viewModel.salaryRecord.collectAsStateWithLifecycle()
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()

    val rawWarnings = summary?.warningsText?.lines()?.filter { it.isNotBlank() } ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "MoneyManager LK",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Sri Lanka Salary & Budget Reconciler",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OfflineSecurityBadge()
            }
        }

        item {
            MonthSelectorHeader(
                currentMonthKey = selectedMonth,
                onMonthSelected = { viewModel.selectMonth(it) }
            )
        }

        // Warnings & Alerts Section
        if (rawWarnings.isNotEmpty()) {
            item {
                Text(
                    text = "Discrepancies & Warnings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(rawWarnings) { warning ->
                WarningBanner(warning = warning)
            }
        }

        // Hero Financial Reconciliation Card
        item {
            HeroReconciliationCard(
                month = budgetMonth,
                salaryRecord = salaryRecord,
                onNavigateToImport = onNavigateToImport
            )
        }

        // Breakdown Summary Cards
        if (budgetMonth != null) {
            item {
                IncomeExpenseSummaryCard(
                    month = budgetMonth!!,
                    onNavigateToExpenses = onNavigateToExpenses,
                    onNavigateToSalary = onNavigateToSalary
                )
            }

            item {
                SavingsAndCarryForwardCard(month = budgetMonth!!)
            }
        } else {
            item {
                EmptyMonthStateCard(
                    monthKey = selectedMonth,
                    onNavigateToImport = onNavigateToImport
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroReconciliationCard(
    month: BudgetMonth?,
    salaryRecord: SalaryRecord?,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverspent = (month?.closingBalance ?: 0.0) < 0.0
    val netSalary = month?.netSalaryFromPdf ?: salaryRecord?.netSalary ?: 0.0
    val openingBank = month?.openingBankBalance ?: 0.0
    val totalRealPay = month?.totalRealPay ?: 0.0
    val closingBalance = if (month != null) {
        month.closingBalance
    } else {
        openingBank + netSalary - totalRealPay
    }

    val gradientBrush = if (isOverspent) {
        Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF002B24)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_reconciliation_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLOSING BANK BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOverspent) Color(0xFFDC2626) else Color(0xFF16A34A)
                    ) {
                        Text(
                            text = if (isOverspent) "Overspent" else "Reconciled",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = CurrencyFormatter.formatLkr(closingBalance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Formula: Opening (${CurrencyFormatter.formatLkr(openingBank)}) + Net Salary (${CurrencyFormatter.formatLkr(netSalary)}) - Total Outflow (${CurrencyFormatter.formatLkr(totalRealPay)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Net Salary (PDF)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatLkr(netSalary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Real Outflow",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatLkr(totalRealPay),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseSummaryCard(
    month: BudgetMonth,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("breakdown_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Monthly Financial Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            FinancialMetricRow(
                label = "Opening Bank Balance",
                amount = month.openingBankBalance
            )
            FinancialMetricRow(
                label = "Net Salary from Slip (Income)",
                amount = month.netSalaryFromPdf,
                colorOverride = Color(0xFF16A34A),
                isBold = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            FinancialMetricRow(
                label = "Mandatory Real Pay (Fixed)",
                amount = month.totalMandatoryRealPay
            )
            FinancialMetricRow(
                label = "Optional Real Pay (Discretionary)",
                amount = month.totalOptionalRealPay
            )
            FinancialMetricRow(
                label = "Total Real Pay (Cash Outflow)",
                amount = month.totalRealPay,
                colorOverride = MaterialTheme.colorScheme.error,
                isBold = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToExpenses,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_expenses_button")
                ) {
                    Text("View Expenses")
                }
                Button(
                    onClick = onNavigateToSalary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_salary_button")
                ) {
                    Text("Slip Details")
                }
            }
        }
    }
}

@Composable
private fun SavingsAndCarryForwardCard(
    month: BudgetMonth,
    modifier: Modifier = Modifier
) {
    val savingsMet = month.closingBalance >= month.savingsTarget

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_target_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Savings",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Savings & Unpaid Obligations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (month.savingsTarget > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (savingsMet) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (savingsMet) "Target Met" else "Target Unmet",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (savingsMet) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FinancialMetricRow(
                label = "Savings Target (Sheet)",
                amount = month.savingsTarget
            )
            FinancialMetricRow(
                label = "Unpaid Bills (Carry Forward)",
                amount = month.totalNotPaid,
                colorOverride = if (month.totalNotPaid > 0) Color(0xFFD97706) else null
            )
            if (month.sheetHandSave > 0.0) {
                FinancialMetricRow(
                    label = "Target 'Hand Save' (Sheet)",
                    amount = month.sheetHandSave
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthStateCard(
    monthKey: String,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_month_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Data for $monthKey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Import a Sri Lankan Salary Slip PDF and a Budget CSV/XLSX to automatically reconcile this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onNavigateToImport,
                modifier = Modifier.testTag("import_now_button")
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Documents")
            }
        }
    }
}
