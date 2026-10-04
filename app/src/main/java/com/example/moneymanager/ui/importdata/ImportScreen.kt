package com.example.moneymanager.ui.importdata

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.ui.components.MonthSelectorHeader
import com.example.moneymanager.ui.components.OfflineSecurityBadge

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    // File pickers
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importSalaryPdf(context, uri)
        }
    }

    val sheetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBudgetSheet(context, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    viewModel.runOcrOnBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Handled in VM
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("import_screen"),
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
                        text = "Document Import",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Direct stream parsing • Zero cloud sync",
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
                onMonthSelected = { viewModel.setMonth(it) }
            )
        }

        // Status Cards
        when (val s = status) {
            is ImportStatus.Processing -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = s.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is ImportStatus.Success -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_success_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF16A34A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import Verified",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Text("View in Dashboard")
                            }
                        }
                    }
                }
            }
            is ImportStatus.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_error_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import Discrepancy",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.resetStatus() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            is ImportStatus.NeedsOcr -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Scanned PDF Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Page Image / OCR")
                            }
                        }
                    }
                }
            }
            ImportStatus.Idle -> {}
        }

        // Two Primary Import Cards (PDF + CSV/XLSX)
        item {
            Text(
                text = "Select Document to Reconcile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Card 1: Salary Slip PDF
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_pdf_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "1. Salary Slip PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sri Lanka 2-Column Pay Slip (LKR)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Parses Gross, EPF, ETF, APIT, and Net Salary with strict mathematical validation (Gross - Deductions = Net).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Salary Slip PDF")
                    }
                }
            }
        }

        // Card 2: Budget Sheet CSV/XLSX
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_sheet_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF006A60),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "2. Monthly Budget CSV / XLSX",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Left (Expenses A–D) & Right (Salary K–N)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Reads 'Current Bank Rs' opening balance, auto-categorizes mandatory vs optional expenses, and extracts targets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { sheetPickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_sheet_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Budget CSV or XLSX")
                    }
                }
            }
        }

        // Quick Real-World Demo / Sample Data loader
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sample_data_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test with Real-World Sri Lankan Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Load the exact sample Salary Slip (Rs. 268,569.16 Gross) and 2-section budget (20 expense items) from the project specification with 1 click.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.loadRealisticSampleData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_sample_data_button")
                    ) {
                        Text("Load Specification Sample Data")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
