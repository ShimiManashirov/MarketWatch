package com.example.marketwatch

import android.graphics.*
import com.squareup.picasso.Transformation

/**
 * A standard Picasso transformation to crop an image into a circle.
 * This version is more robust and handles potential Bitmap configuration issues.
 */
class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        
        // We do NOT recycle source here if it's the same as squaredBitmap
        // But Bitmap.createBitmap might return the same object if no changes needed.
        
        val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        // Clean up the intermediate squared bitmap
        if (squaredBitmap != source) {
            squaredBitmap.recycle()
        }
        
        // Picasso requires us to recycle the source bitmap
        source.recycle()

        return bitmap
    }

    override fun key(): String = "circle"
}
