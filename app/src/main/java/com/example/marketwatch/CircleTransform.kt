package com.example.marketwatch

import android.graphics.*
import com.squareup.picasso.Transformation

/**
 * A robust Picasso transformation to crop an image into a circle.
 * Properly manages bitmap recycling to comply with Picasso's contract.
 */
class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = Math.min(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        
        val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        // Picasso contract: source must be recycled if a different instance is returned
        source.recycle()
        if (squaredBitmap !== source) {
            squaredBitmap.recycle()
        }

        return bitmap
    }

    override fun key(): String = "circle"
}
