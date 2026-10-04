package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SalaryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SalaryRecord): Long

    @Query("SELECT * FROM salary_records WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): SalaryRecord?

    @Query("SELECT * FROM salary_records WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<SalaryRecord?>

    @Query("SELECT * FROM salary_records ORDER BY month DESC")
    fun observeAll(): Flow<List<SalaryRecord>>

    @Query("SELECT * FROM salary_records ORDER BY month DESC LIMIT 12")
    fun observeLast12Months(): Flow<List<SalaryRecord>>

    @Delete
    suspend fun delete(record: SalaryRecord)

    @Query("DELETE FROM salary_records WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM salary_records")
    suspend fun deleteAll()
}