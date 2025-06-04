package com.game.bubblepop // Make sure this matches your package name

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sqrt

/**
 * Represents a single projectile fired by the turret.
 *
 * @param x The current X coordinate of the projectile's center.
 * @param y The current Y coordinate of the projectile's center.
 * @param radius The radius of the projectile.
 * @param speedX The horizontal speed of the projectile.
 * @param speedY The vertical speed of the projectile (negative for upwards movement).
 * @param color The color of the projectile.
 */
data class Projectile(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedX: Float, // Added horizontal speed
    val speedY: Float,
    val color: Int
) {
    // NEW: Paint for the projectile's outline
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK // Black outline
        style = Paint.Style.STROKE
        strokeWidth = 5f // Thick outline, adjust as needed
    }
    /**
     * Updates the projectile's position based on its speed.
     * @param deltaTime The time elapsed since the last update, in milliseconds.
     */
    fun update(deltaTime: Long) {
        val dtSeconds = deltaTime / 1000f
        x += speedX * dtSeconds // Update horizontal position
        y += speedY * dtSeconds // Update vertical position
    }

    /**
     * Draws the projectile on the given canvas.
     * @param canvas The canvas to draw on.
     * @param paint The paint object to use for drawing.
     */
    fun draw(canvas: Canvas, paint: Paint) {
        // Draw the fill color
        paint.color = color
        canvas.drawCircle(x, y, radius, paint)

        // Draw the outline
        canvas.drawCircle(x, y, radius, strokePaint)
    }

    /**
     * Checks if the projectile is off-screen (above the top edge or outside horizontal bounds).
     * @return True if the projectile is off-screen, false otherwise.
     */
    fun isOffScreen(screenWidth: Int, screenHeight: Int): Boolean {
        // Consider off-screen if completely outside the view bounds
        return x + radius < 0 || x - radius > screenWidth || y + radius < 0 || y - radius > screenHeight
    }
}