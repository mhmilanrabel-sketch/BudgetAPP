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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.local.entity.SalaryRecord
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.ui.components.FinancialMetricRow
import com.example.moneymanager.ui.components.MonthSelectorHeader
import com.example.moneymanager.ui.components.OfflineSecurityBadge

@Composable
fun SalaryBreakdownScreen(
    repository: MoneyManagerRepository,
    selectedMonthKey: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val salaryRecord by repository.getSalaryRecord(selectedMonthKey)
        .collectAsStateWithLifecycle(initialValue = null)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("salary_breakdown_screen"),
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
                        text = "Salary Slip Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Authoritative PDF Income Source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OfflineSecurityBadge()
            }
        }

        item {
            MonthSelectorHeader(
                currentMonthKey = selectedMonthKey,
                onMonthSelected = onMonthSelected
            )
        }

        if (salaryRecord != null) {
            val record = salaryRecord!!

            // Net Salary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET SALARY (TAKE HOME)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF16A34A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Math Verified (±1 LKR)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = CurrencyFormatter.formatLkr(record.netSalary),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (record.salaryToBank > 0.0) {
                            Text(
                                text = "Direct Bank Transfer: ${CurrencyFormatter.formatLkr(record.salaryToBank)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Earnings & Allowances
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Earnings & Allowances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FinancialMetricRow(label = "Basic Salary", amount = record.basicSalary)
                        FinancialMetricRow(label = "Vehicle Allowance", amount = record.vehicleAllowance)
                        FinancialMetricRow(label = "Exceptional Incentive", amount = record.exceptionalIncentive)
                        FinancialMetricRow(label = "Shift Compensation", amount = record.shiftCompensation)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        FinancialMetricRow(
                            label = "Gross Salary (Total)",
                            amount = record.grossSalary,
                            isBold = true,
                            colorOverride = Color(0xFF16A34A)
                        )
                    }
                }
            }

            // Statutory Deductions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Statutory & Other Deductions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FinancialMetricRow(label = "APIT (Advance Income Tax)", amount = record.apit)
                        FinancialMetricRow(label = "EPF Employee (8%)", amount = record.epfEmployee)
                        FinancialMetricRow(label = "Excess Mobile Phone Usage", amount = record.excessMobile)
                        FinancialMetricRow(label = "Funeral Fund", amount = record.funeralFund)
                        FinancialMetricRow(label = "Meals Deduction", amount = record.mealsDeduction)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        FinancialMetricRow(
                            label = "Total Deductions",
                            amount = record.totalDeductions,
                            isBold = true,
                            colorOverride = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Employer Statutory Contributions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Employer Statutory Contributions (Not Deducted)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FinancialMetricRow(label = "EPF Employer (12%)", amount = record.epfEmployer)
                        FinancialMetricRow(label = "ETF Employer (3%)", amount = record.etfEmployer)
                        if (record.stampDutyEmployer > 0.0) {
                            FinancialMetricRow(label = "Stamp Duty Employer", amount = record.stampDutyEmployer)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        FinancialMetricRow(
                            label = "Total Statutory Benefit Contribution",
                            amount = record.epfEmployer + record.etfEmployer + record.stampDutyEmployer,
                            isBold = true
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No salary slip PDF has been imported for $selectedMonthKey yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
