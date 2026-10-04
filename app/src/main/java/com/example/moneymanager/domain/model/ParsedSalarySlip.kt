package com.example.moneymanager.domain.model

data class ParsedSalarySlip(
    val monthKey: String, // "YYYY-MM"
    val basicSalary: Double,
    val vehicleAllowance: Double,
    val exceptionalIncentive: Double,
    val shiftCompensation: Double,
    val grossSalary: Double,
    val totalForEpf: Double,
    val totalForEtf: Double,
    val totalForTax: Double,
    val apit: Double,
    val lumpsumTax: Double,
    val stampDuty: Double,
    val epfEmployee: Double,
    val funeralFund: Double,
    val excessMobile: Double,
    val mealsDeduction: Double,
    val totalDeductions: Double,
    val netSalary: Double,
    val cashSalary: Double,
    val salaryToBank: Double,
    val epfEmployer: Double,
    val etfEmployer: Double,
    val stampDutyEmployer: Double,
    val rawText: String = "",
    val isOcrFallback: Boolean = false
)
