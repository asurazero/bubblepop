package com.game.bubblepop // Make sure this matches your app's package

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

class SpikeTrap(
    var x: Float,
    var y: Float,
    var width: Float, // Changed from private val to var for public access
    var height: Float, // Changed from private val to var for public access
    private val speed: Float
) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val hitBox: RectF = RectF(x, y, x + width, y + height)

    fun update(deltaTime: Long) {
        y -= speed * (deltaTime / 1000f) // Move upwards. deltaTime in milliseconds.
        hitBox.set(x, y, x + width, y + height) // Update hitbox position
    }

    fun draw(canvas: Canvas) {
        val path = Path()
        path.moveTo(x + width / 2, y) // Top point
        path.lineTo(x, y + height)    // Bottom-left point
        path.lineTo(x + width, y + height) // Bottom-right point
        path.close() // Connect back to the top point
        canvas.drawPath(path, paint)
    }

    fun getHitBox(): RectF = hitBox

    // Check for collision with a bubble's circle
    fun collidesWithBubble(bubbleX: Float, bubbleY: Float, bubbleRadius: Float): Boolean {
        // Simple bounding box check first
        if (!hitBox.intersects(bubbleX - bubbleRadius, bubbleY - bubbleRadius, bubbleX + bubbleRadius, bubbleY + bubbleRadius)) {
            return false
        }

        // More accurate collision for triangle and circle.
        // This is a simplified approach, a full point-in-triangle or line-segment-circle intersection
        // would be more complex. For a game like this, a bounding box check might be sufficient
        // or a slightly more refined check.

        // As a starting point, check if the bubble's center is within the spike's bounding box
        // or if the bubble significantly overlaps.
        val closestX = Math.max(hitBox.left, Math.min(bubbleX, hitBox.right))
        val closestY = Math.max(hitBox.top, Math.min(bubbleY, hitBox.bottom))

        val dx = bubbleX - closestX
        val dy = bubbleY - closestY

        return (dx * dx + dy * dy) < (bubbleRadius * bubbleRadius)
    }

    // Check if a tap (point) is within the spike's bounding box
    fun isTapped(tapX: Float, tapY: Float): Boolean {
        return hitBox.contains(tapX, tapY)
    }
}
