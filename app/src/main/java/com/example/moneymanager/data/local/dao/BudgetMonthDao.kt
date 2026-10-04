package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BudgetMonth
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetMonthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetMonth)

    @Query("SELECT * FROM budget_months WHERE month = :month LIMIT 1")
    suspend fun getByMonth(month: String): BudgetMonth?

    @Query("SELECT * FROM budget_months WHERE month = :month LIMIT 1")
    fun observeByMonth(month: String): Flow<BudgetMonth?>

    @Query("SELECT * FROM budget_months ORDER BY month DESC")
    fun observeAll(): Flow<List<BudgetMonth>>

    @Query("DELETE FROM budget_months WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM budget_months")
    suspend fun deleteAll()
}