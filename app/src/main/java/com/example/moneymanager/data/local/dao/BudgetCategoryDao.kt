package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneymanager.data.local.entity.BudgetCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {

    @Query("SELECT * FROM budget_categories ORDER BY name ASC")
    fun getAll(): Flow<List<BudgetCategory>>

    @Query("SELECT * FROM budget_categories ORDER BY name ASC")
    suspend fun getAllSync(): List<BudgetCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cat: BudgetCategory): Long

    @Delete
    suspend fun delete(cat: BudgetCategory)

    @Query("DELETE FROM budget_categories")
    suspend fun clear()
}