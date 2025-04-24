package com.game.bubblepop

// GameView.kt
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.RectF

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs), Game.RedrawListener {
    private var gameContext: Context? = null
    lateinit var game: Game // Declare Game as lateinit var
    private var lastClickTime = 0L
    private val clickDebounceDelay = 200L

    init {
        gameContext = context
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        game.redrawListener = this
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        game.redrawListener = null
    }

    override fun onRedrawRequested() {
        invalidate()
    }

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val approachingPopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 48f
    }

    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 36f
    }

    // Paint for the red rectangle
    private val rectanglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        game?.let { currentGame ->
            // Draw the background (optional)
            canvas.drawColor(Color.WHITE)

            for (bubble in currentGame.getBubbles()) {
                val paintToUse = when (bubble.bubbleType) {
                    BubbleType.NORMAL -> if (bubble.isAboutToPop()) approachingPopPaint else normalPaint
                    BubbleType.NEGATIVE -> negativePaint
                    BubbleType.POWER_UP -> normalPaint
                    else -> normalPaint // Added else branch as a fallback
                }
                canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paintToUse)

                bubble.powerUpType?.let { powerUp ->
                    val powerUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = when (powerUp) {
                            PowerUpType.BOMB -> Color.BLACK
                            PowerUpType.SLOW_TIME -> Color.YELLOW
                            PowerUpType.EXTRA_LIFE -> Color.GREEN
                            PowerUpType.GROWTH_STOPPER -> Color.CYAN
                        }
                    }
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius * 0.6f, powerUpPaint)
                }
                if (bubble.bubbleType == BubbleType.NEGATIVE) {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.BLACK // Darker outline
                        strokeWidth = bubble.radius * 0.1f
                    }
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius, strokePaint) // Draw an outline

                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = bubble.radius * 0.6f // Slightly smaller text
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD // Make it bold
                    }
                    canvas.drawText("-1", bubble.x, bubble.y + textPaint.textSize / 3, textPaint)

                    // Add some subtle inner details (optional)
                    val innerCircleRadius = bubble.radius * 0.4f
                    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.DKGRAY
                        strokeWidth = bubble.radius * 0.05f
                    }
                    canvas.drawCircle(bubble.x, bubble.y, innerCircleRadius, innerPaint)
                }
            }

            // Draw the red rectangle if it's active
            if (currentGame.isRectangleActive()) {
                val rectX = currentGame.getRectangleX()
                val rectY = currentGame.getRectangleY()
                val rectWidth = currentGame.getRectangleWidth()
                val rectHeight = currentGame.getRectangleHeight()
                canvas.drawRect(rectX, rectY, rectX + rectWidth, rectY + rectHeight, rectanglePaint)
            }

            canvas.drawText("Score: ${currentGame.getScore()}", 50f, 100f, scorePaint)
            canvas.drawText("Level: ${currentGame.getLevel()}", 50f, 150f, levelPaint)
            canvas.drawText("Missed: ${currentGame.getMissedBubbles()}", 50f, 200f, scorePaint)

            currentGame.update()
            if (currentGame.getBubbles().isNotEmpty() || !currentGame.isGameOver()) {
                postInvalidateOnAnimation()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        game?.let { currentGame ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > clickDebounceDelay) {
                    val x = event.x
                    val y = event.y
                    currentGame.processClick(x, y)
                    lastClickTime = currentTime
                    return true // Consume the event
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
