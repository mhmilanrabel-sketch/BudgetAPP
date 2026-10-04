package com.example.moneymanager.ui.receipt

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.receipt.ParsedReceipt
import com.example.moneymanager.data.receipt.ReceiptScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Success(val receipt: ParsedReceipt) : ScanState
    data class Error(val message: String) : ScanState
}

class ReceiptScanViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val currentMonth: String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    fun scan(uri: Uri) {
        _state.value = ScanState.Scanning
        viewModelScope.launch {
            try {
                val result = ReceiptScanner.scan(getApplication(), uri)
                _state.value = ScanState.Success(result)
            } catch (t: Throwable) {
                _state.value = ScanState.Error(t.message ?: "Scan failed")
            }
        }
    }

    fun saveAsExpense(
        merchant: String,
        amount: Double,
        isMandatory: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            db.expenseLineDao().insert(
                ExpenseLine(
                    month = currentMonth,
                    item = merchant.ifBlank { "Scanned receipt" },
                    amount = amount,
                    notPaid = 0.0,
                    realPay = amount,
                    isMandatory = isMandatory
                )
            )
            _state.value = ScanState.Idle
            onSaved()
        }
    }

    fun reset() { _state.value = ScanState.Idle }

    override fun onCleared() {
        super.onCleared()
        ReceiptScanner.close()
    }
}