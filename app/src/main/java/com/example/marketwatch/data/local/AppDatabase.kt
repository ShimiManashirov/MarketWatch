package com.example.marketwatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database for the application.
 * Contains tables for Stocks, Posts, User profiles, and News Bookmarks.
 */
@Database(
    entities = [
        StockEntity::class, 
        PostEntity::class, 
        UserEntity::class, 
        NewsBookmarkEntity::class
    ], 
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    
    /** Provides access to stock-related database operations. */
    abstract fun stockDao(): StockDao
    
    /** Provides access to community post-related database operations. */
    abstract fun postDao(): PostDao
    
    /** Provides access to user profile-related database operations. */
    abstract fun userDao(): UserDao

    /** Provides access to news bookmark-related database operations. */
    abstract fun newsBookmarkDao(): NewsBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the database.
         * If it doesn't exist, it creates it using the application context.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "market_watch_db"
                )
                .fallbackToDestructiveMigration() // Simplifies development during schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
