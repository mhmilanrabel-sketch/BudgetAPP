package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneymanager.data.local.entity.BudgetMonth
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetMonthDao {

    @Query("SELECT * FROM budget_months ORDER BY monthKey DESC")
    fun getAllMonths(): Flow<List<BudgetMonth>>

    @Query("SELECT * FROM budget_months WHERE monthKey = :monthKey")
    fun getByMonth(monthKey: String): Flow<BudgetMonth?>

    @Query("SELECT * FROM budget_months WHERE monthKey = :monthKey")
    suspend fun getByMonthSync(monthKey: String): BudgetMonth?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(month: BudgetMonth)

    @Query("DELETE FROM budget_months WHERE monthKey = :monthKey")
    suspend fun deleteMonth(monthKey: String)
}
