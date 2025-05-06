package com.game.bubblepop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
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
    var velocityX: Float = 0f,  // Added for split bubble movement
    var velocityY: Float = 0f,   // Added for split bubble movement
    var canSplit: Boolean = false // Added for split mode
) {
    init {
        // Initialize velocity here.  Make sure the bubbles go down
        velocityY = Random.nextFloat() + 1f // Ensure a minimum downward speed
        velocityX = (Random.nextFloat() - 0.5f) * 3f // Give a little horizontal movement
    }

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 100, 200)
    }
    private val normalStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { //added paint
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f // Adjust the width as needed
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
        if (bubbleType == BubbleType.NORMAL) { //added if statement
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

    companion object {
        private const val MAX_RADIUS = 200f // Moved to companion object

        fun createSplitBubbles(poppedBubble: Bubble): List<Bubble> {
            val newRadius = poppedBubble.radius / 1.5f
            if (newRadius < 10f) {
                return emptyList() // Don't create bubbles too small
            }

            val speed = 2f
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
        x += velocityX
        y += velocityY

        if (x + radius > screenWidth) {
            x = screenWidth - radius;
            velocityX = -velocityX;
        } else if (x - radius < 0) {
            x = radius;
            velocityX = -velocityX;
        }

        if (y + radius > screenHeight) {
            y = screenHeight - radius;
            velocityY = -velocityY;
        } else if (y - radius < 0) {
            y = radius;
            velocityY = -velocityY;
        }
    }
}
