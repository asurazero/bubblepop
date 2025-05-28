// SquareBlock.kt
package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF // <--- ***CRITICAL: ENSURE THIS IMPORT IS HERE***

data class SquareBlock(
    var x: Float,
    var y: Float,
    val size: Float,
    val fillColor: Int,
    var speed: Float = 5f,
    var isStopped: Boolean = false
) {
    // Paint for the inner fill of the square block
    private val fillPaint = Paint().apply {
        color = fillColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for the thick black outline of the square block
    private val strokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
    }

    // Property to easily get the bounding box of the square block
    // This `RectF` must refer to `android.graphics.RectF`
    val bounds: RectF // <-- This type must resolve to android.graphics.RectF
        get() = RectF(x, y, x + size, y + size) // <-- This constructor must be for android.graphics.RectF

    fun moveVertically() {
        y += speed
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, fillPaint)
        canvas.drawRect(bounds, strokePaint)
    }
}