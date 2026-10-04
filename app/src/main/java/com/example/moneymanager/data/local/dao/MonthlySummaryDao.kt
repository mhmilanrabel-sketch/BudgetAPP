package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneymanager.data.local.entity.MonthlySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySummaryDao {

    @Query("SELECT * FROM monthly_summaries WHERE monthKey = :monthKey")
    fun getByMonth(monthKey: String): Flow<MonthlySummary?>

    @Query("SELECT * FROM monthly_summaries WHERE monthKey = :monthKey")
    suspend fun getByMonthSync(monthKey: String): MonthlySummary?

    @Query("SELECT * FROM monthly_summaries ORDER BY monthKey DESC LIMIT 12")
    fun getLast12Months(): Flow<List<MonthlySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: MonthlySummary)

    @Query("DELETE FROM monthly_summaries WHERE monthKey = :monthKey")
    suspend fun deleteForMonth(monthKey: String)
}
