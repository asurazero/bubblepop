package com.game.bubblepop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path // Correct import for Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.RectF
import androidx.media3.common.util.Log // Using androidx.media3.common.util.Log
import kotlin.math.abs // Correct import for abs
import kotlin.math.sqrt // Correct import for sqrt
import android.graphics.PathMeasure // Correct import for PathMeasure
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import com.game.bubblepop.MainActivity.GameModeStates
import kotlin.math.*

// Data class to hold information about a drawn line for fading
data class DrawnLine(val path: Path, var paint: Paint, var decayTime: Long)

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs), Game.RedrawListener {

    // --- Ink Drawing Mutator Variables ---
    var isDrawingModeActive = false
    var currentInk: Float = 100f // Starting ink level
    val maxInk: Float = 100f // Maximum ink capacity
    val inkDepletionRatePerPixel: Float = 0.01f // Adjusted for testing: Ink depleted per pixel drawn (adjust as needed)
    val inkReplenishAmountPerBubble: Float = 10f // Ink gained per bubble popped (adjust as needed)

    val drawnLines = mutableListOf<DrawnLine>() // List to store active drawn lines
    private var currentDrawingPath: Path? = null // The path currently being drawn
    private var lastTouchX: Float = 0f // Last X coordinate of touch for drawing
    private var lastTouchY: Float = 0f // Last Y coordinate of touch for drawing

    // Paint for drawing lines (ink)
    private val linePaint = Paint().apply {
        color = Color.GREEN // Default ink color - ensure this is visible!
        style = Paint.Style.STROKE
        strokeWidth = 20f // Line thickness - ensure this is thick enough to see!
        strokeCap = Paint.Cap.ROUND // Rounded ends for lines
        strokeJoin = Paint.Join.ROUND // Rounded joints for line segments
        isAntiAlias = true // Smooth lines
    }

    // --- NEW: Paints for the Ink Bar ---
    private val inkBarBackgroundPaint = Paint().apply {
        color = Color.GRAY // Background color of the ink bar
        style = Paint.Style.FILL
    }
    private val inkBarFillPaint = Paint().apply {
        color = Color.GREEN // Fill color of the ink bar
        style = Paint.Style.FILL
    }
    private val inkBarBorderPaint = Paint().apply {
        color = Color.BLACK // Border color of the ink bar
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val inkTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f // Adjust text size as needed
        textAlign = Paint.Align.CENTER
    }

    // --- End Ink Drawing Mutator Variables ---

    // --- NEW: Turret Mode Variables ---
    var isTurretModeActive: Boolean = false // This will be set from your settings
    private val turretBaseRadius = 50f
    private val cannonLength = 150f
    private val cannonThickness = 20f
    private val turretColor = Color.DKGRAY
    private val cannonColor = Color.GRAY
    private val projectileColor = Color.GREEN
    private val projectileRadius = 15f
    private val projectileSpeed = -800f // Pixels per second, negative for upwards

    // --- NEW: Paints for Turret Outlines ---
    private val turretBaseStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f // Thick black outline for the turret base
    }
    private val cannonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = cannonThickness + 4f // Slightly thicker than the cannon fill for a clear outline
        strokeCap = Paint.Cap.ROUND
    }

    // --- NEW: Cannon Rotation Variables ---
    private var currentCannonAngleDegrees: Float = -90f // Initial angle: straight up (-90 degrees from positive X-axis)
    private var targetCannonAngleDegrees: Float = -90f // Target angle: straight up
    private val cannonRotationSpeedDegreesPerSecond: Float = 360f // How fast the cannon rotates (e.g., 360 degrees per second)
    // --- End Turret Mode Variables ---
    // --- End Turret Mode Variables ---
    private val projectiles = mutableListOf<Projectile>() // List to hold active projectiles

    private val projectileSpeedMagnitude = 800f
    // --- NEW: Ammo Counter Variables ---
    var currentAmmo: Float = 10f // Starting ammo level
    val maxAmmo: Float = 10f // Maximum ammo capacity
    val ammoRegenRatePerSecond: Float = 1.0f // Ammo gained per second (e.g., 1 ammo per second)
    val ammoDepletionPerShot: Float = 1f // Ammo depleted per shot
    private val ammoBarBackgroundPaint = Paint().apply {
        color = Color.parseColor("#FF616161") // Darker gray for background
        style = Paint.Style.FILL
    }
    private val ammoBarFillPaint = Paint().apply {
        color = Color.parseColor("#FF4CAF50") // Green for fill
        style = Paint.Style.FILL
    }
    private val ammoBarBorderPaint = Paint().apply {
        color = Color.BLACK // Black border
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val ammoTextPaint = Paint().apply {
        color = Color.WHITE // White text for visibility
        textSize = 30f // Adjust text size as needed
        textAlign = Paint.Align.CENTER
    }
    // --- NEW: Bar dimensions as member variables for consistent access ---
    private val inkBarHeight = 40f
    private val inkBarMargin = 20f
    private val ammoBarHeight = 40f
    private val ammoBarMargin = 20f
    // --- End NEW Bar dimensions ---


    // --- End Turret Mode Variables ---

    // --- NEW: Sound Variables ---
    private lateinit var soundPool: SoundPool
    private var shootSoundId: Int = 0
    // --- End Sound Variables ---
    //Other Game modes
    var isSplitModeActive = false
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // This is the correct place to inform the Game object about its dimensions
        game.setGameDimensions(w, h)
        // If you need to re-initialize or adjust game elements based on new dimensions, do it here.
        // For example, if you want initial bubbles to span the full width/height
        // or if a fixed initial position needs to be relative to the new dimensions.
    }


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
        // NEW: Initialize SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = SoundPool.Builder().setMaxStreams(5).build()
        } else {
            @Suppress("DEPRECATION")
            soundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 0)
        }
        // NEW: Load the sound effect (replace R.raw.shoot_sound with your actual sound resource ID)
        // You will need to add a sound file (e.g., shoot_sound.wav or .mp3) to your res/raw/ directory.
        // For example, if your sound file is named 'shoot_sound.mp3', it should be in `res/raw/shoot_sound.mp3`
        // and you would reference it as `R.raw.shoot_sound`.
        try {
            shootSoundId = soundPool.load(context, R.raw.boom, 1) // Assuming R.raw.shoot_sound exists
        } catch (e: Exception) {
            Log.e("GameView", "Error loading sound: ${e.message}")
            // Handle the case where the sound resource is not found or cannot be loaded
            // Perhaps disable sound effects or use a fallback
        }
    }

    override fun onAttachedToWindow() {
        if(MainActivity.GameModeStates.isSplitModeActive==true){
            isSplitModeActive=true
        }else isSplitModeActive=false
        // --- NEW: Link Turret Mode state from MainActivity ---
        if (GameModeStates.isTurretModeActive) {
            isTurretModeActive = true
        } else {
            isTurretModeActive = false
        }
        super.onAttachedToWindow()
        game.redrawListener = this

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        game.redrawListener = null
        soundPool.release()
    }

    override fun onRedrawRequested() {
        invalidate()
    }
    override fun onReplenishAmmo() { // NEW: Implement the new method
        currentAmmo = maxAmmo
        invalidate() // Request redraw to update the ammo bar
    }
    override fun onReplenishHalfAmmo() { // NEW: Implement the new method for half ammo
        currentAmmo = (currentAmmo + maxAmmo / 2).coerceAtMost(maxAmmo)
        invalidate() // Request redraw to update the ammo bar
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
        //Handles draw mode toggle
        if (GameModeStates.isDrawModeActive){
            isDrawingModeActive=true
        }else isDrawingModeActive=false

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
            checkLineRedRectangleCollision(currentGame)


            val bubblesToDraw = currentGame.getBubbles().toList() // <--- THIS IS THE CHANGE
            for (bubble in bubblesToDraw) {
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
                    val path = Path() // Corrected: Use android.graphics.Path
                    path.moveTo(spike.x + spike.width / 2, spike.y) // Top center
                    path.lineTo(spike.x, spike.y + spike.height)    // Bottom-left
                    path.lineTo(spike.x + spike.width, spike.y + spike.height) // Bottom-right
                    path.close()

                    canvas.drawPath(path, spikeFillPaint)   // Draw the red fill
                    canvas.drawPath(path, spikeStrokePaint) // Draw the black outline
                }
            }
            // --- NEW: Draw all square blocks (retrieved from Game) ---
            val squareBlocksToDraw = currentGame.getSquareBlocks().toList() // <--- ADD THIS SNAPSHOT TOO
            squareBlocksToDraw.forEach { block ->
                block.draw(canvas)
            }

            // --- NEW: Draw active drawing path ---
            currentDrawingPath?.let { path ->
                canvas.drawPath(path, linePaint)
            }

            // --- NEW: Draw and update decaying lines ---
            val linesToRemove = mutableListOf<DrawnLine>()
            for (line in drawnLines) {
                val timeLeft = line.decayTime - currentTime
                if (timeLeft > 0) {
                    // Calculate alpha for fading effect
                    val alpha = (255 * (timeLeft.toFloat() / 3000f)).toInt().coerceIn(0, 255) // 3000ms decay time
                    line.paint.alpha = alpha
                    canvas.drawPath(line.path, line.paint)
                } else {
                    linesToRemove.add(line) // Mark for removal if decayed
                }
            }
            drawnLines.removeAll(linesToRemove) // Remove fully faded lines
            if (isDrawingModeActive) { // Only draw the ink bar if drawing mode is active
                // Use member variables for dimensions
                val inkBarWidth = width - (2 * inkBarMargin)
                val inkBarTop = inkBarMargin // Position at the top

                // Background of the ink bar
                canvas.drawRect(inkBarMargin, inkBarTop, inkBarMargin + inkBarWidth, inkBarTop + inkBarHeight, inkBarBackgroundPaint)

                // Fill of the ink bar based on current ink
                val fillWidth = inkBarWidth * (currentInk / maxInk)
                canvas.drawRect(inkBarMargin, inkBarTop, inkBarMargin + fillWidth, inkBarTop + inkBarHeight, inkBarFillPaint)

                // Border of the ink bar
                canvas.drawRect(inkBarMargin, inkBarTop, inkBarMargin + inkBarWidth, inkBarTop + inkBarHeight, inkBarBorderPaint)

                // Optional: Draw ink percentage text
                val inkPercentage = (currentInk / maxInk * 100).toInt()
                val inkText = "Ink: $inkPercentage%"
                canvas.drawText(inkText, width / 2f, inkBarTop + inkBarHeight / 2f + inkTextPaint.textSize / 3, inkTextPaint)
            }
            // -   -- NEW: Draw turret and projectiles only if turret mode is active ---
            if (isTurretModeActive) {
                drawTurret(canvas)
                drawProjectiles(canvas)
                // Use member variables for dimensions
                val ammoBarWidth = width - (2 * ammoBarMargin)
                val ammoBarTop = if (isDrawingModeActive) {
                    inkBarMargin + inkBarHeight + ammoBarMargin // Below ink bar + margin
                } else {
                    ammoBarMargin // At top margin if ink bar isn't active
                }
                // Background of the ammo bar
                canvas.drawRect(ammoBarMargin, ammoBarTop, ammoBarMargin + ammoBarWidth, ammoBarTop + ammoBarHeight, ammoBarBackgroundPaint)

                // Fill of the ammo bar based on current ammo
                val fillWidth = ammoBarWidth * (currentAmmo / maxAmmo)
                canvas.drawRect(ammoBarMargin, ammoBarTop, ammoBarMargin + fillWidth, ammoBarTop + ammoBarHeight, ammoBarFillPaint)

                // Border of the ammo bar
                canvas.drawRect(ammoBarMargin, ammoBarTop, ammoBarMargin + ammoBarWidth, ammoBarTop + ammoBarHeight, ammoBarBorderPaint)

                // Optional: Draw ammo percentage text
                val ammoPercentage = (currentAmmo / maxAmmo * 100).toInt()
                val ammoText = "Ammo: $ammoPercentage%"
                canvas.drawText(ammoText, width / 2f, ammoBarTop + ammoBarHeight / 2f + ammoTextPaint.textSize / 3, ammoTextPaint)

            }


            // Draw score and level
            canvas.drawText("Score: ${currentGame.getScore()}", 50f, 100f, scorePaint)
            canvas.drawText("Level: ${currentGame.getLevel()}", 50f, 150f, levelPaint)
            canvas.drawText("Missed: ${currentGame.getMissedBubbles()}", 50f, 200f, scorePaint)

            // --- NEW: Draw the Ink Bar ---

            // Update game state and request redraw
            // --- NEW: Update existing projectiles and fire new ones if turret mode is active ---
            // This logic is moved here from the previous `update` method, as your `GameView` calls `game.update`.
            // The `game.update` method will then call `game.redrawListener?.onRedrawRequested()` which triggers `invalidate()`.
            // Update game state and request redraw
            // --- Update existing projectiles (removed automatic firing logic from here) ---
            // Update game state and request redraw
            // --- Update existing projectiles and handle collisions ---
            val projectilesToRemove = mutableListOf<Projectile>()
            val bubblesToPopByProjectile = mutableSetOf<Bubble>() // Use a set to avoid duplicate popping

            for (projectile in projectiles) {
                projectile.update(deltaTime)

                // Check for collision with bubbles if turret mode is active
                if (isTurretModeActive) {
                    val bubblesSnapshot = currentGame.getBubbles().toList() // Get a snapshot to iterate safely
                    for (bubble in bubblesSnapshot) {
                        val distance = sqrt(
                            (projectile.x - bubble.x).pow(2) + (projectile.y - bubble.y).pow(2)
                        )
                        if (distance < projectile.radius + bubble.radius) {
                            // Collision detected!
                            bubblesToPopByProjectile.add(bubble)
                            projectilesToRemove.add(projectile) // Mark projectile for removal
                            break // Only pop one bubble per projectile per frame
                        }
                    }
                }

                if (projectile.isOffScreen(width, height)) { // Pass screen dimensions to isOffScreen
                    projectilesToRemove.add(projectile)
                }
            }
            projectiles.removeAll(projectilesToRemove)

            // Pop bubbles that collided with projectiles
            for (bubble in bubblesToPopByProjectile) {
                currentGame.popBubble(bubble)
            }
            // --- End NEW Turret Update ---
            // --- NEW: Smoothly rotate the cannon ---
            if (isTurretModeActive) {
                val angleDiff = targetCannonAngleDegrees - currentCannonAngleDegrees
                // Normalize angleDiff to be between -180 and 180 for shortest path
                val normalizedAngleDiff = (angleDiff + 540) % 360 - 180 // (x + 360*N + 180) % 360 - 180
                val maxRotationThisFrame = cannonRotationSpeedDegreesPerSecond * (deltaTime / 1000f)

                val rotationAmount = if (abs(normalizedAngleDiff) <= maxRotationThisFrame) {
                    normalizedAngleDiff
                } else if (normalizedAngleDiff > 0) {
                    maxRotationThisFrame
                } else {
                    -maxRotationThisFrame
                }

                currentCannonAngleDegrees += rotationAmount
                // Ensure currentCannonAngleDegrees stays within 0 to 360
                currentCannonAngleDegrees = (currentCannonAngleDegrees + 360) % 360

                // --- NEW: Regenerate ammo ---
                currentAmmo = (currentAmmo + ammoRegenRatePerSecond * (deltaTime / 1000f)).coerceAtMost(maxAmmo)
            }
            // --- End NEW Cannon Rotation ---
            // Update game state and request redraw
            currentGame.update(deltaTime) // Pass deltaTime to game update

            postInvalidateOnAnimation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("GameView", "TOP_LEVEL_TOUCH_EVENT: Action ${event.actionToString()}")
        game?.let { currentGame ->
            val currentTime = System.currentTimeMillis()
            if (isTurretModeActive) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // Only fire if there is ammo
                    if (currentAmmo >= ammoDepletionPerShot) {
                        val targetX = event.x
                        val targetY = event.y
                        fireProjectile(targetX, targetY)
                        currentAmmo -= ammoDepletionPerShot // Deplete ammo
                        invalidate() // Redraw to show the new projectile and updated ammo bar
                        return true // Consume the event
                    } else {
                        Log.d("GameView", "Not enough ammo to fire!")
                        // Optionally, play a "no ammo" sound or display a message
                    }
                }
            }
            // --- NEW: Automatically exit drawing mode if ink is 0 ---
            // This ensures that even if isDrawingModeActive was left true,
            // any new tap (ACTION_DOWN) when ink is 0 will correctly fall
            // through to the normal bubble popping logic.
            if (isDrawingModeActive && currentInk <= 0) {
                isDrawingModeActive = false
                currentDrawingPath = null // Clear the path if ink runs out in drawing mode
                Log.d("GameView", "Ink is 0, automatically exiting drawing mode for tap.")
                // DO NOT return true here. Let the event fall through to the tap handling.
            }

            // --- Handle Drawing Mode only if it's still active (i.e., had ink) ---
            if (isDrawingModeActive) { // This check now only passes if currentInk > 0
                val x = event.x
                val y = event.y

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d("GameView", "ACTION_DOWN received in drawing mode. Current Ink: $currentInk")
                        if (currentInk > 0) {
                            currentDrawingPath = Path()
                            currentDrawingPath?.moveTo(x, y)
                            lastTouchX = x
                            lastTouchY = y
                            Log.d("GameView", "ACTION_DOWN: Path initialized and moved to ($x, $y).")
                        } else {
                            Log.d("GameView", "ACTION_DOWN: Ink is 0, cannot start drawing. (Should have exited drawing mode earlier)")
                            // This return false is a fallback in case the initial 'isDrawingModeActive && currentInk <= 0' check doesn't fully handle it.
                            return false
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Only process MOVE if a path is actively being drawn (currentDrawingPath is not null)
                        // and there is still ink (to ensure the drawing path condition).
                        if (currentDrawingPath != null && currentInk > 0) {
                            val dx = abs(x - lastTouchX)
                            val dy = abs(y - lastTouchY)

                            if (dx >= 4 || dy >= 4) {
                                currentDrawingPath?.lineTo(x, y) // Add to path
                                val distanceMoved = sqrt((dx * dx) + (dy * dy))
                                depleteInk(distanceMoved * inkDepletionRatePerPixel)
                                lastTouchX = x
                                lastTouchY = y

                                // IMPORTANT FIX: Call collision check *here*, after adding to path and depleting ink,
                                // but *before* potentially nulling currentDrawingPath due to ink running out.
                                checkLineBubbleCollisions(currentDrawingPath!!, currentGame) // This is likely your line 424.

                                // Now, after drawing and checking collisions, see if ink ran out
                                if (currentInk <= 0) {
                                    currentDrawingPath = null // Null the path because drawing has ended
                                    isDrawingModeActive = false // Deactivate drawing mode
                                    Log.d("GameView", "ACTION_MOVE: Ink ran out, drawing mode deactivated.")
                                }
                            }
                        } else if (currentInk <= 0) { // This handles cases where ink was already 0 at the start of MOVE
                            // Ensure drawing mode is deactivated and path is cleared if we got here with 0 ink
                            currentDrawingPath = null
                            isDrawingModeActive = false
                            Log.d("GameView", "ACTION_MOVE: Ink ran out, drawing mode deactivated.")
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d("GameView", "ACTION_UP received in drawing mode.")
                        if (currentDrawingPath != null) {
                            val linePaintCopy = Paint(linePaint)
                            drawnLines.add(DrawnLine(currentDrawingPath!!, linePaintCopy, System.currentTimeMillis() + 3000L))
                            checkLineBubbleCollisions(currentDrawingPath!!, currentGame)
                            currentDrawingPath = null
                            Log.d("GameView", "ACTION_UP: Drawing path completed.")
                        }
                    }
                }
                invalidate() // Redraw the view to show the drawing and update ink bar
                return true // Consume the event if actively drawing or tried to draw with ink
            }

            // --- Original Game Mode Touch Handling (this is for regular taps) ---
            // This block is now correctly reached if isDrawingModeActive is false (either by choice, or because ink ran out)
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("GameView", "Calling processClick for regular bubble pop path.") // This log should now appear!
                if (currentTime - lastClickTime > clickDebounceDelay) {
                    val x = event.x
                    val y = event.y

                    // --- Handle Spike Trap Tap First ---
                    if (GameModeStates.isSpikeTrapModeActive) {
                        val spikeTapped = currentGame.handleSpikeTrapTap(x, y)
                        if (spikeTapped) {
                            lastClickTime = currentTime
                            invalidate()
                            return true
                        }
                    }

                    // Handle bomb tap
                    if (currentGame.isBombActive() && currentGame.isPointInsideBomb(x, y)) {
                        currentGame.setBombStopped(true)
                        currentGame.popAdjacentBubbles()
                        currentGame.setBombActive(false)
                        currentGame.redrawListener?.onRedrawRequested()
                        lastClickTime = currentTime
                        invalidate()
                        return true
                    }
                    // Handle regular bubble tap
                    else {
                        val poppedBubble = currentGame.processClick(x, y, isSplitModeActive)
                        if (poppedBubble != null && poppedBubble.bubbleType == BubbleType.NORMAL) {
                            replenishInk(inkReplenishAmountPerBubble)
                        }
                        lastClickTime = currentTime
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // Extension function for easier logging, put outside the class or in a utility file
    fun Int.actionToString(): String {
        return when (this) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            else -> this.toString()
        }
    }
    /**
     * Depletes the current ink level.
     * This will automatically trigger a redraw and ink bar update via invalidate().
     */
    fun depleteInk(amount: Float) {
        currentInk = (currentInk - amount).coerceAtLeast(0f)
        Log.d("GameView", "Ink depleted by $amount, new ink: $currentInk")
        invalidate() // Request redraw to update ink bar
    }

    /**
     * Replenishes the current ink level.
     * This will automatically trigger a redraw and ink bar update via invalidate().
     */
    fun replenishInk(amount: Float) {
        currentInk = (currentInk + amount).coerceAtMost(maxInk)
        Log.d("GameView", "Ink replenished by $amount, new ink: $currentInk")
        invalidate() // Request redraw to update ink bar
    }

    /**
     * Checks for collisions between a drawn line path and existing bubbles.
     * Pops bubbles that intersect the line and replenishes ink.
     * @param linePath The Path representing the drawn line.
     * @param game The Game instance containing the bubbles.
     */
    private fun checkLineBubbleCollisions(linePath: Path, game: Game) {
        val bubblesToPop = mutableSetOf<Bubble>()
        val bubbles = game.getBubbles().toList() // Get a snapshot of bubbles

        val pathPoints = mutableListOf<Pair<Float, Float>>()
        val pm = PathMeasure(linePath, false)
        val length = pm.length
        val step = 5f // Check every 5 pixels along the path for precision
        var distance = 0f
        val pos = floatArrayOf(0f, 0f)

        while (distance <= length) {
            pm.getPosTan(distance, pos, null)
            pathPoints.add(pos[0] to pos[1])
            distance += step
        }
        if (distance - step < length) {
            pm.getPosTan(length, pos, null)
            pathPoints.add(pos[0] to pos[1])
        }

        for (bubble in bubbles) {
            // MODIFIED CONDITION: Now includes BubbleType.MINUS_ONE
            if (!bubblesToPop.contains(bubble) &&
                (bubble.bubbleType == BubbleType.NORMAL ||
                        bubble.bubbleType == BubbleType.POWER_UP ||
                        bubble.bubbleType == BubbleType.NEGATIVE)) { // ADDED THIS TYPE

                val bubbleX = bubble.x
                val bubbleY = bubble.y
                val bubbleRadiusSq = bubble.radius * bubble.radius

                for (i in 0 until pathPoints.size - 1) {
                    val p1x = pathPoints[i].first
                    val p1y = pathPoints[i].second
                    val p2x = pathPoints[i+1].first
                    val p2y = pathPoints[i+1].second

                    val dx = p2x - p1x
                    val dy = p2y - p1y
                    val lenSq = dx * dx + dy * dy

                    val t = if (lenSq != 0f) {
                        (((bubbleX - p1x) * dx + (bubbleY - p1y) * dy) / lenSq).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    val closestX = p1x + t * dx
                    val closestY = p1y + t * dy

                    val distSqFromClosestPoint = (bubbleX - closestX) * (bubbleX - closestX) + (bubbleY - closestY) * (bubbleY - closestY)

                    if (distSqFromClosestPoint <= bubbleRadiusSq) {
                        bubblesToPop.add(bubble)
                        break
                    }
                }
            }
        }

        for (bubble in bubblesToPop) {
            game.popBubble(bubble)

        }

        game.redrawListener?.onRedrawRequested()
    }
    // --- NEW: Draws the turret base and cannon on the canvas. ---
    /**
     * Draws the turret base and cannon on the canvas.
     * @param canvas The canvas to draw on.
     */
    // --- NEW: Draws the turret base and cannon on the canvas. ---
    /**
     * Draws the turret base and cannon on the canvas.
     * @param canvas The canvas to draw on.
     */
    private fun drawTurret(canvas: Canvas) {
        val centerX = width / 2f
        val baseY = height - turretBaseRadius // Turret base sits at the bottom

        // Draw turret base (circle fill)
        paint.color = turretColor
        canvas.drawCircle(centerX, baseY, turretBaseRadius, paint)
        // Draw turret base (circle outline)
        canvas.drawCircle(centerX, baseY, turretBaseRadius, turretBaseStrokePaint)


        // Cannon drawing
        canvas.save() // Save the current canvas state
        canvas.translate(centerX, baseY) // Move origin to the pivot point (center of the turret base)

        // Rotate the canvas. The cannon is drawn pointing upwards (negative Y).
        // atan2 gives angle from positive X. So, if atan2 gives 0 (right), we need to rotate 90 degrees clockwise from straight up.
        // If atan2 gives -90 (up), we need to rotate 0 degrees from straight up.
        // This means we need to rotate by (currentCannonAngleDegrees + 90) degrees.
        canvas.rotate(currentCannonAngleDegrees + 90f)

        // Draw cannon (thick line fill)
        paint.color = cannonColor
        paint.strokeWidth = cannonThickness
        // Draw from (0,0) (which is now centerX, baseY) up along the negative Y axis
        canvas.drawLine(0f, 0f, 0f, -cannonLength, paint)

        // Draw cannon (thick line outline)
        cannonStrokePaint.strokeWidth = cannonThickness + 4f // Ensure outline is visible
        canvas.drawLine(0f, 0f, 0f, -cannonLength, cannonStrokePaint)

        canvas.restore() // Restore the canvas state to remove the translation and rotation
    }

    // --- NEW: Adds a new projectile to the game. ---
    /**
     * Adds a new projectile to the game.
     */
    // --- Adds a new projectile to the game, firing towards targetX, targetY. ---
    /**
     * Adds a new projectile to the game, firing towards the specified target coordinates.
     * @param targetX The X coordinate of the tap.
     * @param targetY The Y coordinate of the tap.
     */
    private fun fireProjectile(targetX: Float, targetY: Float) {
        val turretCenterX = width / 2f
        val turretBaseY = height - turretBaseRadius

        // Calculate the actual start point of the projectile at the tip of the rotated cannon
        val angleRadians = Math.toRadians(currentCannonAngleDegrees.toDouble()).toFloat()
        val projectileStartX = turretCenterX + (cannonLength * cos(angleRadians))
        val projectileStartY = turretBaseY + (cannonLength * sin(angleRadians))

        // Calculate direction vector from the *new* projectile start point to the target
        val dirX = targetX - projectileStartX
        val dirY = targetY - projectileStartY

        // Calculate magnitude (length) of the direction vector
        val magnitude = sqrt(dirX * dirX + dirY * dirY)

        // Update target cannon angle
        if (magnitude > 0) { // Avoid division by zero if tap is exactly at turret center
            targetCannonAngleDegrees = Math.toDegrees(atan2(dirY, dirX).toDouble()).toFloat()
        }

        // Normalize the direction vector and multiply by desired speed magnitude
        val speedX = (dirX / magnitude) * projectileSpeedMagnitude
        val speedY = (dirY / magnitude) * projectileSpeedMagnitude

        projectiles.add(Projectile(projectileStartX, projectileStartY, projectileRadius, speedX, speedY, projectileColor))

        // NEW: Play shoot sound effect
        if (shootSoundId != 0) {
            soundPool.play(shootSoundId, 1f, 1f, 0, 0, 1f)
        } else {
            Log.w("GameView", "Shoot sound not loaded, cannot play.")
        }
    }


    // --- NEW: Draws all active projectiles on the canvas. ---
    /**
     * Draws all active projectiles on the canvas.
     * @param canvas The canvas to draw on.
     */
    private fun drawProjectiles(canvas: Canvas) {
        for (projectile in projectiles) {
            projectile.draw(canvas, paint)
        }
    }

    // Extension function to convert MotionEvent action to string for better logging
    fun MotionEvent.actionToString(): String {
        return when (action) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            else -> action.toString()
        }
    }

    private fun checkLineRedRectangleCollision(game: Game) {
        // Reset the stopped state at the beginning of each check.
        // If it's no longer touching a line, it should be allowed to move again.
        game.setRedRectangleStoppedByLine(false)

        val activeLines = drawnLines.toList() // Use a snapshot to avoid concurrent modification issues

        // Only proceed if there are active lines AND the red rectangle is currently active in the game
        if (activeLines.isEmpty() || !game.isRedRectangleCurrentlyActive()) {
            return
        }

        val redRectBounds = game.getRedRectangleBounds()

        for (drawnLine in activeLines) {
            // Extract points from the drawn line's path, similar to other collision checks
            val pathPoints = mutableListOf<Pair<Float, Float>>()
            val pm = PathMeasure(drawnLine.path, false)
            val length = pm.length
            val step = 5f // Check every 5 pixels along the path for precision
            var distance = 0f
            val pos = floatArrayOf(0f, 0f)

            while (distance <= length) {
                pm.getPosTan(distance, pos, null)
                pathPoints.add(pos[0] to pos[1])
                distance += step
            }
            if (distance - step < length) { // Ensure the very end of the path is also checked
                pm.getPosTan(length, pos, null)
                pathPoints.add(pos[0] to pos[1])
            }

            // Iterate through all segments of the drawn line
            for (i in 0 until pathPoints.size - 1) {
                val p1x = pathPoints[i].first
                val p1y = pathPoints[i].second
                val p2x = pathPoints[i+1].first
                val p2y = pathPoints[i+1].second

                // Use the helper function to check if this line segment intersects the red rectangle
                if (lineRectIntersection(p1x, p1y, p2x, p2y, redRectBounds)) {
                    game.setRedRectangleStoppedByLine(true) // Mark the red rectangle as stopped
                    return // Found a collision, so no need to check further lines or segments for this frame
                }
            }
        }
    }
    private fun lineRectIntersection(x1: Float, y1: Float, x2: Float, y2: Float, rect: RectF): Boolean {
        // Check if either point is inside the rectangle
        if (rect.contains(x1, y1) || rect.contains(x2, y2)) {
            return true
        }

        // Check intersection with each of the four sides of the rectangle
        // Top side
        if (lineSegmentIntersection(x1, y1, x2, y2, rect.left, rect.top, rect.right, rect.top)) return true
        // Bottom side
        if (lineSegmentIntersection(x1, y1, x2, y2, rect.left, rect.bottom, rect.right, rect.bottom)) return true
        // Left side
        if (lineSegmentIntersection(x1, y1, x2, y2, rect.left, rect.top, rect.left, rect.bottom)) return true
        // Right side
        if (lineSegmentIntersection(x1, y1, x2, y2, rect.right, rect.top, rect.right, rect.bottom)) return true

        return false
    }

    // Helper function for line segment - line segment intersection.
// Implements the orientation test approach or similar.
    private fun lineSegmentIntersection(
        p1x: Float, p1y: Float, q1x: Float, q1y: Float, // Segment 1: (p1, q1)
        p2x: Float, p2y: Float, q2x: Float, q2y: Float  // Segment 2: (p2, q2)
    ): Boolean {
        val o1 = orientation(p1x, p1y, q1x, q1y, p2x, p2y)
        val o2 = orientation(p1x, p1y, q1x, q1y, q2x, q2y)
        val o3 = orientation(p2x, p2y, q2x, q2y, p1x, p1y)
        val o4 = orientation(p2x, p2y, q2x, q2y, q1x, q1y)

        // General case
        if (o1 != 0 && o2 != 0 && o3 != 0 && o4 != 0 && o1 != o2 && o3 != o4) { // Modified general case check slightly
            return true
        }

        // Special Cases
        // p1, q1 and p2 are collinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1x, p1y, p2x, p2y, q1x, q1y)) return true
        // p1, q1 and q2 are collinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1x, p1y, q2x, q2y, q1x, q1y)) return true
        // p2, q2 and p1 are collinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2x, p2y, p1x, p1y, q2x, q2y)) return true
        // p2, q2 and q1 are collinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2x, p2y, q1x, q1y, q2x, q2y)) return true

        return false // Doesn't fall in any of the above cases
    }

    // To find orientation of ordered triplet (p, q, r).
// The function returns following values:
// 0 --> p, q and r are collinear
// 1 --> Clockwise
// 2 --> Counterclockwise
    private fun orientation(px: Float, py: Float, qx: Float, qy: Float, rx: Float, ry: Float): Int {
        val `val` = (qy - py) * (rx - qx) - (qx - px) * (ry - qy)
        if (`val` == 0f) return 0 // collinear
        return if (`val` > 0) 1 else 2 // clock or counterclock wise
    }

    // Given three collinear points p, q, r, the function checks if
// point q lies on line segment 'pr'
    private fun onSegment(px: Float, py: Float, qx: Float, qy: Float, rx: Float, ry: Float): Boolean {
        // No more 'kotlin.math.' prefix needed after the import
        return qx <= maxOf(px, rx) && qx >= minOf(px, rx) &&
                qy <= maxOf(py, ry) && qy >= minOf(py, ry)
    }
}
