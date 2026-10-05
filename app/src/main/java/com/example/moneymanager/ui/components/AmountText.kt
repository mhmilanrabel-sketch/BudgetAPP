package com.example.moneymanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.moneymanager.core.privacy.PrivacyState

const val HIDDEN_AMOUNT = "Rs. ••••••"

fun formatRsRaw(amount: Double): String = "Rs. " + "%,.2f".format(amount)

/** Renders the amount, or "Rs. ••••••" if privacy mode is on. */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    Text(
        text = if (PrivacyState.isPrivate) HIDDEN_AMOUNT else formatRsRaw(amount),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

/** Eye toggle — place in a TopAppBar or header row. */
@Composable
fun PrivacyToggle() {
    IconButton(onClick = { PrivacyState.toggle() }) {
        Icon(
            imageVector = if (PrivacyState.isPrivate) Icons.Default.VisibilityOff
                          else Icons.Default.Visibility,
            contentDescription = if (PrivacyState.isPrivate) "Show amounts" else "Hide amounts"
        )
    }
}