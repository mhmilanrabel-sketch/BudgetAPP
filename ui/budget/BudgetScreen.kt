package com.example.moneymanager.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    vm: BudgetViewModel = viewModel()
) {
    val progress by vm.progress.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Monthly Budgets") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add budget")
            }
        }
    ) { pad ->
        if (progress.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text("No budgets yet. Tap + to set one.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(progress, key = { it.category }) { p ->
                    BudgetProgressCard(
                        p = p,
                        onDelete = { vm.removeBudget(p.category) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddBudgetDialog(
            onDismiss = { showAdd = false },
            onConfirm = { cat, limit ->
                vm.setLimit(cat, limit)
                showAdd = false
            }
        )
    }
}

@Composable
private fun BudgetProgressCard(
    p: CategoryBudgetProgress,
    onDelete: () -> Unit
) {
    val tint = remember(p.colorHex) {
        try { Color(android.graphics.Color.parseColor(p.colorHex)) }
        catch (_: Throwable) { Color(0xFF4CAF50) }
    }
    val overTint = Color(0xFFD32F2F)

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { p.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp)),
                color = if (p.isOverBudget) overTint else tint,
                trackColor = Color(0xFFEEEEEE)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Spent: Rs. ${"%,.2f".format(p.spent)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (p.isOverBudget)
                        "Over by Rs. ${"%,.2f".format(p.spent - p.limit)}"
                    else
                        "Left: Rs. ${"%,.2f".format(p.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.isOverBudget) overTint else Color(0xFF388E3C),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Limit: Rs. ${"%,.2f".format(p.limit)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category Budget") },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Grocery)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monthly limit (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = limit.toDoubleOrNull() ?: return@TextButton
                    if (category.isNotBlank() && amount > 0) onConfirm(category.trim(), amount)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}