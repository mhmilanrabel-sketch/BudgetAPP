package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salary_records")
data class SalaryRecord(
    @PrimaryKey
    val monthKey: String, // e.g. "2026-10"
    val basicSalary: Double = 0.0,
    val vehicleAllowance: Double = 0.0,
    val exceptionalIncentive: Double = 0.0,
    val shiftCompensation: Double = 0.0,
    val grossSalary: Double = 0.0,
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
    val totalDeductions: Double = 0.0,
    val netSalary: Double = 0.0,
    val cashSalary: Double = 0.0,
    val salaryToBank: Double = 0.0,
    val epfEmployer: Double = 0.0,
    val etfEmployer: Double = 0.0,
    val stampDutyEmployer: Double = 0.0,
    val rawText: String = "",
    val importedAt: Long = System.currentTimeMillis()
)
