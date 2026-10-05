package com.example.moneymanager.ui.expenses

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.ui.dashboard.monthLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    modifier: Modifier = Modifier,
    vm: ExpensesViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val selectedMonth by vm.selectedMonth.collectAsState()
    val allMonths by vm.allMonths.collectAsState()
    val templateCount by vm.templateCount.collectAsState()
    val carryForward by vm.carryForward.collectAsState()
    val ctx = LocalContext.current

    var editing by remember { mutableStateOf<ExpenseLine?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth, state.loading) {
        if (!state.loading && state.mandatory.isEmpty() && state.optional.isEmpty()) {
            vm.ensureSeeded(selectedMonth)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save month as template") },
                            onClick = { menuOpen = false; showSaveTemplateDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear template") },
                            onClick = {
                                menuOpen = false
                                vm.clearTemplate()
                                Toast.makeText(ctx, "Template cleared", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clip.primaryClip?.getItemAt(0)?.text?.toString()
                        if (text.isNullOrBlank()) {
                            Toast.makeText(ctx, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.pasteFromClipboard(text) { count ->
                                Toast.makeText(ctx,
                                    if (count == 0) "Nothing new to paste" else "Pasted $count items",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Icon(Icons.Default.ContentPaste, contentDescription = "Paste") }
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = { showNewDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add") }
                )
            }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allMonths, key = { it }) { month ->
                        FilterChip(
                            selected = month == selectedMonth,
                            onClick = { vm.selectMonth(month) },
                            label = { Text(monthLabel(month)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── BRING FORWARD SECTION ──────────────────────────
            if (carryForward.isNotEmpty()) {
                item {
                    CarryForwardCard(
                        items = carryForward,
                        onAddOne = { item ->
                            vm.addCarryForwardItem(item) {
                                Toast.makeText(ctx, "Added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddAll = {
                            vm.addAllCarryForward { count ->
                                Toast.makeText(ctx, "Added $count items", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = { vm.dismissCarryForward() }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (state.mandatory.isEmpty() && state.optional.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No expenses for ${monthLabel(selectedMonth)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            if (templateCount == 0) {
                                Text(
                                    "Tip: tap ⋮ → Save month as template " +
                                    "so future months auto-fill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Loading ${templateCount} items from template…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            item { TotalsCard(state) }

            if (state.mandatory.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Mandatory — ${state.mandatory.size} items")
                }
                items(state.mandatory, key = { it.id }) { line ->
                    ExpenseRow(line) { editing = it }
                }
            }

            if (state.optional.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Optional — ${state.optional.size} items")
                }
                items(state.optional, key = { it.id }) { line ->
                    ExpenseRow(line) { editing = it }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showNewDialog) {
        ExpenseEditDialog(
            existing = null,
            monthKey = selectedMonth,
            onDismiss = { showNewDialog = false },
            onSave = { vm.addOrUpdate(it); showNewDialog = false }
        )
    }

    editing?.let { current ->
        ExpenseEditDialog(
            existing = current,
            monthKey = selectedMonth,
            onDismiss = { editing = null },
            onSave = { vm.addOrUpdate(it); editing = null },
            onDelete = { vm.delete(it); editing = null }
        )
    }

    if (showSaveTemplateDialog) {
        val count = state.mandatory.size + state.optional.size
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save as template") },
            text = {
                Text(
                    "Copy all $count items from ${monthLabel(selectedMonth)} " +
                    "into the master template?\n\n" +
                    "Every new month will auto-fill from this template."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveCurrentAsTemplate { savedCount ->
                        Toast.makeText(ctx,
                            "Template saved ($savedCount items)",
                            Toast.LENGTH_SHORT).show()
                    }
                    showSaveTemplateDialog = false
                }) { Text("Save template") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Carry forward card ─────────────────────────────────────────
@Composable
private fun CarryForwardCard(
    items: List<CarryForwardItem>,
    onAddOne: (CarryForwardItem) -> Unit,
    onAddAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = items.sumOf { it.unpaidAmount }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Bring Forward",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Unpaid from ${monthLabel(items.first().fromMonth)} " +
                        "— Rs. ${"%,.2f".format(total)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.itemName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Rs. ${"%,.2f".format(item.unpaidAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = { onAddOne(item) }) { Text("Add") }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAddAll, modifier = Modifier.fillMaxWidth()) {
                Text("Add all to ${monthLabel(items.first().fromMonth.let { "current" })} month")
            }
        }
    }
}

@Composable
private fun TotalsCard(state: ExpensesState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bill Total", style = MaterialTheme.typography.labelMedium)
                Text(formatRs(state.grandTotal), fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paid", style = MaterialTheme.typography.labelMedium)
                Text(formatRs(state.paidTotal), fontWeight = FontWeight.SemiBold,
                     color = Color(0xFF388E3C))
            }
            if (state.notPaidTotal > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Remaining", style = MaterialTheme.typography.labelMedium)
                    Text(formatRs(state.notPaidTotal), fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Mandatory: ${formatRs(state.mandatoryTotal)} · " +
                "Optional: ${formatRs(state.optionalTotal)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ExpenseRow(line: ExpenseLine, onClick: (ExpenseLine) -> Unit) {
    val isCarried = line.category == ExpensesViewModel.CARRIED_CATEGORY_TAG
    val statusColor = when {
        line.notPaidAmount <= 0.0 -> Color(0xFF388E3C)         // Paid — green
        line.realPayAmount <= 0.0 -> Color(0xFFD32F2F)         // Not paid — red
        else                      -> Color(0xFFF57C00)         // Partial — orange
    }
    val statusText = when {
        line.notPaidAmount <= 0.0 -> "Paid"
        line.realPayAmount <= 0.0 -> "Not paid"
        else                      -> "Partial"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(line) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCarried)
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.itemName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        Text(
                            line.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .background(statusColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatRs(line.budgetAmount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Paid: ${formatRs(line.realPayAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (line.notPaidAmount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Remaining: ${formatRs(line.notPaidAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatRs(amount: Double): String = "Rs. " + "%,.2f".format(amount)