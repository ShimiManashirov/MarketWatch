package com.example.marketwatch

import android.graphics.*
import com.squareup.picasso.Transformation

/**
 * A robust Picasso transformation to crop an image into a circle.
 * Properly manages bitmap recycling to prevent "recycled bitmap" crashes.
 */
class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        // Create a squared version of the source
        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        
        // Create the output bitmap
        val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        // Picasso Contract: We must recycle the source bitmap.
        source.recycle()
        
        // If squaredBitmap is a new instance (not source), we should recycle it too.
        if (squaredBitmap != source) {
            squaredBitmap.recycle()
        }

        return bitmap
    }

    override fun key(): String = "circle"
}
