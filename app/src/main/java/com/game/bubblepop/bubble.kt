package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
    var isRed: Boolean = false,
    var popLifespanMultiplier: Float = 1f,
    var isShrinking: Boolean = false,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var canSplit: Boolean = false,
    var isChaoticMovementEnabled: Boolean = false, // New flag for chaotic movement
    private val chaosFactor: Float = 3f // Adjust this value to control chaos intensity
) {
    init {
        // Initialize velocity here. Make sure the bubbles go down initially
        velocityY = Random.nextFloat() + 1f
        velocityX = (Random.nextFloat() - 0.5f) * 2f // Less initial horizontal movement
    }

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 100, 200)
    }
    private val normalStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
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
        if (bubbleType == BubbleType.NORMAL) {
            canvas.drawCircle(x, y, radius, normalStrokePaint)
        }

        powerUpType?.let { powerUp ->
            val powerUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = when (powerUp) {
                    PowerUpType.BOMB -> Color.BLACK
                    PowerUpType.SLOW_TIME -> Color.YELLOW
                    PowerUpType.EXTRA_LIFE -> Color.GREEN
                    PowerUpType.GROWTH_STOPPER -> Color.CYAN
                    PowerUpType.GREEN_RECTANGLE -> Color.GREEN
                }
            }
            canvas.drawCircle(x, y, radius * 0.6f, powerUpPaint)
        }

        if (bubbleType == BubbleType.NEGATIVE) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.BLACK
                strokeWidth = radius * 0.1f
            }
            canvas.drawCircle(x, y, radius, strokePaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = radius * 0.6f
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText("-1", x, y + textPaint.textSize / 3, textPaint)

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

    companion object {
        const val MAX_RADIUS = 200f // It's good practice to keep constants in the companion object

        fun createSplitBubbles(poppedBubble: Bubble): List<Bubble> {
            val newRadius = poppedBubble.radius / 1.5f
            if (newRadius < 10f) {
                return emptyList()
            }

            val speed = 0.2f
            val angle1 = Random.nextDouble(0.0, 2 * Math.PI).toFloat()
            val angle2 = (angle1 + Math.PI + Random.nextDouble(-0.1, 0.1)).toFloat()

            val bubble1 = Bubble(
                id = -1,
                radius = newRadius,
                initialRadius = newRadius,
                x = poppedBubble.x + (poppedBubble.radius / 2 + newRadius) * cos(angle1),
                y = poppedBubble.y + (poppedBubble.radius / 2 + newRadius) * sin(angle1),
                lifespan = (poppedBubble.lifespan * 0.7f).toLong(),
                velocityX = speed * cos(angle1),
                velocityY = speed * sin(angle1),
                bubbleType = BubbleType.NORMAL
            )
            val bubble2 = Bubble(
                id = -1,
                radius = newRadius,
                initialRadius = newRadius,
                x = poppedBubble.x + (poppedBubble.radius / 2 + newRadius) * cos(angle2),
                y = poppedBubble.y + (poppedBubble.radius / 2 + newRadius) * sin(angle2),
                lifespan = (poppedBubble.lifespan * 0.7f).toLong(),
                velocityX = speed * cos(angle2),
                velocityY = speed * sin(angle2),
                bubbleType = BubbleType.NORMAL
            )
            return listOf(bubble1, bubble2)
        }
    }

    fun update(screenWidth: Int, screenHeight: Int) {
        if (MainActivity.GameModeStates.isChaosModeActive) {
            velocityX += Random.nextFloat() * chaosFactor - chaosFactor / 2
            velocityY += Random.nextFloat() * chaosFactor * 0.2f - chaosFactor * 0.1f // Slight vertical variation

            // Damping effect to prevent excessive speed
            velocityX *= 0.98f
            velocityY *= 0.98f

            // Keep bubbles within horizontal bounds with damping on collision
            if (x + radius > screenWidth) {
                x = screenWidth - radius
                velocityX *= -0.8f
            } else if (x - radius < 0) {
                x = radius
                velocityX *= -0.8f
            }

            // Keep bubbles within vertical bounds with damping on collision
            if (y + radius > screenHeight) {
                y = screenHeight - radius
                velocityY *= -0.8f
            } else if (y - radius < 0) {
                y = radius
                velocityY *= -0.8f
            }

            x += velocityX
            y += velocityY

        } else {
            // Default downward movement with slight initial horizontal drift
            y += velocityY
            x += velocityX

            if (x + radius > screenWidth || x - radius < 0) {
                velocityX *= -1f
            }
            if (y + radius > screenHeight || y - radius < 0) {
                velocityY *= -1f // Basic bounce off top/bottom if needed
            }
        }
    }
}