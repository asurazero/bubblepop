package com.game.bubblepop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.RectF
import androidx.media3.common.util.Log
import com.game.bubblepop.MainActivity.GameModeStates // Import GameModeStates directly

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs), Game.RedrawListener {
    //Other Game modes
    var isSplitModeActive = false


    //Other Game modes
    private var gameContext: Context? = null
    lateinit var game: Game // Declare Game as lateinit var
    private var lastClickTime = 0L
    private val clickDebounceDelay = 200L
    private var initialTouchX = 0f  // Store initial touch position
    private var initialTouchY = 0f
    private var touchOffsetX = 0f // Store offset from bomb center
    private var touchOffsetY = 0f
    private var isCurrentlyDragging = false
    private var lastFrameTime: Long = System.currentTimeMillis() // Added to correctly calculate deltaTime

    init {
        gameContext = context
    }

    override fun onAttachedToWindow() {
        if(MainActivity.GameModeStates.isSplitModeActive==true){
            isSplitModeActive=true
        }else isSplitModeActive=false

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
    private val normalStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // Added for the outline
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f // Adjust as needed
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
    val paint = Paint()

    // --- NEW: Paint for Spike Traps ---
    private val spikeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // For the red fill
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val spikeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // For the black outline
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f // Thicker outline for spikes, adjust as needed
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        game?.let { currentGame ->
            val currentTime = System.currentTimeMillis()
            val deltaTime = currentTime - lastFrameTime // Calculate deltaTime
            lastFrameTime = currentTime // Update for the next frame

            // Draw the background
            canvas.drawColor(Color.WHITE)

            // Bomb logic:  Check for bomb state *before* drawing bubbles
            if (currentGame.isBombActive() && System.currentTimeMillis() > currentGame.getBombEndTime()) {
                currentGame.setBombActive(false)
                currentGame.setBombStopped(false)
            }
            // Display the power-up text
            if (currentGame.getDisplayingPowerUpText()) { // Access via getter

                paint.color = Color.BLACK
                paint.textSize = 80f
                paint.textAlign = Paint.Align.CENTER

                val textX = width / 2f
                val textY = height / 4f
                canvas.drawText(currentGame.getPowerUpText(), textX, textY, paint) // Access via getter

                if (System.currentTimeMillis() > currentGame.getPowerUpTextEndTime()) { // Access via getter
                    currentGame.isDisplayingPowerUpText = false // Access via setter
                    currentGame.redrawListener?.onRedrawRequested()
                }
            }
            // Get the bubbles list *once* here
            val bubbles = currentGame.getBubbles()
            for (bubble in bubbles) {
                // Null check for the bubble object
                if (bubble != null) {
                    val paintToUse = when (bubble.bubbleType) {
                        BubbleType.NORMAL -> {
                            // Check for level > 50 (isRed) or about to pop
                            if (bubble.isRed || bubble.isAboutToPop()) approachingPopPaint else normalPaint
                        }
                        BubbleType.NEGATIVE -> negativePaint
                        BubbleType.POWER_UP -> normalPaint
                        // No else needed if all BubbleType enums are covered
                    }
                    //check for the max radius
                    val drawRadius = Math.min(bubble.radius, 200f) // Using remembered upper limit
                    canvas.drawCircle(bubble.x, bubble.y, drawRadius, paintToUse)
                    if (bubble.bubbleType == BubbleType.NORMAL) { // Draw outline for normal bubbles
                        canvas.drawCircle(bubble.x, bubble.y, drawRadius, normalStrokePaint)
                    }

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
                        canvas.drawCircle(bubble.x, bubble.y, drawRadius * 0.6f, powerUpPaint)
                    }
                    if (bubble.bubbleType == BubbleType.NEGATIVE) {
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            color = Color.BLACK
                            strokeWidth = drawRadius * 0.1f
                        }
                        canvas.drawCircle(bubble.x, bubble.y, drawRadius, strokePaint)

                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            textSize = drawRadius * 0.6f
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText("-1", bubble.x, bubble.y + textPaint.textSize / 3, textPaint)

                        val innerCircleRadius = drawRadius * 0.4f
                        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            color = Color.DKGRAY
                            strokeWidth = drawRadius * 0.1f
                        }
                        canvas.drawCircle(bubble.x, bubble.y, innerCircleRadius, innerPaint)
                    }
                } else {
                    Log.w("GameView", "Encountered a null bubble in the list!")
                }
            }

            // Draw bomb (check if active *before* getting details)
            if (currentGame.isBombActive()) {
                val bombData = currentGame.getBombDetailsForDrawing()
                // Update bomb color for flashing effect if near explosion
                bombPaint.color = Color.BLACK
                val elapsedTime = System.currentTimeMillis() - (currentGame.getBombEndTime() - 3000L) // Assuming 3-sec duration
                val flashInterval = 200 // milliseconds
                if (elapsedTime > 2000L && (elapsedTime / flashInterval) % 2L == 0L) { // Corrected: ensure comparison is Long == Long
                    bombPaint.color = Color.RED
                }
                canvas.drawCircle(bombData.first, bombData.second, bombData.third, bombPaint)
            }

            // Draw rectangle
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

            // --- NEW: Draw Spike Traps (with fill and stroke) ---
            if (GameModeStates.isSpikeTrapModeActive) {
                for (spike in currentGame.getSpikeTraps()) {
                    val path = android.graphics.Path()
                    path.moveTo(spike.x + spike.width / 2, spike.y) // Top center
                    path.lineTo(spike.x, spike.y + spike.height)    // Bottom-left
                    path.lineTo(spike.x + spike.width, spike.y + spike.height) // Bottom-right
                    path.close()

                    canvas.drawPath(path, spikeFillPaint)   // Draw the red fill
                    canvas.drawPath(path, spikeStrokePaint) // Draw the black outline
                }
            }

            // Draw score and level
            canvas.drawText("Score: ${currentGame.getScore()}", 50f, 100f, scorePaint)
            canvas.drawText("Level: ${currentGame.getLevel()}", 50f, 150f, levelPaint)
            canvas.drawText("Missed: ${currentGame.getMissedBubbles()}", 50f, 200f, scorePaint)

            // Update game state and request redraw
            currentGame.update(deltaTime) // Pass deltaTime to game update

            if (currentGame.getBubbles().isNotEmpty() || !currentGame.isGameOver() || GameModeStates.isSpikeTrapModeActive) { // Keep drawing if spikes are active
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

                        // --- NEW: Handle Spike Trap Tap First ---
                        if (GameModeStates.isSpikeTrapModeActive) {
                            val spikeTapped = currentGame.handleSpikeTrapTap(x, y)
                            if (spikeTapped) {
                                lastClickTime = currentTime // Debounce for spike taps too
                                return true // Consume the event if a spike was tapped
                            }
                        }

                        // Original bomb and bubble tap logic (only if no spike was tapped, or if you want both to be possible)
                        if (currentGame.isBombActive()  && currentGame.isPointInsideBomb(x, y)) {
                            // Bomb tapped! Stop the bomb and trigger the pop.
                            currentGame.setBombStopped(true)
                            currentGame.popAdjacentBubbles()
                            currentGame.setBombActive(false) // Deactivate the bomb after popping
                            currentGame.redrawListener?.onRedrawRequested()
                            lastClickTime = currentTime // Debounce for bomb taps
                            return true // Consume the touch event
                        } else {
                            // Handle regular clicks (e.g., shooting bubbles)
                            currentGame.processClick(x, y, isSplitModeActive) // Pass the split mode state
                            lastClickTime = currentTime
                        }
                        return true
                    }
                }
                // We can remove the ACTION_MOVE and ACTION_UP logic related to dragging
                // as the bomb will no longer be draggable.
            }
        }
        return super.onTouchEvent(event)
    }
}
