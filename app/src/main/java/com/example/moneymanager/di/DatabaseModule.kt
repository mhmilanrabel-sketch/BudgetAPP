package com.example.moneymanager.di

import android.content.Context
import androidx.room.Room
import com.example.moneymanager.MoneyManagerApplication
import com.example.moneymanager.data.local.AppDatabase
import com.example.moneymanager.data.local.dao.BudgetDao
import com.example.moneymanager.data.local.dao.ExpenseDao
import com.example.moneymanager.data.local.dao.SalaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "money_manager.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val app = context as MoneyManagerApplication
        val passphrase: ByteArray = app.getDatabasePassphrase()

        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()   // TODO: replace with real Migrations before release
            .build()
    }

    @Provides
    fun provideSalaryDao(db: AppDatabase): SalaryDao = db.salaryDao()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
}