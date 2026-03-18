package com.example.marketwatch.data

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class ImgurRepository {
    // You should create your own Client ID at https://api.imgur.com/oauth2/addclient
    private val CLIENT_ID = "629d899557a536b" // Temporary placeholder - highly recommended to use your own
    
    private val apiService: ImgurApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ImgurApiService::class.java)
    }

    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return try {
            val file = uriToFile(context, imageUri) ?: return null
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            
            val response = apiService.uploadImage("Client-ID $CLIENT_ID", body)
            if (response.isSuccessful) {
                response.body()?.data?.link
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return file
    }
}
