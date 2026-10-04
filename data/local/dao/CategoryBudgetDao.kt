package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moneymanager.data.local.entity.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: CategoryBudget): Long

    @Update
    suspend fun update(budget: CategoryBudget)

    @Delete
    suspend fun delete(budget: CategoryBudget)

    @Query("SELECT * FROM category_budgets WHERE month = :month ORDER BY category ASC")
    fun observeByMonth(month: String): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE month = :month")
    suspend fun getByMonth(month: String): List<CategoryBudget>

    @Query("DELETE FROM category_budgets WHERE month = :month AND category = :category")
    suspend fun deleteByCategory(month: String, category: String)
}