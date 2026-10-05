package com.example.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.moneymanager.ui.dashboard.DashboardScreen
import com.example.moneymanager.ui.expenses.ExpenseListScreen
import com.example.moneymanager.ui.imports.ImportScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    NavShell()
                }
            }
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun NavShell() {
    val tabs = listOf(
        Tab("dashboard", "Home", Icons.Default.Home),
        Tab("expenses", "Expenses", Icons.Default.List),
        Tab("imports", "Import", Icons.Default.UploadFile),
        Tab("budget", "Budget", Icons.Default.PieChart),
        Tab("receipt", "Scan", Icons.Default.Scanner)
    )
    var current by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { current = tab.route },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (current) {
                "dashboard" -> DashboardScreen()
                "expenses"  -> ExpenseListScreen()
                "imports"   -> ImportScreen(
                    onImported = { current = "expenses" }
                )
                "budget"    -> CenterText("Budget")
                "receipt"   -> CenterText("Scan receipt")
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}