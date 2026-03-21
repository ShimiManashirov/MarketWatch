package com.example.marketwatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Utility class for handling image saving, loading, and caching with Picasso
 */
object ImageManager {

    private const val CACHE_DURATION_DAYS = 30L
    private const val IMAGE_QUALITY = 85
    private const val MAX_IMAGE_WIDTH = 1080  // Maximum width in pixels
    private const val MAX_IMAGE_HEIGHT = 1080 // Maximum height in pixels

    /**
     * Compress an image from Uri and return it as ByteArray for Firebase upload
     */
    fun uriToCompressedBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val optimizedBitmap = optimizeBitmap(originalBitmap)
            
            val outputStream = ByteArrayOutputStream()
            optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
            
            val bytes = outputStream.toByteArray()
            
            // Cleanup
            if (optimizedBitmap != originalBitmap) {
                optimizedBitmap.recycle()
            }
            originalBitmap.recycle()
            
            bytes
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save a bitmap to local storage with size optimization
     */
    fun saveBitmapLocally(context: Context, bitmap: Bitmap, fileName: String? = null): String {
        val finalFileName = fileName ?: "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, finalFileName)
        
        try {
            val optimizedBitmap = optimizeBitmap(bitmap)
            FileOutputStream(file).use { out ->
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
                out.flush()
            }
            if (optimizedBitmap != bitmap) optimizedBitmap.recycle()
        } catch (e: Exception) {
            throw Exception("Failed to save bitmap: ${e.message}")
        }
        
        return file.absolutePath
    }

    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) return bitmap
        
        val widthRatio = width.toFloat() / MAX_IMAGE_WIDTH
        val heightRatio = height.toFloat() / MAX_IMAGE_HEIGHT
        val scaleFactor = maxOf(widthRatio, heightRatio)
        
        val newWidth = (width / scaleFactor).toInt()
        val newHeight = (height / scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Load an image into an ImageView with Picasso
     * @param skipCache Useful when updating profile picture to force reload
     */
    fun loadImage(
        imageView: ImageView,
        source: String?,
        placeholderId: Int = R.drawable.ic_account_circle,
        errorId: Int = R.drawable.ic_account_circle,
        isCircle: Boolean = false,
        skipCache: Boolean = false
    ) {
        if (source.isNullOrEmpty()) {
            imageView.setImageResource(placeholderId)
            return
        }

        var request = if (source.startsWith("http")) {
            Picasso.get().load(source)
        } else {
            Picasso.get().load(File(source))
        }

        if (skipCache) {
            request = request
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
        }

        request = request
            .placeholder(placeholderId)
            .error(errorId)
            .resize(1080, 1080)
            .onlyScaleDown()
            .centerCrop()

        if (isCircle) {
            request = request.transform(CircleTransform())
        }

        request.into(imageView)
    }

    fun loadProfileImage(
        imageView: ImageView,
        source: String?,
        placeholderId: Int = R.drawable.ic_account_circle,
        skipCache: Boolean = false
    ) {
        loadImage(
            imageView = imageView,
            source = source,
            placeholderId = placeholderId,
            errorId = placeholderId,
            isCircle = true,
            skipCache = skipCache
        )
    }

    fun getImageCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return cacheDir
    }

    @Suppress("unused")
    fun clearOldCache(context: Context) {
        val cacheDir = getImageCacheDir(context)
        val now = System.currentTimeMillis()
        val maxAge = TimeUnit.DAYS.toMillis(CACHE_DURATION_DAYS)
        cacheDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAge) file.delete()
        }
    }
}
