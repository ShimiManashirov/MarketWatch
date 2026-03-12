package com.example.marketwatch.di

import com.example.marketwatch.data.AuthRepository
import com.example.marketwatch.data.PortfolioRepository
import com.example.marketwatch.data.PostsRepository
import com.example.marketwatch.data.StockRepository
import com.example.marketwatch.data.UserRepository
import com.example.marketwatch.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository-related dependencies.
 * This class handles the provision of singleton repository instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth, db: FirebaseFirestore): AuthRepository {
        return AuthRepository(auth, db)
    }

    @Provides
    @Singleton
    fun provideUserRepository(db: FirebaseFirestore, auth: FirebaseAuth): UserRepository {
        return UserRepository(db, auth)
    }

    @Provides
    @Singleton
    fun providePostsRepository(db: FirebaseFirestore, localDb: AppDatabase, auth: FirebaseAuth): PostsRepository {
        return PostsRepository(db, localDb, auth)
    }

    @Provides
    @Singleton
    fun provideStockRepository(): StockRepository {
        return StockRepository()
    }

    @Provides
    @Singleton
    fun providePortfolioRepository(localDb: AppDatabase): PortfolioRepository {
        return PortfolioRepository(localDb)
    }
}
