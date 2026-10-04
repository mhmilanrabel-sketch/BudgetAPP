package com.example.moneymanager.di

import android.content.Context
import com.example.moneymanager.core.security.BiometricAuthManager
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.parser.BudgetSheetParser
import com.example.moneymanager.data.parser.OcrFallbackParser
import com.example.moneymanager.data.parser.PaySlipParser
import com.example.moneymanager.data.repository.MoneyManagerRepository
import com.example.moneymanager.data.repository.MoneyManagerRepositoryImpl
import com.example.moneymanager.domain.usecase.ReconcileMonthlyFinancesUseCase

interface AppContainer {
    val database: AppDatabase
    val repository: MoneyManagerRepository
    val paySlipParser: PaySlipParser
    val budgetSheetParser: BudgetSheetParser
    val ocrFallbackParser: OcrFallbackParser
    val biometricAuthManager: BiometricAuthManager
    val reconcileUseCase: ReconcileMonthlyFinancesUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val reconcileUseCase: ReconcileMonthlyFinancesUseCase by lazy {
        ReconcileMonthlyFinancesUseCase()
    }

    override val repository: MoneyManagerRepository by lazy {
        MoneyManagerRepositoryImpl(database, reconcileUseCase)
    }

    override val paySlipParser: PaySlipParser by lazy {
        PaySlipParser(context)
    }

    override val budgetSheetParser: BudgetSheetParser by lazy {
        BudgetSheetParser(context)
    }

    override val ocrFallbackParser: OcrFallbackParser by lazy {
        OcrFallbackParser(context, paySlipParser)
    }

    override val biometricAuthManager: BiometricAuthManager by lazy {
        BiometricAuthManager(context)
    }
}
