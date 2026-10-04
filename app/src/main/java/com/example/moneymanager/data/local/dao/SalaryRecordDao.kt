package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneymanager.data.local.entity.SalaryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryRecordDao {

    @Query("SELECT * FROM salary_records WHERE monthKey = :monthKey")
    fun getByMonth(monthKey: String): Flow<SalaryRecord?>

    @Query("SELECT * FROM salary_records WHERE monthKey = :monthKey")
    suspend fun getByMonthSync(monthKey: String): SalaryRecord?

    @Query("SELECT * FROM salary_records ORDER BY monthKey DESC")
    fun getAllSalaryRecords(): Flow<List<SalaryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: SalaryRecord)

    @Query("DELETE FROM salary_records WHERE monthKey = :monthKey")
    suspend fun deleteForMonth(monthKey: String)
}
