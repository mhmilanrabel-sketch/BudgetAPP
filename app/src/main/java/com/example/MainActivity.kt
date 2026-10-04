package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.ui.dashboard.DashboardScreen
import com.example.moneymanager.ui.dashboard.DashboardViewModel
import com.example.moneymanager.ui.expenses.ExpenseListScreen
import com.example.moneymanager.ui.history.HistoryScreen
import com.example.moneymanager.ui.history.HistoryViewModel
import com.example.moneymanager.ui.importdata.ImportScreen
import com.example.moneymanager.ui.importdata.ImportViewModel
import com.example.moneymanager.ui.navigation.Screen
import com.example.moneymanager.ui.salary.SalaryBreakdownScreen
import com.example.moneymanager.ui.settings.SettingsScreen
import com.example.ui.theme.MoneyManagerTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security: Prevent screenshots, recordings, and recent app previews of financial balances
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        val app = application as MoneyManagerApplication
        val container = app.container

        setContent {
            MoneyManagerTheme {
                val biometricManager = remember { container.biometricAuthManager }
                var isUnlocked by remember {
                    mutableStateOf(!biometricManager.isBiometricEnabled)
                }

                LaunchedEffect(Unit) {
                    if (biometricManager.isBiometricEnabled && !isUnlocked) {
                        biometricManager.promptBiometric(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { /* Keep locked */ }
                        )
                    }
                }

                if (!isUnlocked) {
                    BiometricLockScreen(
                        onUnlockClicked = {
                            biometricManager.promptBiometric(
                                activity = this@MainActivity,
                                onSuccess = { isUnlocked = true },
                                onError = { /* Keep locked */ }
                            )
                        }
                    )
                } else {
                    MoneyManagerAppScaffold(app = app)
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("biometric_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Locked",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "MoneyManager LK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Confidential financial data protected by hardware-backed security.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = onUnlockClicked,
                    modifier = Modifier.testTag("unlock_biometric_button")
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Unlock with Biometrics")
                }
            }
        }
    }
}

@Composable
fun MoneyManagerAppScaffold(app: MoneyManagerApplication) {
    val container = app.container

    val dashboardViewModel: DashboardViewModel = remember {
        DashboardViewModel.provideFactory(container.repository)
            .create(DashboardViewModel::class.java)
    }

    val importViewModel: ImportViewModel = remember {
        ImportViewModel.provideFactory(
            container.repository,
            container.paySlipParser,
            container.budgetSheetParser,
            container.ocrFallbackParser
        ).create(ImportViewModel::class.java)
    }

    val historyViewModel: HistoryViewModel = remember {
        HistoryViewModel.provideFactory(container.repository)
            .create(HistoryViewModel::class.java)
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var activeMonthKey by remember { mutableStateOf(dashboardViewModel.selectedMonth.value) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToImport = { currentScreen = Screen.Import },
                        onNavigateToExpenses = { currentScreen = Screen.Expenses },
                        onNavigateToSalary = { currentScreen = Screen.Salary }
                    )
                }
                Screen.Import -> {
                    ImportScreen(
                        viewModel = importViewModel,
                        onNavigateToDashboard = {
                            currentScreen = Screen.Dashboard
                            dashboardViewModel.refresh()
                        }
                    )
                }
                Screen.Expenses -> {
                    ExpenseListScreen(
                        repository = container.repository,
                        selectedMonthKey = activeMonthKey,
                        onMonthSelected = {
                            activeMonthKey = it
                            dashboardViewModel.selectMonth(it)
                        }
                    )
                }
                Screen.Salary -> {
                    SalaryBreakdownScreen(
                        repository = container.repository,
                        selectedMonthKey = activeMonthKey,
                        onMonthSelected = {
                            activeMonthKey = it
                            dashboardViewModel.selectMonth(it)
                        }
                    )
                }
                Screen.History -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onSelectMonth = { monthKey ->
                            activeMonthKey = monthKey
                            dashboardViewModel.selectMonth(monthKey)
                            currentScreen = Screen.Dashboard
                        }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        biometricAuthManager = container.biometricAuthManager,
                        budgetSheetParser = container.budgetSheetParser
                    )
                }
            }
        }
    }
}
