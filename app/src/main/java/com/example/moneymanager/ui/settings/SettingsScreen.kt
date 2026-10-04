package com.example.moneymanager.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moneymanager.core.security.BiometricAuthManager
import com.example.moneymanager.data.parser.BudgetSheetParser
import com.example.moneymanager.ui.components.OfflineSecurityBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    biometricAuthManager: BiometricAuthManager,
    budgetSheetParser: BudgetSheetParser,
    modifier: Modifier = Modifier
) {
    var isBiometricEnabled by remember { mutableStateOf(biometricAuthManager.isBiometricEnabled) }
    var keywords by remember { mutableStateOf(budgetSheetParser.getSavedMandatoryKeywords()) }
    var newKeywordText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
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
                        text = "Security & Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Biometric Lock & Classification Rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OfflineSecurityBadge()
            }
        }

        // Biometric Security Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Biometric App Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Require fingerprint / face recognition upon opening",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { checked ->
                                isBiometricEnabled = checked
                                biometricAuthManager.isBiometricEnabled = checked
                            },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }
                }
            }
        }

        // Mandatory Keyword Classification Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mandatory Expense Keywords",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                keywords = BudgetSheetParser.DEFAULT_MANDATORY_KEYWORDS
                                budgetSheetParser.saveMandatoryKeywords(keywords)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Keywords",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Any item name containing these keywords (case-insensitive) is automatically classified as a Mandatory fixed expense.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Add keyword row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newKeywordText,
                            onValueChange = { newKeywordText = it },
                            placeholder = { Text("e.g. tuition, insurance") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_keyword_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newKeywordText.trim().lowercase()
                                if (trimmed.isNotBlank() && !keywords.contains(trimmed)) {
                                    val updated = keywords + trimmed
                                    keywords = updated
                                    budgetSheetParser.saveMandatoryKeywords(updated)
                                    newKeywordText = ""
                                }
                            },
                            modifier = Modifier.testTag("add_keyword_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chips layout
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keywords.forEach { keyword ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text(keyword) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val updated = keywords.filter { it != keyword }
                                                keywords = updated
                                                budgetSheetParser.saveMandatoryKeywords(updated)
                                            },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Security Architecture Verification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Architecture & Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SecurityFeatureItem(
                        title = "100% Offline by Design",
                        description = "android.permission.INTERNET is completely absent from AndroidManifest.xml. Zero telemetry or network requests."
                    )
                    SecurityFeatureItem(
                        title = "SQLCipher 256-bit Database",
                        description = "SQLite database is fully encrypted at rest using a 256-bit passphrase managed by Android KeyStore hardware."
                    )
                    SecurityFeatureItem(
                        title = "FLAG_SECURE Screen Protection",
                        description = "Screenshots, screen recording, and task switcher previews of financial balances are prevented."
                    )
                    SecurityFeatureItem(
                        title = "Zero Disk Footprint",
                        description = "Uploaded PDF, CSV, and XLSX streams are parsed purely in-memory from InputStreams without writing files to storage."
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityFeatureItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )
    }
}
