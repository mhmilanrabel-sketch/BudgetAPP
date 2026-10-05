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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.moneymanager.data.local.entity.BudgetCategory
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.ui.expenses.ExpenseEditDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    onOpenExpenses: () -> Unit = {},
    vm: BudgetViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val editing by vm.editing.collectAsState()
    val dialogOpen by vm.dialogOpen.collectAsState()
    val focusedName by vm.focusedCategoryName.collectAsState()
    val editingExpense by vm.editingExpense.collectAsState()

    val focused = focusedName?.let { name ->
        state.items.firstOrNull { it.category.name == name }
    }

    if (focused != null) {
        CategoryDetailScreen(
            progress = focused,
            onBack = { vm.closeCategory() },
            onEditCategory = { vm.startEditing(focused.category) },
            onEditExpense = { vm.startEditingExpense(it) },
            onOpenExpensesTab = { vm.closeCategory(); onOpenExpenses() }
        )
        if (dialogOpen) {
            BudgetCategoryDialog(
                existing = editing,
                onDismiss = { vm.stopEditing() },
                onSave = { n, l, k, c -> vm.saveCategory(n, l, k, c) },
                onDelete = { vm.deleteCategory(it) }
            )
        }
        editingExpense?.let { line ->
            ExpenseEditDialog(
                existing = line,
                monthKey = line.monthKey,
                onDismiss = { vm.stopEditingExpense() },
                onSave = { vm.saveExpense(it) },
                onDelete = { vm.deleteExpense(it) }
            )
        }
        return
    }

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
                Text("Budget", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Tap a category to see matched expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
            if (state.items.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No categories yet",
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap + to add one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
                return@LazyColumn
            }
            item { OverallCard(state) }
            items(state.items, key = { it.category.id }) { p ->
                CategoryCard(p = p, onClick = { vm.openCategory(p.category.name) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (dialogOpen) {
        BudgetCategoryDialog(
            existing = editing,
            onDismiss = { vm.stopEditing() },
            onSave = { n, l, k, c -> vm.saveCategory(n, l, k, c) },
            onDelete = { vm.deleteCategory(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDetailScreen(
    progress: CategoryProgress,
    onBack: () -> Unit,
    onEditCategory: () -> Unit,
    onEditExpense: (ExpenseLine) -> Unit,
    onOpenExpensesTab: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(progress.category.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = onEditCategory) { Text("Edit") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val tint = try {
                Color(android.graphics.Color.parseColor(progress.category.colorHex))
            } catch (_: Throwable) { Color(0xFF4CAF50) }

            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (progress.isOverBudget)
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Bill this month", style = MaterialTheme.typography.labelMedium)
                    Text("Rs. ${"%,.2f".format(progress.spent)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Limit Rs. ${"%,.2f".format(progress.category.monthlyLimit)}",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = if (progress.isOverBudget) MaterialTheme.colorScheme.error else tint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Paid", style = MaterialTheme.typography.labelSmall)
                            Text("Rs. ${"%,.2f".format(progress.paid)}",
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF388E3C))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                            Text("Rs. ${"%,.2f".format(progress.outstanding)}",
                                fontWeight = FontWeight.SemiBold,
                                color = if (progress.outstanding > 0)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (progress.matched.isEmpty()) {
                Text("No expenses matched this category this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${progress.matched.size} items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                progress.matched.forEach { line ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onEditExpense(line) },
                        shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(line.itemName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium)
                                    Text(line.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Rs. ${"%,.2f".format(line.budgetAmount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("Paid Rs. ${"%,.2f".format(line.realPayAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (line.notPaidAmount > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("Remaining: Rs. ${"%,.2f".format(line.notPaidAmount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenExpensesTab,
                modifier = Modifier.fillMaxWidth()) {
                Text("Open Expenses tab")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverallCard(state: BudgetState) {
    val spent = state.totalSpent
    val limit = state.totalLimit
    val fraction = if (limit <= 0) 0f else (spent / limit).coerceIn(0.0, 1.0).toFloat()
    val over = spent > limit
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (over) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("Total this month", style = MaterialTheme.typography.labelMedium)
            Text("Rs. ${"%,.2f".format(spent)} / Rs. ${"%,.2f".format(limit)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp))
            LinearProgressIndicator(progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (over) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(if (over) "Over by Rs. ${"%,.2f".format(spent - limit)}"
                 else "Left: Rs. ${"%,.2f".format(limit - spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun CategoryCard(p: CategoryProgress, onClick: () -> Unit) {
    val tint = remember(p.category.colorHex) {
        try { Color(android.graphics.Color.parseColor(p.category.colorHex)) }
        catch (_: Throwable) { Color(0xFF4CAF50) }
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.category.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${p.percent}%", style = MaterialTheme.typography.labelLarge,
                    color = if (p.isOverBudget) MaterialTheme.colorScheme.error else tint,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { p.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (p.isOverBudget) MaterialTheme.colorScheme.error else tint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bill Rs. ${"%,.2f".format(p.spent)}",
                    style = MaterialTheme.typography.bodySmall)
                Text(if (p.isOverBudget)
                        "Over Rs. ${"%,.2f".format(p.spent - p.category.monthlyLimit)}"
                     else "Left Rs. ${"%,.2f".format(p.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.isOverBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(2.dp))
            Text("Paid Rs. ${"%,.2f".format(p.paid)}  ·  Outstanding Rs. ${"%,.2f".format(p.outstanding)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.matched.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(p.matched.joinToString(", ") { it.itemName },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun BudgetCategoryDialog(
    existing: BudgetCategory?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit,
    onDelete: (BudgetCategory) -> Unit
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
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Category name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = limitText,
                    onValueChange = { s -> limitText = s.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monthly limit (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = keywords, onValueChange = { keywords = it },
                    label = { Text("Keywords (comma-separated)") },
                    placeholder = { Text("electric,water,gas,wifi") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(6.dp))
                Text("Any expense whose item name contains one of these words counts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = limitText.toDoubleOrNull() ?: 0.0
                if (name.isBlank() || limit <= 0 || keywords.isBlank()) return@Button
                onSave(name, limit, keywords, existing?.colorHex ?: "#4CAF50")
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