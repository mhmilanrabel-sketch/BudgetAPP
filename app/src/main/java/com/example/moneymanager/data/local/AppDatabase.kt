package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.security.KeyManager
import com.example.data.local.dao.BudgetMonthDao
import com.example.data.local.dao.ExpenseLineDao
import com.example.data.local.dao.MonthlySummaryDao
import com.example.data.local.dao.SalaryRecordDao
import com.example.data.local.entity.BudgetMonth
import com.example.data.local.entity.ExpenseLine
import com.example.data.local.entity.MonthlySummary
import com.example.data.local.entity.SalaryRecord
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        SalaryRecord::class,
        ExpenseLine::class,
        BudgetMonth::class,
        MonthlySummary::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun salaryRecordDao(): SalaryRecordDao
    abstract fun expenseLineDao(): ExpenseLineDao
    abstract fun budgetMonthDao(): BudgetMonthDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao

    companion object {
        private const val DB_NAME = "moneymanager_lk_secure.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            val passphrase = KeyManager.getOrCreateDatabasePassphrase(appContext)
            val supportFactory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(supportFactory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}