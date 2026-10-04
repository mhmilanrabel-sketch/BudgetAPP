package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ExpenseLine
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseLineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<ExpenseLine>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(line: ExpenseLine): Long

    @Query("""
        SELECT * FROM expense_lines
        WHERE month = :month
        ORDER BY isMandatory DESC, item ASC
    """)
    fun observeByMonth(month: String): Flow<List<ExpenseLine>>

    @Query("SELECT * FROM expense_lines WHERE month = :month")
    suspend fun getByMonth(month: String): List<ExpenseLine>

    @Query("SELECT * FROM expense_lines WHERE month = :month AND isMandatory = 1")
    suspend fun getMandatoryByMonth(month: String): List<ExpenseLine>

    @Query("SELECT * FROM expense_lines WHERE month = :month AND isMandatory = 0")
    suspend fun getOptionalByMonth(month: String): List<ExpenseLine>

    @Query("UPDATE expense_lines SET notPaid = :notPaid, realPay = :realPay WHERE id = :id")
    suspend fun updatePayment(id: Long, notPaid: Double, realPay: Double)

    @Query("DELETE FROM expense_lines WHERE month = :month")
    suspend fun deleteByMonth(month: String)

    @Query("DELETE FROM expense_lines")
    suspend fun deleteAll()
}