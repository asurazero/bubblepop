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
    var x: Float, // This will be the bubble's actual drawing position
    var y: Float, // This will be the bubble's actual drawing position
    val creationTime: Long = System.currentTimeMillis(),
    val lifespan: Long,
    val powerUpType: PowerUpType? = null,
    val bubbleType: BubbleType = BubbleType.NORMAL,
    var isRed: Boolean = false,
    var popLifespanMultiplier: Float = 1f,
    var isShrinking: Boolean = false,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f, // The base falling speed for the orbit's center or normal movement
    var canSplit: Boolean = false,
    var isChaoticMovementEnabled: Boolean = false,
    private val chaosFactor: Float = 3f,
    var initialX: Float = x, // The X-coordinate of the orbit's center (fixed horizontally for its "lane")
    var orbitalCenterY: Float = y, // The Y-coordinate of the orbit's center (this will fall)
    var orbitalAngle: Float = Random.nextFloat() * 360f,
    var orbitalRadius: Float = 50f,
    var orbitalSpeed: Float = 1f,
    // Add the flag to control orbital motion within the Bubble instance
    var isOrbitalBubble: Boolean = false // Default to false
) {
    init {
        // Initialize velocities for normal movement, or as base for orbitalCenterY fall
        velocityY = Random.nextFloat() + 4f
        velocityX = (Random.nextFloat() - 0.5f) * 2f

    // orbitalCenterY is already initialized to the bubble's initial y (spawn y)
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
        const val MAX_RADIUS = 200f

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
                bubbleType = BubbleType.NORMAL,
                initialX = poppedBubble.x + (poppedBubble.radius / 2 + newRadius) * cos(angle1),
                isOrbitalBubble = false // Split bubbles are not orbital by default
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
                bubbleType = BubbleType.NORMAL,
                initialX = poppedBubble.x + (poppedBubble.radius / 2 + newRadius) * cos(angle2),
                isOrbitalBubble = false // Split bubbles are not orbital by default
            )
            return listOf(bubble1, bubble2)
        }
    }

    fun update(screenWidth: Int, screenHeight: Int, isGameModeOrbitalActive: Boolean) { // Renamed for clarity
        // Use the new isOrbitalBubble flag to control orbital motion for THIS bubble instance
        if (isGameModeOrbitalActive && isOrbitalBubble && !isChaoticMovementEnabled) {
            // --- Crucial Change for Perfect Orbit ---
            // 1. Make the invisible orbit center fall
            orbitalCenterY += velocityY

            // 2. Update the orbital angle
            orbitalAngle += orbitalSpeed
            if (orbitalAngle > 360f) orbitalAngle -= 360f
            else if (orbitalAngle < 0f) orbitalAngle += 360f

            val angleInRadians = Math.toRadians(orbitalAngle.toDouble()).toFloat()

            // 3. Calculate the bubble's ACTUAL position relative to the falling orbit center
            val desiredX = initialX + orbitalRadius * cos(angleInRadians.toDouble()).toFloat()
            val desiredY = orbitalCenterY + orbitalRadius * sin(angleInRadians.toDouble()).toFloat()

            // 4. Set the bubble's (x, y) drawing position
            x = desiredX.coerceIn(radius, screenWidth - radius) // Clamp X to screen bounds
            y = desiredY // Y is set directly from orbital calculation

            // 5. Handle Y-axis boundary for the bubble's actual position (y)
            if (y - radius < 0) { // If the bubble itself hits the top of the screen
                y = radius // Clamp the bubble to the top edge
                velocityY *= -1f // Reverse the falling direction of the orbit's center

                // Crucially, if the bubble bounces, the orbit's center (orbitalCenterY)
                // must also be adjusted to maintain consistency.
                // Re-calculate orbitalCenterY based on the new 'y' and the current orbital offset.
                orbitalCenterY = y - orbitalRadius * sin(angleInRadians.toDouble()).toFloat()
            }
            // Note: Game.kt still handles removal if y > screenHeight (bubble falls off screen)

        } else if (isChaoticMovementEnabled) {
            // ... (Chaotic movement logic - remains the same)
            velocityX += Random.nextFloat() * chaosFactor - chaosFactor / 2
            velocityY += Random.nextFloat() * chaosFactor * 0.2f - chaosFactor * 0.1f

            velocityX *= 0.98f
            velocityY *= 0.98f

            x += velocityX
            y += velocityY

            if (x + radius > screenWidth) { x = screenWidth - radius; velocityX *= -0.8f } else if (x - radius < 0) { x = radius; velocityX *= -0.8f }
            if (y + radius > screenHeight) { y = screenHeight - radius; velocityY *= -0.8f } else if (y - radius < 0) { y = radius; velocityY *= -0.8f }

        } else {
            // Default movement: standard horizontal and vertical velocity, no mutators active
            x += velocityX
            y += velocityY

            if (x + radius > screenWidth || x - radius < 0) { velocityX *= -1f }
            if (y - radius < 0) { y = radius; velocityY *= -1f }
        }

        // Apply the maximum radius limit
        radius = radius.coerceAtMost(MAX_RADIUS)
    }
}