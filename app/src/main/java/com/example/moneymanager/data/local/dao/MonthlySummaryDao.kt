package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MonthlySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: MonthlySummary)

    @Query("SELECT * FROM monthly_summaries WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): MonthlySummary?

    @Query("SELECT * FROM monthly_summaries WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<MonthlySummary?>

    @Query("SELECT * FROM monthly_summaries ORDER BY month DESC LIMIT 12")
    fun observeLast12Months(): Flow<List<MonthlySummary>>

    @Query("SELECT * FROM monthly_summaries ORDER BY month DESC")
    fun observeAll(): Flow<List<MonthlySummary>>

    @Query("DELETE FROM monthly_summaries WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM monthly_summaries")
    suspend fun deleteAll()
}