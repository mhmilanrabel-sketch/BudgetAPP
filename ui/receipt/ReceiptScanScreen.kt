package com.example.moneymanager.ui.receipt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    vm: ReceiptScanViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val state by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) vm.scan(uri)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Scan Receipt") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { picker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose receipt image")
            }

            when (val s = state) {
                ScanState.Idle -> Text(
                    "Pick a receipt photo. ML Kit will extract the merchant and amount.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ScanState.Scanning -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Reading receipt…")
                    }
                }
                is ScanState.Error -> Text(
                    "Error: ${s.message}",
                    color = MaterialTheme.colorScheme.error
                )
                is ScanState.Success -> ParsedReceiptForm(
                    receipt = s.receipt,
                    onSave = { merchant, amount, mandatory ->
                        vm.saveAsExpense(merchant, amount, mandatory, onSaved)
                    }
                )
            }
        }
    }
}

@Composable
private fun ParsedReceiptForm(
    receipt: com.example.moneymanager.data.receipt.ParsedReceipt,
    onSave: (String, Double, Boolean) -> Unit
) {
    var merchant by remember(receipt) { mutableStateOf(receipt.merchant.orEmpty()) }
    var amount by remember(receipt) { mutableStateOf(receipt.amount?.toString().orEmpty()) }
    var mandatory by remember { mutableStateOf(false) }

    Text("Extracted data", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(
        value = merchant,
        onValueChange = { merchant = it },
        label = { Text("Merchant") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = amount,
        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Amount (Rs.)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = mandatory, onCheckedChange = { mandatory = it })
        Spacer(Modifier.width(8.dp))
        Text("Mandatory expense")
    }

    Button(
        onClick = {
            val value = amount.toDoubleOrNull() ?: return@Button
            if (merchant.isBlank()) return@Button
            onSave(merchant.trim(), value, mandatory)
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Save as expense") }

    Spacer(Modifier.height(8.dp))
    Text("Raw text", style = MaterialTheme.typography.titleSmall)
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            receipt.rawText.take(1500),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}