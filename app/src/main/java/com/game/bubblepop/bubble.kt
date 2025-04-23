package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

enum class BubbleType {
    NORMAL,
    NEGATIVE,
    POWER_UP
}


class Bubble(
    val id: Int,
    var radius: Float,
    val initialRadius: Float,
    var x: Float,
    var y: Float,
    val creationTime: Long = System.currentTimeMillis(),
    val lifespan: Long,
    val powerUpType: PowerUpType? = null,
    val bubbleType: BubbleType = BubbleType.NORMAL,
    var isRed: Boolean = false,  // Added isRed property
    var popLifespanMultiplier: Float = 1f, // Added popLifespanMultiplier
    var isShrinking: Boolean = false // Added isShrinking property
) {
    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 100, 200)
    }

    private val approachingPopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
    }

    fun draw(canvas: Canvas) {
        val paintToUse = when (bubbleType) {
            BubbleType.NORMAL, BubbleType.POWER_UP -> {
                if (isRed || isAboutToPop()) approachingPopPaint else normalPaint
            }
            BubbleType.NEGATIVE -> negativePaint
            else -> normalPaint
        }
        canvas.drawCircle(x, y, radius, paintToUse)

        powerUpType?.let { powerUp ->
            val powerUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = when (powerUp) {
                    PowerUpType.BOMB -> Color.BLACK
                    PowerUpType.SLOW_TIME -> Color.YELLOW
                    PowerUpType.EXTRA_LIFE -> Color.GREEN
                    PowerUpType.GROWTH_STOPPER -> Color.CYAN
                }
            }
            canvas.drawCircle(x, y, radius * 0.6f, powerUpPaint)
        }

        if (bubbleType == BubbleType.NEGATIVE) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK // Darker outline
                strokeWidth = radius * 0.1f
            }
            canvas.drawCircle(x, y, radius, strokePaint) // Draw an outline

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = radius * 0.6f // Slightly smaller text
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD // Make it bold
            }
            canvas.drawText("-1", x, y + textPaint.textSize / 3, textPaint)

            // Add some subtle inner details (optional)
            val innerCircleRadius = radius * 0.4f
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.DKGRAY
                strokeWidth = radius * 0.05f
            }
            canvas.drawCircle(x, y, innerCircleRadius, innerPaint)
        }
    }

    fun isAboutToPop(): Boolean {
        return System.currentTimeMillis() - creationTime > lifespan * 0.8f
    }

    fun isClicked(clickX: Float, clickY: Float): Boolean {
        val distance = sqrt((clickX - x) * (clickX - x) + (clickY - y) * (clickY - y))
        return distance <= radius
    }

    fun shouldPop(): Boolean {
        return System.currentTimeMillis() - creationTime > lifespan * popLifespanMultiplier
    }
}
