// SquareBlock.kt
package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF

data class SquareBlock(
    var x: Float,
    var y: Float,
    val size: Float,
    val fillColor: Int,
    var initialSpeed: Float = 5f, // Renamed to clarify it's an initial push, not constant speed
    var isStopped: Boolean = false,
    val defaultFillColor: Int = Color.parseColor("#808080")
) {
    private val fillPaint = Paint().apply {
        color = defaultFillColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f
        isAntiAlias = true
    }

    // --- NEW: Physics properties ---
    private var velocityY: Float = 0f // Vertical velocity
    private val GRAVITY = 200f // Halved gravity for slower fall // Pixels per second squared (adjust as needed, standard gravity is ~9.8m/s^2)
    // This value will need tuning based on your game's scale and desired fall speed.
    // 980 is a common starting point for pixels/second^2.

    // --- NEW: Method to change the fill color ---
    fun setFillColor(color: Int) {
        fillPaint.color = color
    }

    // --- NEW: Method to get the current fill color (useful for checking) ---
    fun getCurrentFillColor(): Int {
        return fillPaint.color
    }

    // Property to easily get the bounding box of the square block
    val bounds: RectF
        get() = RectF(x, y, x + size, y + size)

    // --- MODIFIED: moveVertically to use deltaTime and gravity ---
    fun moveVertically(deltaTime: Long) { // Pass deltaTime here
        if (isStopped) {
            velocityY = 0f // Reset velocity if stopped
            return
        }

        // Convert deltaTime from milliseconds to seconds
        val dtSeconds = deltaTime / 1000f

        // Apply gravity to velocity
        velocityY += GRAVITY * dtSeconds

        // Apply velocity to position
        y += velocityY * dtSeconds

        // You might want to add a terminal velocity here to prevent blocks from falling too fast
        // val MAX_FALL_SPEED = 1500f // Pixels per second
        // if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, fillPaint)
        canvas.drawRect(bounds, strokePaint)
    }
}