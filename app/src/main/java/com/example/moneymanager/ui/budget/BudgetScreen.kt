package com.example.moneymanager.ui.budget

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    vm: BudgetViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val editing by vm.editing.collectAsState()
    val dialogOpen by vm.dialogOpen.collectAsState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startEditing(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New category") }
            )
        }
    ) { pad ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Budget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Spending limits per category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.items.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No categories yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap + to add one, e.g. \"Utilities\" with keywords \"electric,water,gas,wifi\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item { OverallCard(state) }

            items(state.items, key = { it.category.id }) { p ->
                CategoryCard(p = p, onClick = { vm.startEditing(p.category) })
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (dialogOpen) {
        BudgetCategoryDialog(
            existing = editing,
            onDismiss = { vm.stopEditing() },
            onSave = { name, limit, keywords, color ->
                vm.saveCategory(name, limit, keywords, color)
            },
            onDelete = { vm.deleteCategory(it) }
        )
    }
}

@Composable
private fun OverallCard(state: BudgetState) {
    val spent = state.totalSpent
    val limit = state.totalLimit
    val fraction = if (limit <= 0) 0f else (spent / limit).coerceIn(0.0, 1.0).toFloat()
    val over = spent > limit

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (over) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Total this month", style = MaterialTheme.typography.labelMedium)
            Text(
                "Rs. ${"%,.2f".format(spent)} / Rs. ${"%,.2f".format(limit)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (over) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (over) "Over by Rs. ${"%,.2f".format(spent - limit)}"
                else "Left: Rs. ${"%,.2f".format(limit - spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CategoryCard(p: CategoryProgress, onClick: () -> Unit) {
    val tint = remember(p.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(p.category.colorHex)) }
        catch (_: Throwable) { Color(0xFF4CAF50) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${p.percent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (p.isOverBudget) MaterialTheme.colorScheme.error else tint,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { p.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (p.isOverBudget) MaterialTheme.colorScheme.error else tint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Rs. ${"%,.2f".format(p.spent)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (p.isOverBudget) "Over by Rs. ${"%,.2f".format(p.spent - p.category.monthlyLimit)}"
                    else "Left Rs. ${"%,.2f".format(p.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.isOverBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            if (p.matchedItems.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    p.matchedItems.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun BudgetCategoryDialog(
    existing: com.example.moneymanager.data.local.entity.BudgetCategory?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit,
    onDelete: (com.example.moneymanager.data.local.entity.BudgetCategory) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var limitText by remember {
        mutableStateOf(existing?.monthlyLimit?.takeIf { it > 0 }?.toString() ?: "")
    }
    var keywords by remember { mutableStateOf(existing?.keywordsCsv ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New category" else "Edit category") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { s -> limitText = s.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monthly limit (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma-separated)") },
                    placeholder = { Text("electric,water,gas,wifi") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Any expense whose item name contains one of these words counts toward this category.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = limitText.toDoubleOrNull() ?: 0.0
                if (name.isBlank() || limit <= 0 || keywords.isBlank()) return@Button
                onSave(name, limit, keywords, "#4CAF50")
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                             modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}