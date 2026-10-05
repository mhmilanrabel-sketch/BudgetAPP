package com.example.moneymanager.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneymanager.core.security.KeyManager
import com.example.moneymanager.data.local.dao.BudgetCategoryDao
import com.example.moneymanager.data.local.dao.BudgetMonthDao
import com.example.moneymanager.data.local.dao.ExpenseLineDao
import com.example.moneymanager.data.local.dao.MonthlySummaryDao
import com.example.moneymanager.data.local.dao.SalaryRecordDao
import com.example.moneymanager.data.local.entity.BudgetCategory
import com.example.moneymanager.data.local.entity.BudgetMonth
import com.example.moneymanager.data.local.entity.ExpenseLine
import com.example.moneymanager.data.local.entity.MonthlySummary
import com.example.moneymanager.data.local.entity.SalaryRecord
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        SalaryRecord::class,
        ExpenseLine::class,
        BudgetMonth::class,
        MonthlySummary::class,
        BudgetCategory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun salaryRecordDao(): SalaryRecordDao
    abstract fun expenseLineDao(): ExpenseLineDao
    abstract fun budgetMonthDao(): BudgetMonthDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "moneymanager_lk_secure.db"

        /** Adds the budget_categories table without touching existing data. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budget_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        keywordsCsv TEXT NOT NULL,
                        monthlyLimit REAL NOT NULL,
                        colorHex TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase {
            val factory = try {
                val passphrase = KeyManager.getOrCreateDatabasePassphrase(context)
                SupportFactory(passphrase)
            } catch (t: Throwable) {
                Log.w(TAG, "SQLCipher unavailable; using unencrypted Room", t)
                null
            }

            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).addMigrations(MIGRATION_1_2)

            return if (factory != null) {
                builder.openHelperFactory(factory).build()
            } else {
                builder.build()
            }
        }
    }
}