package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

class PowerUp(var x: Float, var y: Float, var radius: Float, val type: PowerUpType) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = when (type) {
            PowerUpType.BOMB -> Color.BLACK
            PowerUpType.SLOW_TIME -> Color.YELLOW
            PowerUpType.EXTRA_LIFE -> Color.GREEN
            PowerUpType.GROWTH_STOPPER -> Color.CYAN
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
        // You could add an icon or text on the power-up here to make it more visually distinct
    }

    fun isClicked(clickX: Float, clickY: Float): Boolean {
        val distanceSquared = (clickX - x) * (clickX - x) + (clickY - y) * (clickY - y)
        return distanceSquared < radius * radius
    }
}

enum class PowerUpType {
    BOMB,
    SLOW_TIME,
    EXTRA_LIFE,
    GROWTH_STOPPER
}