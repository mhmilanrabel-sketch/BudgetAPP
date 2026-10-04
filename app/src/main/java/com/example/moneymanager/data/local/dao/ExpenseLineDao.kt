package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moneymanager.data.local.entity.ExpenseLine
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseLineDao {

    @Query("SELECT * FROM expense_lines WHERE monthKey = :monthKey ORDER BY isMandatory DESC, itemName ASC")
    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseLine>>

    @Query("SELECT * FROM expense_lines WHERE monthKey = :monthKey ORDER BY isMandatory DESC, itemName ASC")
    suspend fun getExpensesForMonthSync(monthKey: String): List<ExpenseLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<ExpenseLine>)

    @Update
    suspend fun update(line: ExpenseLine)

    @Query("DELETE FROM expense_lines WHERE monthKey = :monthKey")
    suspend fun deleteForMonth(monthKey: String)

    @Query("UPDATE expense_lines SET isMandatory = :isMandatory, category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, isMandatory: Boolean, category: String)
}
