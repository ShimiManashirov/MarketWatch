package com.example.marketwatch

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.squareup.picasso.Picasso
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
     * Save a bitmap to local storage with size optimization
     * @param context The application context
     * @param bitmap The bitmap to save
     * @param fileName The name of the file (optional, will generate if not provided)
     * @return The absolute path of the saved file
     */
    fun saveBitmapLocally(context: Context, bitmap: Bitmap, fileName: String? = null): String {
        val finalFileName = fileName ?: "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, finalFileName)
        
        try {
            // Optimize bitmap size if too large
            val optimizedBitmap = optimizeBitmap(bitmap)
            
            FileOutputStream(file).use { out ->
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
                out.flush()
            }
            
            // Recycle the optimized bitmap if it's different
            if (optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
            }
            
        } catch (e: Exception) {
            throw Exception("Failed to save bitmap: ${e.message}")
        }
        
        return file.absolutePath
    }

    /**
     * Optimize bitmap size to prevent OutOfMemory errors
     */
    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // If bitmap is smaller than max, return as is
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return bitmap
        }
        
        // Calculate scaling ratio
        val widthRatio = width.toFloat() / MAX_IMAGE_WIDTH
        val heightRatio = height.toFloat() / MAX_IMAGE_HEIGHT
        val scaleFactor = maxOf(widthRatio, heightRatio)
        
        val newWidth = (width / scaleFactor).toInt()
        val newHeight = (height / scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // ...existing code...

    /**
     * Load an image into an ImageView with Picasso
     * Handles both URL and local file paths
     * @param imageView The target ImageView
     * @param source The image source (URL or file path)
     * @param placeholderId The placeholder drawable resource ID
     * @param errorId The error drawable resource ID
     * @param isCircle Whether to apply circle transformation
     */
    fun loadImage(
        imageView: ImageView,
        source: String?,
        placeholderId: Int = R.drawable.ic_account_circle,
        errorId: Int = R.drawable.ic_account_circle,
        isCircle: Boolean = false
    ) {
        if (source.isNullOrEmpty()) {
            imageView.setImageResource(placeholderId)
            return
        }

        val picassoRequest = if (source.startsWith("http")) {
            Picasso.get().load(source)
        } else {
            // It's a local file path
            Picasso.get().load(File(source))
        }

        picassoRequest
            .placeholder(placeholderId)
            .error(errorId)
            // Use explicit resize in pixels (avoid resizeDimen which expects resource ids)
            .resize(400, 400)
            .centerCrop()

        if (isCircle) {
            picassoRequest.transform(CircleTransform())
        }

        picassoRequest.into(imageView)
    }

    /**
     * Load a profile image with optimizations
     */
    fun loadProfileImage(
        imageView: ImageView,
        source: String?,
        placeholderId: Int = R.drawable.ic_account_circle
    ) {
        loadImage(
            imageView = imageView,
            source = source,
            placeholderId = placeholderId,
            errorId = placeholderId,
            isCircle = true
        )
    }

    /**
     * Get the cache directory for images
     */
    fun getImageCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Clear old cached images (older than CACHE_DURATION_DAYS)
     */
    @Suppress("unused")
    fun clearOldCache(context: Context) {
        val cacheDir = getImageCacheDir(context)
        val now = System.currentTimeMillis()
        val maxAge = TimeUnit.DAYS.toMillis(CACHE_DURATION_DAYS)

        cacheDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAge) {
                file.delete()
            }
        }
    }

    /**
     * Delete a specific cached image
     */
    @Suppress("unused")
    fun deleteCachedImage(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get size of all cached images
     */
    @Suppress("unused")
    fun getCacheSizeInMB(context: Context): Double {
        val cacheDir = getImageCacheDir(context)
        var sizeInBytes = 0L
        cacheDir.listFiles()?.forEach { file ->
            sizeInBytes += file.length()
        }
        return sizeInBytes / (1024.0 * 1024.0)
    }

    /**
     * Clear all cached images
     */
    @Suppress("unused")
    fun clearAllCache(context: Context) {
        val cacheDir = getImageCacheDir(context)
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}

