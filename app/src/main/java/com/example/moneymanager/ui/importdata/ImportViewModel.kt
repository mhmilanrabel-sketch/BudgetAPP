package com.example.moneymanager.ui.importdata

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.parser.BudgetSheetParser
import com.example.moneymanager.data.parser.OcrFallbackParser
import com.example.moneymanager.data.parser.PaySlipParser
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportState {
    data object Idle : ImportState()
    data class Processing(val message: String) : ImportState()
    data class SalarySlipSuccess(val slip: ParsedSalarySlip) : ImportState()
    data class BudgetSheetSuccess(val sheet: ParsedBudgetSheet) : ImportState()
    data class Error(val message: String, val canFallbackToOcr: Boolean = false) : ImportState()
}

class ImportViewModel(
    private val repository: MoneyManagerRepository,
    private val paySlipParser: PaySlipParser,
    private val budgetSheetParser: BudgetSheetParser,
    private val ocrFallbackParser: OcrFallbackParser
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun parsePaySlipPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Processing("Reading and decrypting Salary Slip PDF in-memory...")
            try {
                val slip = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Could not open file stream")
                    stream.use { paySlipParser.parse(it) }
                }
                repository.saveSalarySlip(slip)
                _importState.value = ImportState.SalarySlipSuccess(slip)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unknown parsing error"
                val isTextMissing = msg.contains("no extractable text", ignoreCase = true)
                _importState.value = ImportState.Error(
                    message = "Payslip Parsing Failed: $msg",
                    canFallbackToOcr = isTextMissing
                )
            }
        }
    }

    fun parsePaySlipOcr(bitmap: Bitmap) {
        viewModelScope.launch {
            _importState.value = ImportState.Processing("Running On-Device ML Kit OCR on payslip image...")
            try {
                val slip = withContext(Dispatchers.IO) {
                    ocrFallbackParser.recognizeAndParse(bitmap)
                }
                repository.saveSalarySlip(slip)
                _importState.value = ImportState.SalarySlipSuccess(slip)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    message = "OCR Payslip Parsing Failed: ${e.localizedMessage ?: "Could not recognize text"}",
                    canFallbackToOcr = false
                )
            }
        }
    }

    fun parseBudgetSheet(context: Context, uri: Uri, isExcel: Boolean) {
        viewModelScope.launch {
            val formatName = if (isExcel) "Excel (.xlsx)" else "CSV"
            _importState.value = ImportState.Processing("Processing two-section $formatName budget sheet...")
            try {
                val sheet = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Could not open file stream")
                    stream.use { budgetSheetParser.parse(it, isExcel) }
                }
                repository.saveBudgetSheet(sheet)
                _importState.value = ImportState.BudgetSheetSuccess(sheet)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    message = "Budget Sheet Parsing Failed: ${e.localizedMessage ?: "Failed to read rows"}"
                )
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            _importState.value = ImportState.Processing("Loading authentic sample Sri Lankan financial dataset...")
            try {
                // Real-world sample budget CSV text matching the prompt's layout
                val sampleCsv = """
Current Bank Rs,3500.00,,,,,,,,,,
Item,Amount,Not pay,Total real pay,,,,,,MONTHLY Fixed Expenses,Basic Salary,"192,455.00"
House Rent,45000.00,0,45000.00,,,,,,Fixed,Vehicle Allowance,"48,114.00"
Electricity Bill CEB,12500.00,0,12500.00,,,,,,Fixed,Exceptional Incentive,"4,000.00"
Water Supply Bill,2400.00,0,2400.00,,,,,,Fixed,Shift Compensation,"2,000.00"
SLT Fiber Internet,5800.00,0,5800.00,,,,,,Fixed,Gross Salary,"246,569.00"
Dialog Mobile Postpaid,3500.00,0,3500.00,,,,,,Fixed,APIT,"18,450.00"
Supermarket Groceries,38000.00,0,38000.00,,,,,,Fixed,EPF,"15,396.40"
Mother Medical & Medicine,15000.00,0,15000.00,,,,,,Fixed,Funeral Fund,"500.00"
Fuel Petrol Allowance,22000.00,0,22000.00,,,,,,Fixed,Meals,"4,800.00"
Motorbike Lease Installment,18500.00,0,18500.00,,,,,,Fixed,Total Deductions,"39,146.40"
Personal Loan Commercial Bank,28000.00,0,28000.00,,,,,,Fixed,Sal,"207,422.60"
Credit Card Payment,35000.00,15000.00,20000.00,,,,,,Fixed,Saving,"25,000.00"
Car Insurance Premium,14000.00,14000.00,0.00,,,,,,Fixed,Hand Save,"15,000.00"
Gym Membership,4500.00,0,4500.00,,,,,,Discretionary,Exspenses Total,"216,700.00"
Dining Out & Weekend,12000.00,0,12000.00,,,,,,Discretionary,Other,"10,000.00"
Clothing & Shopping,8500.00,8500.00,0.00,,,,,,Discretionary,,
Online Subscriptions,3200.00,0,3200.00,,,,,,Discretionary,,
Exspenses Total,254900.00,37500.00,217400.00,,,,,,,,
                """.trimIndent()

                val sheet = withContext(Dispatchers.IO) {
                    budgetSheetParser.parse(sampleCsv.byteInputStream(), isExcel = false, monthKeyFallback = "2026-10")
                }
                repository.saveBudgetSheet(sheet)

                // Match with a compliant parsed payslip (Gross - Deductions = Net)
                // Gross 246569.00 - Deductions 39146.40 = 207422.60
                val sampleSlip = ParsedSalarySlip(
                    monthKey = "2026-10",
                    basicSalary = 192455.00,
                    vehicleAllowance = 48114.00,
                    exceptionalIncentive = 4000.00,
                    shiftCompensation = 2000.00,
                    grossSalary = 246569.00,
                    totalForEpf = 192455.00,
                    totalForEtf = 192455.00,
                    totalForTax = 246569.00,
                    apit = 18450.00,
                    lumpsumTax = 0.0,
                    stampDuty = 0.0,
                    epfEmployee = 15396.40,
                    funeralFund = 500.00,
                    excessMobile = 0.0,
                    mealsDeduction = 4800.00,
                    totalDeductions = 39146.40,
                    netSalary = 207422.60,
                    cashSalary = 0.0,
                    salaryToBank = 207422.60,
                    epfEmployer = 23094.60,
                    etfEmployer = 5773.65,
                    stampDutyEmployer = 0.0,
                    rawText = "Sample Authentic Slip"
                )
                repository.saveSalarySlip(sampleSlip)

                _importState.value = ImportState.BudgetSheetSuccess(sheet)
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Sample load failed: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    companion object {
        fun provideFactory(
            repository: MoneyManagerRepository,
            paySlipParser: PaySlipParser,
            budgetSheetParser: BudgetSheetParser,
            ocrFallbackParser: OcrFallbackParser
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImportViewModel(repository, paySlipParser, budgetSheetParser, ocrFallbackParser) as T
            }
        }
    }
}
