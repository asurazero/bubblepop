package com.game.bubblepop


import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

data class SquareBlock(
    var x: Float,
    var y: Float,
    val size: Float,
    // The color parameter in the constructor will now be used for the fill color
    // but we'll hardcode the outline color here for simplicity.
    val fillColor: Int, // Renamed 'color' to 'fillColor' for clarity
    var speed: Float = 5f,
    var isStopped: Boolean = false
) {
    private val fillPaint = Paint().apply {
        color = fillColor // Use the fillColor passed to the constructor
        style = Paint.Style.FILL
        isAntiAlias = true // For smoother edges
    }

    private val strokePaint = Paint().apply {
        color = Color.BLACK // Black color for the outline
        style = Paint.Style.STROKE // Set style to STROKE for outline
        strokeWidth = 12f // Adjust thickness as desired (e.g., 8f, 10f, 15f)
        isAntiAlias = true // For smoother edges
    }

    val bounds: RectF
        get() = RectF(x, y, x + size, y + size)

    fun update(screenHeight: Int) {
        if (!isStopped) {
            y += speed
            if (y + size >= screenHeight) {
                y = (screenHeight - size).toFloat()
                isStopped = true
            }
        }
    }

    fun draw(canvas: Canvas) {
        // Draw the fill first
        canvas.drawRect(bounds, fillPaint)
        // Then draw the outline on top
        canvas.drawRect(bounds, strokePaint)
    }
}