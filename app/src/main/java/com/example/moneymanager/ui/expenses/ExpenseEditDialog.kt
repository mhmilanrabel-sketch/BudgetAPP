package com.example.moneymanager.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moneymanager.data.local.entity.ExpenseLine

@Composable
fun ExpenseEditDialog(
    existing: ExpenseLine?,
    monthKey: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseLine) -> Unit,
    onDelete: ((ExpenseLine) -> Unit)? = null
) {
    var itemName by remember { mutableStateOf(existing?.itemName ?: "") }
    var amountText by remember {
        mutableStateOf(existing?.budgetAmount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var paidText by remember {
        mutableStateOf(existing?.realPayAmount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var isFixed by remember { mutableStateOf(existing?.category == "Fixed") }
    var isMandatory by remember { mutableStateOf(existing?.isMandatory ?: true) }

    var status by remember {
        mutableStateOf(
            when {
                existing == null -> PaymentStatus.PAID
                existing.notPaidAmount <= 0.0 -> PaymentStatus.PAID
                existing.realPayAmount <= 0.0 -> PaymentStatus.NOT_PAID
                else -> PaymentStatus.PARTIAL
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add expense" else "Edit expense") },
        text = {
            Column(Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { s ->
                        amountText = s.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Bill amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Payment status",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == PaymentStatus.PAID,
                        onClick = { status = PaymentStatus.PAID },
                        label = { Text("Paid") }
                    )
                    FilterChip(
                        selected = status == PaymentStatus.PARTIAL,
                        onClick = { status = PaymentStatus.PARTIAL },
                        label = { Text("Partial") }
                    )
                    FilterChip(
                        selected = status == PaymentStatus.NOT_PAID,
                        onClick = { status = PaymentStatus.NOT_PAID },
                        label = { Text("Not paid") }
                    )
                }

                if (status == PaymentStatus.PARTIAL) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { s ->
                            paidText = s.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Amount paid (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fixed", Modifier.width(70.dp))
                    Switch(checked = isFixed, onCheckedChange = { isFixed = it })
                    Spacer(Modifier.width(16.dp))
                    Text("Mandatory", Modifier.width(90.dp))
                    Switch(checked = isMandatory, onCheckedChange = { isMandatory = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bill = amountText.toDoubleOrNull() ?: 0.0
                    val paid = when (status) {
                        PaymentStatus.PAID -> bill
                        PaymentStatus.NOT_PAID -> 0.0
                        PaymentStatus.PARTIAL -> paidText.toDoubleOrNull() ?: 0.0
                    }
                    val notPaid = (bill - paid).coerceAtLeast(0.0)
                    if (itemName.isBlank()) return@Button
                    val saved = ExpenseLine(
                        id = existing?.id ?: 0,
                        monthKey = monthKey,
                        itemName = itemName.trim(),
                        budgetAmount = bill,
                        notPaidAmount = notPaid,
                        realPayAmount = paid,
                        isMandatory = isMandatory,
                        category = if (isFixed) "Fixed" else "Variable"
                    )
                    onSave(saved)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(existing) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}