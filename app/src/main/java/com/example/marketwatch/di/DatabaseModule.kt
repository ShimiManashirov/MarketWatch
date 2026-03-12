package com.example.marketwatch.di

import android.content.Context
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostDao
import com.example.marketwatch.data.local.StockDao
import com.example.marketwatch.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 * This class handles the creation and scoping of the Room database and its DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of the [AppDatabase].
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Provides the [PostDao] from the [AppDatabase].
     */
    @Provides
    fun providePostDao(database: AppDatabase): PostDao {
        return database.postDao()
    }

    /**
     * Provides the [StockDao] from the [AppDatabase].
     */
    @Provides
    fun provideStockDao(database: AppDatabase): StockDao {
        return database.stockDao()
    }

    /**
     * Provides the [UserDao] from the [AppDatabase].
     */
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}
