package com.example.moneymanager.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneymanager.ui.components.AmountText
import com.example.moneymanager.ui.components.PrivacyToggle

private val GREEN = Color(0xFF388E3C)
private val RED = Color(0xFFD32F2F)

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    vm: DashboardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val selectedMonth by vm.selectedMonth.collectAsState()
    val allMonths by vm.allMonths.collectAsState()

    var showSavingsDialog by remember { mutableStateOf(false) }

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header with privacy eye ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MoneyManager LK",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        monthLabel(selectedMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PrivacyToggle()
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "SQLCipher",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── History month chips ──
        item {
            Text(
                "History — tap to view any month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allMonths, key = { it }) { month ->
                    FilterChip(
                        selected = month == selectedMonth,
                        onClick = { vm.selectMonth(month) },
                        label = { Text(monthLabel(month)) }
                    )
                }
            }
        }

        // ── Closing balance hero ──
        item {
            val over = state.isOverspent
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (over) MaterialTheme.colorScheme.errorContainer
                                     else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Projected Closing Balance",
                        style = MaterialTheme.typography.labelMedium
                    )
                    AmountText(
                        amount = state.closingBalance,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (over) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opening ", style = MaterialTheme.typography.bodySmall)
                        AmountText(state.openingBalance,
                            style = MaterialTheme.typography.bodySmall)
                        Text(" + Net ", style = MaterialTheme.typography.bodySmall)
                        AmountText(state.netSalary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("− Expenses ", style = MaterialTheme.typography.bodySmall)
                        AmountText(state.totalRealPay,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Salary Summary (top 3 numbers) ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Salary Summary (from PDF)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    if (!state.hasPdfImported) {
                        Text(
                            "No salary slip imported for this month. " +
                            "Go to Import → Choose salary slip PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SalaryRow("Gross Salary", state.grossSalary, bold = true)
                        SalaryRow("Total Deductions", state.totalDeductions,
                            valueColor = RED)
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Net Salary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            AmountText(
                                amount = state.netSalary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = GREEN
                            )
                        }
                    }
                }
            }
        }

        // ── Savings ──
        item {
            SavingsCard(
                state = state,
                onEditTarget = { showSavingsDialog = true }
            )
        }

        // ── Full Salary Breakdown (fully privacy-aware) ──
        item {
            Text(
                "Full Salary Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Every line from your payslip PDF",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { SalaryBreakdownCard(state) }

        // ── Footer ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.expenseCount == 0 && state.netSalary == 0.0)
                            "No data for ${monthLabel(selectedMonth)}"
                        else
                            "${state.expenseCount} expense items this month",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showSavingsDialog) {
        SavingsTargetDialog(
            currentTarget = state.savingsTarget,
            monthLabel = monthLabel(selectedMonth),
            onDismiss = { showSavingsDialog = false },
            onSave = {
                vm.setSavingsTarget(it)
                showSavingsDialog = false
            }
        )
    }
}

// ── Savings card ──
@Composable
private fun SavingsCard(
    state: DashboardState,
    onEditTarget: () -> Unit
) {
    val target = state.savingsTarget
    val actual = state.savingsActual
    val hasTarget = target > 0
    val over = state.isOverspent
    val achieved = state.savingsAchieved

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                over -> MaterialTheme.colorScheme.errorContainer
                achieved -> Color(0xFFE8F5E9)
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Savings this month",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasTarget) {
                        Text(
                            "${state.savingsPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (achieved) GREEN
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = onEditTarget) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (hasTarget) "Edit" else "Set target")
                    }
                }
            }
            AmountText(
                amount = actual,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (over) RED else GREEN,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (hasTarget) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target: ", style = MaterialTheme.typography.bodySmall)
                    AmountText(target, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                val fraction = (actual / target).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = if (achieved) GREEN else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (achieved) "Above goal by " else "To reach target: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (achieved) GREEN
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        amount = if (achieved) actual - target else target - actual,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (achieved) GREEN
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No savings target set for this month. " +
                    "Tap 'Set target' above to define how much you want to keep.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Savings target dialog ──
@Composable
private fun SavingsTargetDialog(
    currentTarget: Double,
    monthLabel: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(
            currentTarget.takeIf { it > 0 }?.toString() ?: ""
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Savings target for $monthLabel") },
        text = {
            Column {
                Text(
                    "How much do you want to keep this month? " +
                    "Stored for this month only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { s ->
                        text = s.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Target (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = text.toDoubleOrNull() ?: 0.0
                onSave(value)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Salary breakdown card — every row now respects privacy ──
@Composable
private fun SalaryBreakdownCard(state: DashboardState) {
    if (!state.hasPdfImported) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Salary slip not imported",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Go to Import → Choose salary slip PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Earnings", MaterialTheme.colorScheme.primary)
            SalaryRow("Basic Salary", state.basicSalary)
            SalaryRow("Vehicle Allowance", state.vehicleAllowance)
            SalaryRow("Exceptional Incentive", state.exceptionalIncentive)
            SalaryRow("Shift Compensation", state.shiftCompensation)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            SalaryRow("Gross Salary", state.grossSalary, bold = true)

            Spacer(Modifier.height(14.dp))
            SectionLabel("Statutory Bases", MaterialTheme.colorScheme.primary)
            SalaryRow("Total For EPF", state.totalForEpf)
            SalaryRow("Total For ETF", state.totalForEtf)
            SalaryRow("Total For TAX", state.totalForTax)

            Spacer(Modifier.height(14.dp))
            SectionLabel("Deductions", RED)
            SalaryRow("APIT", state.apit)
            SalaryRow("EPF Employee", state.epfEmployee)
            SalaryRow("Funeral Fund", state.funeralFund)
            SalaryRow("Excess Mobile Usage", state.excessMobile)
            SalaryRow("Meals Deduction", state.mealsDeduction)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            SalaryRow("Total Deductions", state.totalDeductions,
                bold = true, valueColor = RED)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Net Salary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AmountText(
                    amount = state.netSalary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GREEN
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Credited to bank: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AmountText(
                    state.salaryToBank,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SalaryRow(
    label: String,
    amount: Double,
    bold: Boolean = false,
    valueColor: Color? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Uses AmountText → respects PrivacyState.isPrivate
        AmountText(
            amount = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun monthLabel(monthKey: String): String {
    return try {
        val parts = monthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val names = listOf("Jan","Feb","Mar","Apr","May","Jun",
                           "Jul","Aug","Sep","Oct","Nov","Dec")
        "${names[month - 1]} $year"
    } catch (t: Throwable) { monthKey }
}