package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salary_records",
    indices = [Index(value = ["month"], unique = true)]
)
data class SalaryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    val basicSalary: Double,
    val vehicleAllowance: Double,
    val exceptionalIncentive: Double,
    val shiftCompensation: Double,
    val grossSalary: Double,
    val totalForEpf: Double,
    val totalForEtf: Double,
    val totalForTax: Double,
    val apit: Double,
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
    val rawText: String = "",
    val importedAt: Long = System.currentTimeMillis()
)