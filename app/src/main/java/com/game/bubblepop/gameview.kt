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
    private var initialTouchX = 0f  // Store initial touch position
    private var initialTouchY = 0f
    private var touchOffsetX = 0f // Store offset from bomb center
    private var touchOffsetY = 0f
    private var isCurrentlyDragging = false
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
    private val bombPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply{ //added bomb paint here
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        game?.let { currentGame ->
            // Draw the background
            canvas.drawColor(Color.WHITE)

            for (bubble in currentGame.getBubbles()) {
                val paintToUse = when (bubble.bubbleType) {
                    BubbleType.NORMAL -> if (bubble.isAboutToPop()) approachingPopPaint else normalPaint
                    BubbleType.NEGATIVE -> negativePaint
                    BubbleType.POWER_UP -> normalPaint
                    else -> normalPaint
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
                            PowerUpType.GREEN_RECTANGLE -> Color.GREEN
                        }
                    }
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius * 0.6f, powerUpPaint)
                }
                if (bubble.bubbleType == BubbleType.NEGATIVE) {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.BLACK
                        strokeWidth = bubble.radius * 0.1f
                    }
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius, strokePaint)

                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = bubble.radius * 0.6f
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText("-1", bubble.x, bubble.y + textPaint.textSize / 3, textPaint)

                    val innerCircleRadius = bubble.radius * 0.4f
                    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.DKGRAY
                        strokeWidth = bubble.radius * 0.05f
                    }
                    canvas.drawCircle(bubble.x, bubble.y, innerCircleRadius, innerPaint)
                }
            }
            //draw bomb
            if(currentGame.isDraggingBomb()){ //changed to use the public method
                val bombData = currentGame.getBombDetailsForDrawing()
                canvas.drawCircle(bombData.first, bombData.second, bombData.third, bombPaint)
            }

            val rectPaint = Paint()
            rectPaint.color = currentGame.rectangleColor
            rectPaint.style = Paint.Style.FILL
            canvas.drawRect(
                currentGame.getRectangleX(),
                currentGame.getRectangleY(),
                currentGame.getRectangleX() + currentGame.getRectangleWidth(),
                currentGame.getRectangleY() + currentGame.getRectangleHeight(),
                rectPaint
            )

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
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > clickDebounceDelay) {
                        val x = event.x
                        val y = event.y
                        currentGame.processClick(x, y)
                        lastClickTime = currentTime
                        if (currentGame.isDraggingBomb()) {
                            initialTouchX = event.x
                            initialTouchY = event.y
                            val bombData = currentGame.getBombDetailsForDrawing()
                            touchOffsetX = initialTouchX - bombData.first
                            touchOffsetY = initialTouchY - bombData.second
                            isCurrentlyDragging = true
                        }
                        return true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (currentGame.isDraggingBomb()) {
                        // Update bomb position, account for initial offset
                        currentGame.setBombCoordinates(event.x - touchOffsetX, event.y - touchOffsetY)
                        currentGame.redrawListener?.onRedrawRequested()
                        return true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (currentGame.isDraggingBomb()) {
                        currentGame.setDraggingBomb(false)
                        isCurrentlyDragging = false
                        currentGame.redrawListener?.onRedrawRequested()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
