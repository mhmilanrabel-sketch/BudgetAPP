package com.example.moneymanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.core.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun OfflineSecurityBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("security_badge"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted On-Device",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% Offline • Keystore 256-bit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MonthSelectorHeader(
    currentMonthKey: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYm = try {
        YearMonth.parse(currentMonthKey, DateTimeFormatter.ofPattern("yyyy-MM"))
    } catch (e: Exception) {
        YearMonth.now()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("month_selector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = currentYm.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    onMonthSelected(prev)
                },
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = CurrencyFormatter.formatMonth(currentMonthKey),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentMonthKey,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    val next = currentYm.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    onMonthSelected(next)
                },
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month"
                )
            }
        }
    }
}

@Composable
fun WarningBanner(
    warning: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Warning
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("warning_banner_item"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Warning",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun FinancialMetricRow(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    colorOverride: Color? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = CurrencyFormatter.formatLkr(amount),
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = colorOverride ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
