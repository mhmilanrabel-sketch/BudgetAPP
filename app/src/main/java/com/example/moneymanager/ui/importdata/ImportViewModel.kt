package com.example.moneymanager.ui.importdata

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.core.util.CurrencyFormatter
import com.example.moneymanager.data.parser.BudgetSheetParser
import com.example.moneymanager.data.parser.EmptyPdfTextException
import com.example.moneymanager.data.parser.OcrFallbackParser
import com.example.moneymanager.data.parser.PaySlipParser
import com.example.moneymanager.data.parser.PaySlipValidationException
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.domain.model.ParsedBudgetSheet
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.example.moneymanager.domain.model.ReconciliationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data class Processing(val message: String) : ImportStatus()
    data class Success(val message: String, val reconciliation: ReconciliationResult) : ImportStatus()
    data class Error(val error: String) : ImportStatus()
    data class NeedsOcr(val message: String, val fileUri: Uri?) : ImportStatus()
}

class ImportViewModel(
    private val repository: MoneyManagerRepository,
    private val paySlipParser: PaySlipParser,
    private val budgetSheetParser: BudgetSheetParser,
    private val ocrFallbackParser: OcrFallbackParser
) : ViewModel() {

    private val _status = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val status: StateFlow<ImportStatus> = _status.asStateFlow()

    private val _selectedMonth = MutableStateFlow(CurrencyFormatter.getCurrentMonthKey())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    fun setMonth(monthKey: String) {
        _selectedMonth.value = monthKey
    }

    fun resetStatus() {
        _status.value = ImportStatus.Idle
    }

    /**
     * Parse Salary Slip PDF from Uri InputStream directly without writing to disk
     */
    fun importSalaryPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _status.value = ImportStatus.Processing("Parsing Sri Lankan Salary Slip PDF...")
            try {
                val slip = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Could not open PDF file stream.")
                    stream.use { s ->
                        paySlipParser.parse(s, _selectedMonth.value)
                    }
                }

                val recon = repository.saveSalarySlip(slip)
                _status.value = ImportStatus.Success(
                    "Salary slip for ${slip.monthKey} successfully parsed & mathematically verified! (Net: ${CurrencyFormatter.formatLkr(slip.netSalary)})",
                    recon
                )
            } catch (e: EmptyPdfTextException) {
                _status.value = ImportStatus.NeedsOcr(
                    "This PDF appears to be a scanned image or photo with no extractable text. Would you like to run on-device ML Kit OCR?",
                    uri
                )
            } catch (e: PaySlipValidationException) {
                _status.value = ImportStatus.Error("Validation Error: ${e.message}")
            } catch (e: Exception) {
                _status.value = ImportStatus.Error("Failed to parse PDF: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Run ML Kit OCR on a bitmap (scanned PDF page)
     */
    fun runOcrOnBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _status.value = ImportStatus.Processing("Running On-Device ML Kit OCR...")
            try {
                val slip = withContext(Dispatchers.Default) {
                    ocrFallbackParser.recognizeAndParseBitmap(bitmap, _selectedMonth.value)
                }
                val recon = repository.saveSalarySlip(slip)
                _status.value = ImportStatus.Success(
                    "OCR successfully parsed salary slip! (Net: ${CurrencyFormatter.formatLkr(slip.netSalary)})",
                    recon
                )
            } catch (e: Exception) {
                _status.value = ImportStatus.Error("OCR processing failed: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Parse Budget CSV / XLSX directly from InputStream
     */
    fun importBudgetSheet(context: Context, uri: Uri) {
        viewModelScope.launch {
            _status.value = ImportStatus.Processing("Reading Two-Column Budget Sheet...")
            try {
                val fileName = uri.lastPathSegment?.lowercase() ?: ""
                val isXlsx = fileName.endsWith(".xlsx") ||
                        (context.contentResolver.getType(uri)?.contains("spreadsheet") == true)

                val sheet = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Could not open budget file stream.")
                    stream.use { s ->
                        budgetSheetParser.parseInputStreamAuto(s, isXlsx, _selectedMonth.value)
                    }
                }

                val recon = repository.saveBudgetSheet(sheet)
                _status.value = ImportStatus.Success(
                    "Budget sheet for ${sheet.monthKey} parsed successfully! (${sheet.expenses.size} expenses detected, Opening Bank: ${CurrencyFormatter.formatLkr(sheet.openingBankBalance)})",
                    recon
                )
            } catch (e: Exception) {
                _status.value = ImportStatus.Error("Failed to parse budget sheet: ${e.localizedMessage ?: "Check format"}")
            }
        }
    }

    /**
     * Loads the authentic real-world sample data provided in the specification.
     * Perfect for instant verification, user demo, and automated flows.
     */
    fun loadRealisticSampleData() {
        viewModelScope.launch {
            _status.value = ImportStatus.Processing("Loading real-world Sri Lankan sample data...")
            try {
                val sampleMonth = _selectedMonth.value

                // 1. Realistic Salary Slip Data (Matches exact real PDF layout)
                val samplePdfText = """
                    Basic Salary                     192,455.33
                    Vehicle Allowance                 48,113.83
                    Exceptional Incentive              4,000.00
                    Shift Compensation Allow.         24,000.00
                    Gross Salary                     268,569.16
                    Total For EPF                    192,455.33
                    Total For ETF                    192,455.33
                    Total For TAX                    268,569.16
                    APIT                              11,342.45
                    LUMPSUMP TAX                           0.00
                    STAMP DUTY                             0.00
                    EPF Employee Cont.                15,396.43
                    Funeral Fund                         750.00
                    Excess Mobile Phone Usage         11,678.22
                    Meals -Deduction                     915.00
                    Total Deductions                  40,082.10
                    Net Salary                       228,487.06
                    Cash Salary                            0.00
                    Salary To Bank                   228,487.06
                    EPF Employer Cont.                23,094.64
                    ETF Employer Cont.                 5,773.66
                    APIT Employer.                         0.00
                    LUMPSUMP TAX Employer.                 0.00
                    STAMP DUTY Employer.                  25.00
                    Meals -Deduction- Rate 6.00 152.50
                """.trimIndent()

                val slip = paySlipParser.parseText(samplePdfText, sampleMonth)
                repository.saveSalarySlip(slip)

                // 2. Realistic Budget CSV Data (Matches exact real two-section CSV)
                val sampleCsv = """
                    Current Bank Rs,"Rs. 240,760.00",,,,,,,,,
                    Item,Amount,Not pay,Total real pay,,,,,,MONTHLY Fixed Expenses,Basic Salary,192,455.00
                    Rent/mortgage,"Rs. 25,000.00",,,,,,,,,Exceptional Incentive,4,000.00
                    Electric Chinna home,"Rs. 9,276.92",,,,,,,,,Vehicle Allowance,48,114.00
                    Water Chinna,"Rs. 5,570.24",,,,,,,,,Shift compensation,24,000.00
                    Cell phone / Wifi,"Rs. 1,801.00",,,,,,,,,Gross Salary,268,569.00
                    SLT Peo Tv / phone,"Rs. 9,383.76",,,,,,,,,APIT,11,342.00
                    Senaya Van,"Rs. 7,000.00",,,,,,,,,EPF,15,396.00
                    Geetha Van,"Rs. 6,000.00",,,,,,,,,Excess Mobile,0.00
                    Maiyon Van,"Rs. 12,000.00","Rs. 12,000.00",,,,,,,,Meals,1,071.00
                    Maiyon Fees,"Rs. 20,500.00",,,,,,,,,Total Deductions,27,809.00
                    Sanga,"Rs. 30,000.00",,,,,,,,,Sal,240,760.00
                    Mint Pay,"Rs. 13,825.98",,,,,,,,,Exspenses Total,200,158.00
                    Nipuna,"Rs. 30,000.00",,,,,,,,,Hand Save,40,602.00
                    Sachini,"Rs. 110,000.00","Rs. 100,000.00","Rs. 10,000.00",,,,,,Saving,25,000.00
                    Poli(08),"Rs. 7,500.00",,,,,,,,,Other,15,602.00
                    Washing M,"Rs. 60,600.00","Rs. 51,900.00","Rs. 8,700.00"
                    School Annual Fees,"Rs. 12,000.00","Rs. 6,000.00","Rs. 6,000.00"
                    Amma,"Rs. 10,000.00","Rs. 10,000.00",
                    Ramani Aunty Grocery,"Rs. 45,000.00","Rs. 27,705.00","Rs. 17,295.00"
                    Smk,"Rs. 8,300.00","Rs. 8,300.00",
                    Total,"Rs. 443,107.90","Rs. 215,905.00","Rs. 41,995.00"
                """.trimIndent()

                val sheet = budgetSheetParser.parseCsv(ByteArrayInputStream(sampleCsv.toByteArray()), sampleMonth)
                val recon = repository.saveBudgetSheet(sheet)

                _status.value = ImportStatus.Success(
                    "Sample Sri Lankan Salary Slip & Budget sheet imported successfully for $sampleMonth!",
                    recon
                )
            } catch (e: Exception) {
                _status.value = ImportStatus.Error("Failed to load sample data: ${e.message}")
            }
        }
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
                return ImportViewModel(
                    repository,
                    paySlipParser,
                    budgetSheetParser,
                    ocrFallbackParser
                ) as T
            }
        }
    }
}
