package com.example.moneymanager.ui.salary

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.data.repository.MoneyManagerRepository

@Composable
fun SalaryBreakdownScreen(
    repository: MoneyManagerRepository,
    selectedMonthKey: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allMonths by repository.getAllMonths().collectAsState(initial = emptyList())
    val salaryRecord by repository.getSalaryRecord(selectedMonthKey).collectAsState(initial = null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("salary_breakdown_screen")
    ) {
        Text(
            text = "Salary Breakdown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Itemized earnings, statutory EPF/APIT deductions & contributions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        if (salaryRecord == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payslip imported for $selectedMonthKey.\nGo to the Import tab to load a PDF salary slip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val slip = salaryRecord!!
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Net Pay Hero
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Net Take Home Pay",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = CurrencyFormatter.formatLkr(slip.netSalary),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Gross (${CurrencyFormatter.formatLkr(slip.grossSalary)}) - Deductions (${CurrencyFormatter.formatLkr(slip.totalDeductions)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Earnings Section
                item {
                    BreakdownSectionCard(title = "Earnings & Allowances") {
                        SalaryRowItem("Basic Salary", slip.basicSalary)
                        SalaryRowItem("Vehicle Allowance", slip.vehicleAllowance)
                        SalaryRowItem("Exceptional Incentive", slip.exceptionalIncentive)
                        SalaryRowItem("Shift Compensation", slip.shiftCompensation)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        SalaryRowItem("Gross Salary", slip.grossSalary, isBold = true, highlightColor = MaterialTheme.colorScheme.primary)
                    }
                }

                // Deductions Section
                item {
                    BreakdownSectionCard(title = "Deductions & Statutory Contributions") {
                        SalaryRowItem("APIT (Tax)", slip.apit)
                        SalaryRowItem("EPF Employee (8%)", slip.epfEmployee)
                        SalaryRowItem("Funeral Fund", slip.funeralFund)
                        SalaryRowItem("Excess Mobile", slip.excessMobile)
                        SalaryRowItem("Meals", slip.mealsDeduction)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        SalaryRowItem("Total Deductions", slip.totalDeductions, isBold = true, highlightColor = MaterialTheme.colorScheme.error)
                    }
                }

                // Employer Benefit Contributions
                item {
                    BreakdownSectionCard(title = "Employer Contributions (Benefits)") {
                        SalaryRowItem("EPF Employer (12%)", slip.epfEmployer)
                        SalaryRowItem("ETF Employer (3%)", slip.etfEmployer)
                        val totalEmployer = slip.epfEmployer + slip.etfEmployer
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        SalaryRowItem("Total Employer Contribution", totalEmployer, isBold = true)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun BreakdownSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SalaryRowItem(
    label: String,
    amount: Double,
    isBold: Boolean = false,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = CurrencyFormatter.formatLkr(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
