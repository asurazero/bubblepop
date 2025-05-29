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
    var isStopped: Boolean = false,
    val defaultFillColor: Int = Color.parseColor("#808080") // Example: A default gray. Use your desired initial color.
) {
    private val fillPaint = Paint().apply {
        color = defaultFillColor // Initialize with the default color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for the thick black outline of the square block (remains unchanged)
    private val strokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
    }

    // --- NEW: Method to change the fill color ---
    fun setFillColor(color: Int) {
        fillPaint.color = color
    }

    // --- NEW: Method to get the current fill color (useful for checking) ---
    fun getCurrentFillColor(): Int {
        return fillPaint.color
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