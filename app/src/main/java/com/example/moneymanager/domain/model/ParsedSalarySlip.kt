package com.example.moneymanager.domain.model

data class ParsedSalarySlip(
    val monthKey: String,
    val basicSalary: Double,
    val vehicleAllowance: Double = 0.0,
    val exceptionalIncentive: Double = 0.0,
    val shiftCompensation: Double = 0.0,
    val grossSalary: Double,
    val totalForEpf: Double = 0.0,
    val totalForEtf: Double = 0.0,
    val totalForTax: Double = 0.0,
    val apit: Double = 0.0,
    val lumpsumTax: Double = 0.0,
    val stampDuty: Double = 0.0,
    val epfEmployee: Double = 0.0,
    val funeralFund: Double = 0.0,
    val excessMobile: Double = 0.0,
    val mealsDeduction: Double = 0.0,
    val totalDeductions: Double,
    val netSalary: Double,
    val cashSalary: Double = 0.0,
    val salaryToBank: Double = 0.0,
    val epfEmployer: Double = 0.0,
    val etfEmployer: Double = 0.0,
    val stampDutyEmployer: Double = 0.0,
    val allLineItems: Map<String, Double> = emptyMap(),
    val rawTextExtracted: Boolean = true
)
