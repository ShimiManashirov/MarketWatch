package com.example.marketwatch.di

import com.example.marketwatch.FinnhubApiService
import com.example.marketwatch.FrankfurterApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Hilt module that provides network-related and Firebase dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideFinnhubApiService(client: OkHttpClient): FinnhubApiService {
        return Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFrankfurterApiService(client: OkHttpClient): FrankfurterApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.frankfurter.app/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApiService::class.java)
    }
}
