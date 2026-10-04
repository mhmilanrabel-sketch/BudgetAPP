package com.example.moneymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Overview", Icons.Default.AccountBalance)
    data object Import : Screen("import", "Import", Icons.Default.FileUpload)
    data object Expenses : Screen("expenses", "Expenses", Icons.Default.ReceiptLong)
    data object Salary : Screen("salary", "Salary", Icons.Default.Payments)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Import,
                Expenses,
                Salary,
                History,
                Settings
            )
    }
}
