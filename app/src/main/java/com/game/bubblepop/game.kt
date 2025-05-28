package com.game.bubblepop



import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.bubblepop.MainActivity.GameModeStates
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


class Game(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private var context: Context,
    private val isOrbitModeActive: Boolean
) {
    // Public getter for gameActive
    object appWideGameData{
        var playerXP: Int=0
        var globalScore: Int=0
    }


    fun isGameActive(): Boolean {
        return gameActive
    }

    // Public setter for gameActive
    fun setGameActive(isActive: Boolean) {
        gameActive = isActive
    }

    interface GameOverListener {
        fun onGameOver(isNewHighScore: Boolean, score: Int)
    }

    interface MissedBubbleChangeListener {
        fun onMissedBubbleCountChanged(newCount: Int)
    }

    interface RedrawListener {
        fun onRedrawRequested()
    }

    var redrawListener: RedrawListener? = null
    var missedBubbleChangeListener: MissedBubbleChangeListener? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    var gameOverListener: GameOverListener? = null // The listener instance
    private fun decrementMissedBubbles() {
        if (missedBubbles > 0) {
            missedBubbles--
            Log.d("Game", "Missed bubbles reduced. Current missed: $missedBubbles")
            missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles) // Notify listener
        }
    }
    private var missedBubbles = 0
    private val missedBubbleThreshold = 10
    private var level = 1
    private var score = 0
    private var bubbleSpawnInterval = 3000L // 3 seconds
    private var baseBubbleGrowthRate = 0.00001f
    private var bubbleGrowthRateIncreasePerLevel = 0.001f
    private var bubbles = mutableListOf<Bubble>()
    private var bubbleIdCounter = 0
    private var nextBubbleId: Int = 0
    private val initialBubbleRadius: Float = 100f
    private val initialBubbleLifespan: Long = 1500
    private val initialSpawnInterval: Long = 1000
    private val initialBubbleGrowthRate: Float = 1.0f
    private val negativeBubbleProbability = 0.2f
    private val powerUpProbability = 0.1f
    private var gameActive = true
    private var currentBubbleGrowthRate: Float = 0.0f
        get() = baseBubbleGrowthRate + (level * bubbleGrowthRateIncreasePerLevel)
    // Game configuration that scales with difficulty
    private val initialBubbles = 1
    private var maxBubbleRadius = 200f // Increased radius for touch targets
    private var minBubbleRadius = 150f
    private val baseSpawnInterval = 1500L
    private val spawnIntervalDecreasePerMissed = 100L
    private var lastUpdateTime = System.currentTimeMillis()
    private var lastSpawnTime = System.currentTimeMillis()
    private var currentSpawnInterval: Long = 0
        get() {
            val decrease = missedBubbles * spawnIntervalDecreasePerMissed
            val newInterval = if (baseSpawnInterval - decrease < 500L) 500L else baseSpawnInterval - decrease
            return newInterval
        }
    private val centralOrbitPoint: PointF = PointF(screenWidth / 2f, screenHeight / 3f) // Adjusted Y for better visibility of orbit
    //Spikes

    private var lastLoopTime: Long = System.currentTimeMillis()
    private val spikeTraps = mutableListOf<SpikeTrap>()
    private var lastSpikeSpawnTime: Long = 0
    private val spikeSpawnInterval = 2000L // Spawn a spike every 2 seconds (adjust as needed)
    private val spikeSpeed = 150f // Pixels per second (adjust as needed)
    private val spikeWidth = 80f // Adjust size as needed
    private val spikeHeight = 80f // Adjust size as needed
    //Spikes
    private var levelsSinceAd = 0
    private val maxAllowedBubbleRadius = 250f // Define your maximum allowed radius
    private val maxSpawnInterval: Long = 4
    private val minSpawnInterval: Long = 1
    private val maxGrowthRate: Float = 100.5f
    private val minGrowthRate: Float = 10.1f
    private val maxBubbleLifespan: Long = 1500
    private val minBubbleLifespan: Long = 1000
    private var currentBubbleLifespan: Long = initialBubbleLifespan
    // Integrate LoopingMusicPlayer
    private val musicPlayer: LoopingMusicPlayer
    private var soundPool: SoundPool

    private val popSoundId: Int
    private val coinRingSoundId: Int
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val shrinkingProbability = 0.2f

    // Red Rectangle properties
    private val rectangleWidth = 2000f
    private val rectangleHeight = 2000000f
    private var rectangleX = (screenWidth - rectangleWidth) / 2
    private var rectangleY = screenHeight + 50 // Start below the screen
    private var rectangleRiseSpeed = 0.1f
    private var isRectangleActive = false
    private val rectangleActivationDelay = 5000L // 5 seconds delay after game starts
    private var rectangleActivationTime = System.currentTimeMillis() + rectangleActivationDelay
    private val negativeBubbleDescentAmount = 75f
    //BOMB handler
    private var bombExploding = false
    private var bombX: Float = 0f
    private var bombY: Float = 0f
    private var bombRadius: Float = 0f
    private var bombEndTime: Long = 0L
    private val bombDuration = 10000L // 2 seconds
    private val bombMaxRadius = 150f

    private var draggingBomb = false
    private var draggedBomb: Bubble? = null
    private var isDraggingBomb = false
    private var bombShrinking = false
    private val bombShrinkSpeed = 2f
    //Other game modes
    var isSplitModeActive = false

    init {
        Log.d("MusicSetup", "Initializing audio...")
        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
        Log.d("MusicSetup", "SoundPool initialized")

        // Load sound effects.
        popSoundId = soundPool.load(context, R.raw.pop, 1)
        Log.d("MusicSetup", "popSoundId loaded: $popSoundId")
        coinRingSoundId = soundPool.load(context, R.raw.ding, 1)
        Log.d("MusicSetup", "coinRingSoundId loaded: $coinRingSoundId")

        // Load and start music
        val gameMusicResourceId = R.raw.tgs
        Log.d("MusicSetup", "Loading music resource: $gameMusicResourceId")
        musicPlayer = LoopingMusicPlayer(context, gameMusicResourceId)
        Log.d("MusicSetup", "LoopingMusicPlayer created")
        musicPlayer.startLooping()

        spawnInitialBubbles()
        startGameLoop()

    }


// ... rest of your Game class ...

    private var powerUpSpawnProbability = 0.25f //was .05f
    private val negativeBubbleSpawnProbability = 0.25f
    private val negativeBubbleSpeed = 5f
    private val shrinkingSpeedMultiplier = 0.08f
    // ... existing init and other methods ...
    private val fallingBubbleProbability = 1f


    fun addRandomBubble() {
        // entry for more power ups mutator
        if (MainActivity.GameModeStates.isPowerUpModeActive == true) {
            powerUpSpawnProbability = 0.4f
        }
        val initialRadius = Random.nextFloat() * (minBubbleRadius * 0.5f) + (minBubbleRadius * 0.25f)
        val startRadius = initialRadius

        // --- REVERTED CHANGE: Random X position ---
        // Bubbles will spawn anywhere across the top of the screen
        val x = Random.nextFloat() * (screenWidth - 2 * startRadius) + startRadius
        // --- END REVERTED CHANGE ---

        // All bubbles start at the top, just off-screen
        val y = -startRadius

        var chosenBubbleType: BubbleType
        val chosenPowerUpType: PowerUpType?
        var setCanSplit = false

        if (Random.nextFloat() < powerUpSpawnProbability) {
            // It's a power-up bubble
            chosenBubbleType = BubbleType.POWER_UP
            chosenPowerUpType = PowerUpType.values().random()
        } else if (Random.nextFloat() < negativeBubbleSpawnProbability) {
            // It's a negative bubble
            chosenBubbleType = BubbleType.NEGATIVE
            chosenPowerUpType = null
        } else {
            // It's a normal bubble
            chosenBubbleType = BubbleType.NORMAL
            chosenPowerUpType = null
            setCanSplit = true //normal bubbles can split
        }
        Log.d("Game", "addRandomBubble: bubbleType = $chosenBubbleType, powerUpType = $chosenPowerUpType")

        val finalY = -startRadius // Ensure bubbles start at the top

        val isShrinking = Random.nextFloat() < shrinkingProbability

        // Decide if this specific bubble should be orbital based on the global mode
        // and a random chance (e.g., 70% of bubbles become orbital if mode is active)
        val shouldThisBubbleBeOrbital = isOrbitModeActive && Random.nextFloat() < 0.7f

        val newBubble = Bubble(
            id = nextBubbleId++,
            radius = startRadius,
            initialRadius = startRadius,
            x = x, // Use the randomized X
            y = finalY, // Use the finalY (top of screen)
            lifespan = Random.nextLong(9999, 99999),
            powerUpType = chosenPowerUpType,
            bubbleType = chosenBubbleType,
            isShrinking = isShrinking,
            canSplit = setCanSplit,
            isOrbitalBubble = shouldThisBubbleBeOrbital // Pass the flag
        )

        // If the bubble is orbital, set its specific orbit parameters
        if (shouldThisBubbleBeOrbital) {
            // Adjust orbital radius and speed to your desired visual effect.
            // These values worked for clear testing and can be fine-tuned.
            val orbitalRadiusForThisBubble = 100f + Random.nextFloat() * 50f // e.g., 100 to 150 pixels radius
            val orbitalSpeedForThisBubble = 1f + Random.nextFloat() * 5f // e.g., 5 to 10 degrees per update

            newBubble.orbitalRadius = orbitalRadiusForThisBubble
            newBubble.orbitalSpeed = orbitalSpeedForThisBubble
            // The bubble's initialX and orbitalCenterY for its own orbit are automatically set
            // in the Bubble constructor based on the 'x' and 'y' passed above.
        }

        bubbles.add(newBubble)
    }

    var isCyanRectangleActive = false
        private set
    var cyanRectangleEndTime = 0L

    fun isGreenRectangleEffectActive(): Boolean {
        return isGreenRectangleActive
    }
    //Square block functions
    private val bubblesToRemove = mutableListOf<Bubble>() // List to hold bubbles to be removed
    // NEW: List to hold SquareBlock objects
    private val squareBlocks = mutableListOf<SquareBlock>()
    private var lastSquareBlockSpawnTime: Long = 0L
    private val squareBlockSpawnInterval = 3000L // Spawn a new block every 3 seconds
    // NEW: Properties for game dimensions
    var gameWidth: Int = 0
        private set
    var gameHeight: Int = 0
        private set
    // NEW: Initialize game dimensions
    fun setGameDimensions(width: Int, height: Int) {
        this.gameWidth = width
        this.gameHeight = height

        rectangleX = (gameWidth - rectangleWidth) / 2f
        rectangleY = gameHeight + 50f // Keep it off-screen, it won't be used
    }
    //TODO start from here and finish block collisions and bubble removal
    fun getSquareBlocks(): List<SquareBlock> {
        return squareBlocks
    }
    private var blocksToRemove = mutableListOf<SquareBlock>()
    fun update(deltaTime: Long) {
        if(GameModeStates.isBlockModeActive){

            val maxRectangleRiseSpeed = 0.000f
            rectangleRiseSpeed =  0.00000f + (level * 0.00f)
            if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                    rectangleRiseSpeed = maxRectangleRiseSpeed
            }
            isRectangleActive=false

            val currentTime = System.currentTimeMillis()
            if (GameModeStates.isBlockModeActive && currentTime - lastSquareBlockSpawnTime > squareBlockSpawnInterval) {
                println("add square block")
                addRandomSquareBlock()
                lastSquareBlockSpawnTime = currentTime
            }
        }
        // --- NEW: Update and clean up Square Blocks ---
        // Iterate backward to safely remove stopped blocks

        val currentRectangleBounds = RectF(rectangleX, rectangleY,
            rectangleX + rectangleWidth,
            rectangleY + rectangleHeight) // Ensure this is defined outside the loop

        for (block in squareBlocks) {
            val blockCurrentBounds = block.bounds // Get the block's bounds *before* potential vertical movement

            // --- Vertical Movement and Stopping ---
            if (!block.isStopped) { // Only move vertically if not currently stopped
                block.moveVertically()
            }

            // Update block's bounds after potential vertical movement for current frame
            val blockNextVerticalBounds = block.bounds

            // Get Red Rectangle's current position for collision checks (using properties directly)
            val redRectangleTop = rectangleY
            val redRectangleBottom = rectangleY + rectangleHeight
            val redRectangleLeft = rectangleX
            val redRectangleRight = rectangleX + rectangleWidth

            // Check for vertical collision (sitting on top of the Red Rectangle)
            // Use a small tolerance for the 'was above' check to handle floating point precision
            val verticalCollisionTolerance = block.speed + 1f // Slightly more than block's speed

            val blockWouldHitRedRectangleVertically =
                blockNextVerticalBounds.bottom >= redRectangleTop && // Block's bottom is at or below rectangle's top
                        blockCurrentBounds.bottom <= redRectangleTop + verticalCollisionTolerance && // Block was just above or touching
                        blockNextVerticalBounds.right > redRectangleLeft && // Horizontal overlap
                        blockNextVerticalBounds.left < redRectangleRight

            if (blockWouldHitRedRectangleVertically) {
                block.y = redRectangleTop - block.size // Snap to top of the Red Rectangle
                block.isStopped = true // Stop vertical movement
                // Blocks move horizontally with the Red Rectangle only if the Red Rectangle moves horizontally,
                // but the Red Rectangle only moves vertically, so no horizontal adjustment here.
            } else if (block.isStopped) {
                // Check if it was previously stopped (on Red Rectangle or ground) but now lost support
                // Use a small tolerance for float comparisons (e.g., 2f)
                val comparisonTolerance = 2f

                val isOnRedRectangleCurrently =
                    abs(blockNextVerticalBounds.bottom - redRectangleTop) < comparisonTolerance && // Check if snapped to rectangle's top within tolerance
                            blockNextVerticalBounds.right > redRectangleLeft &&
                            blockNextVerticalBounds.left < redRectangleRight

                val isOnGroundCurrently = abs(blockNextVerticalBounds.bottom - gameHeight) < comparisonTolerance // Check if snapped to ground within tolerance

                if (isOnRedRectangleCurrently) {
                    // If block is on the red rectangle, it should move up with it
                    // Using the original movement for the red rectangle, applying speed directly per frame.
                    block.y -= rectangleRiseSpeed
                } else if (!isOnGroundCurrently) { // If not on Red Rectangle AND not on ground
                    block.isStopped = false // Allow it to fall again
                }
            }

            // Fallback: Check for collision with the bottom of the screen (ground) if still falling and not on Red Rectangle
            if (!block.isStopped && blockNextVerticalBounds.bottom >= gameHeight) {
                block.y = (gameHeight - block.size).toFloat() // Snap to bottom
                block.isStopped = true // Stop
            }

            // --- Horizontal Collision and Pushing Logic (by the Red Rectangle) ---
            // Blocks should be pushed if they try to enter the Red Rectangle from the sides.
            val horizontalCollisionTolerance = 5f // This tolerance is for vertical overlap check

            // Check if block is vertically within the Red Rectangle's height range (for side collision)
            // AND not currently sitting on top (to avoid pushing it horizontally when it lands)
            val isVerticallyOverlappingForSideCollision =
                blockNextVerticalBounds.bottom > redRectangleTop + horizontalCollisionTolerance && // Block's bottom is below rectangle's top (not on top)
                        blockNextVerticalBounds.top < redRectangleBottom - horizontalCollisionTolerance // Block's top is above rectangle's bottom

            if (isVerticallyOverlappingForSideCollision) {
                val blockLeftEdge = blockNextVerticalBounds.left
                val blockRightEdge = blockNextVerticalBounds.right

                // Determine if there's horizontal overlap
                val horizontalOverlap =
                    (blockRightEdge > redRectangleLeft && blockLeftEdge < redRectangleRight)

                if (horizontalOverlap) {
                    // Decide which side to push the block out from.
                    // Push from the side closest to the block's center.
                    val blockCenterX = blockCurrentBounds.centerX()
                    val rectCenterX = currentRectangleBounds.centerX()

                    if (blockCenterX < rectCenterX) {
                        // Block is to the left of rectangle's center, push it left
                        block.x = redRectangleLeft - block.size
                    } else {
                        // Block is to the right of rectangle's center, push it right
                        block.x = redRectangleRight
                    }
                }
            }
        }
        // --- NEW: Remove blocks marked for removal ---
        if (blocksToRemove.isNotEmpty()) {
            squareBlocks.removeAll(blocksToRemove) // <--- Actual removal happens here
            blocksToRemove.clear() // Clear the list after removal
            redrawListener?.onRedrawRequested() // Request redraw after actual removal
        } // This line likely belongs elsewhere, or is for clearing blocks

// --- NEW: Square Block to Square Block Collision Resolution ---
// This part goes AFTER the loop where each individual block is updated against the environment.
        val numBlocks = squareBlocks.size
        for (i in 0 until numBlocks) {
            val block1 = squareBlocks[i]
            for (j in i + 1 until numBlocks) { // Compare each block with every other block once
                val block2 = squareBlocks[j]

                // Check if the bounding boxes of the two blocks intersect
                if (android.graphics.RectF.intersects(block1.bounds, block2.bounds))  {
                    // Calculate the amount of overlap in both X and Y directions
                    val overlapX = min(block1.bounds.right, block2.bounds.right) - max(block1.bounds.left, block2.bounds.left)
                    val overlapY = min(block1.bounds.bottom, block2.bounds.bottom) - max(block1.bounds.top, block2.bounds.top)

                    // Ensure overlap is positive (meaning they are genuinely overlapping)
                    if (overlapX > 0 && overlapY > 0) {
                        // Resolve collision based on the axis of least penetration (Minimum Translation Vector - MTV)
                        if (overlapX < overlapY) { // Overlap is smaller horizontally, resolve by pushing horizontally
                            // Determine which side to push based on center points
                            if (block1.bounds.centerX() < block2.bounds.centerX()) {
                                // block1 is to the left of block2, push block1 left, block2 right
                                block1.x -= overlapX / 2f
                                block2.x += overlapX / 2f
                            } else {
                                // block1 is to the right of block2, push block1 right, block2 left
                                block1.x += overlapX / 2f
                                block2.x -= overlapX / 2f
                            }
                        } else { // Overlap is smaller vertically, resolve by pushing vertically
                            // Determine which direction to push based on center points
                            if (block1.bounds.centerY() < block2.bounds.centerY()) {
                                // block1 is above block2, push block1 up, block2 down
                                block1.y -= overlapY / 2f
                                block2.y += overlapY / 2f
                                // For true stacking, you'd need more complex logic here
                            } else {
                                // block1 is below block2, push block1 down, block2 up
                                block1.y += overlapY / 2f
                                block2.y -= overlapY / 2f
                            }
                        }
                    }
                }
            }
        }


        // Collision: Stopped Square Blocks popping Bubbles

        // Prepare a list for bubbles that need to be removed (pushed or hit by blocks)
        val bubblesToRemove = mutableListOf<Bubble>()

        // Collision: Stopped Square Blocks popping Bubbles
        Log.d("CollisionDebug", "Checking collision. Number of bubbles: ${bubbles.size}, Number of stopped blocks: ${squareBlocks.count { it.isStopped }}")
        for (block in squareBlocks) {
            if (block.isStopped) { // Only check for stopped blocks
                for (bubble in bubbles) {
                    // Check for intersection between square block (RectF) and bubble (circle)
                    // This is a basic AABB-circle collision check.
                    // More precise: find closest point on rectangle to circle center, then check distance.
                    val closestX = max(block.bounds.left, min(bubble.x, block.bounds.right))
                    val closestY = max(block.bounds.top, min(bubble.y, block.bounds.bottom))

                    val distanceX = bubble.x - closestX
                    val distanceY = bubble.y - closestY
                    val distanceSquared = (distanceX * distanceX) + (distanceY * distanceY)

                    Log.d("CollisionDebug", "Bubble ${bubble.hashCode()} (X:${bubble.x}, Y:${bubble.y}, R:${bubble.radius}) vs Block ${block.hashCode()} (Bounds:${block.bounds}). Closest: (${closestX}, ${closestY}). DistSq:${distanceSquared}, R*R:${bubble.radius * bubble.radius}")

                    if (distanceSquared < (bubble.radius * bubble.radius)) {
                        // Collision detected!
                        if (bubble.bubbleType != BubbleType.NEGATIVE && bubble.bubbleType != BubbleType.POWER_UP) {
                            missedBubbles++ // Increment missed count for non-special bubbles
                            Log.d("Game", "Bubble popped by square block! Missed: $missedBubbles")
                            if (missedBubbles >= missedBubbleThreshold) {
                                isGameOver()
                            }
                        }
                        bubblesToRemove.add(bubble) // Mark bubble for removal
                    }
                }
            }
        }
        if (GameModeStates.isSpikeTrapModeActive) {
            val currentTime = System.currentTimeMillis()

            // Spawn new spikes
            if (currentTime - lastSpikeSpawnTime > spikeSpawnInterval) {
                val spawnX = (Math.random() * (screenWidth - spikeWidth)).toFloat()
                val spawnY = screenHeight.toFloat() // Start at the bottom
                spikeTraps.add(SpikeTrap(spawnX, spawnY, spikeWidth, spikeHeight, spikeSpeed))
                lastSpikeSpawnTime = currentTime
            }

            // Update and remove off-screen spikes
            val spikesToRemove = mutableListOf<SpikeTrap>()
            for (spike in spikeTraps) {
                spike.update(deltaTime)
                // Remove spikes that have gone off-screen (top)
                if (spike.y + spike.height < 0) { // If top of spike is above the screen
                    spikesToRemove.add(spike)
                }
            }
            spikeTraps.removeAll(spikesToRemove)

            // If mutator is inactive, clear any existing spikes
            if(!MainActivity.GameModeStates.isSpikeTrapModeActive) {
                spikeTraps.clear()
            }
        }

        // ... rest of your update logic ...



        if (MainActivity.GameModeStates.isSplitModeActive == true) {
            isSplitModeActive = true
        } else isSplitModeActive = false

        if (!gameActive) return
        musicPlayer.startLooping()
        val currentTime = System.currentTimeMillis()

        // Activate the rectangle after the delay
        if (!isRectangleActive && currentTime >= rectangleActivationTime) {
            isRectangleActive = true
        }

        if (currentTime - lastSpawnTime > currentSpawnInterval) {
            addRandomBubble()
            lastSpawnTime = currentTime
        }



        if (missedBubbles >= missedBubbleThreshold && gameActive) {
            gameActive = false
            println("Game Over! Too many missed bubbles!")
            musicPlayer.stop()
            println("stop the music")
            soundPool.release()
            println("chill the sound")

            // Notify the game over listener
            Log.d("Game", "Game Over condition met. Calling gameOverListener.")
            handleGameOver()
        }
        // Handle green rectangle behavior
        if (isGreenRectangleActive) {
            if (currentTime < greenRectangleEndTime) {
                rectangleY += rectangleRiseSpeed
            } else {
                isGreenRectangleActive = false
                rectangleColor = Color.RED // Reset to default color.
                //TODO add metered rectangle speed as it gets higher it gets slower
                //TODO revert this back after game over tests
                rectangleRiseSpeed = 0.1f
                val maxRectangleRiseSpeed = 0.5f

                if (MainActivity.GameModeStates.gameDifficulty == "Easy") {
                    rectangleRiseSpeed = 0.1f + (level * 0.01f)
                    if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                        rectangleRiseSpeed = maxRectangleRiseSpeed
                    }
                }
                if (MainActivity.GameModeStates.gameDifficulty == "Normal") {
                    rectangleRiseSpeed = 0.1f + (level * 0.02f)
                    val maxRectangleRiseSpeed = 0.3f
                    if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                        rectangleRiseSpeed = maxRectangleRiseSpeed
                    }
                }
                if (MainActivity.GameModeStates.gameDifficulty == "Hard") {
                    rectangleRiseSpeed = 0.3f + (level * 0.02f)
                    val maxRectangleRiseSpeed = 0.4f
                    if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                        rectangleRiseSpeed = maxRectangleRiseSpeed
                    }
                }
                if (GameModeStates.isPowerUpModeActive == true){
                    val maxRectangleRiseSpeed = 1.5f
                    rectangleRiseSpeed =  1.0f + (level * 0.02f)
                    if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                        rectangleRiseSpeed = maxRectangleRiseSpeed
                    }

                }
                if(GameModeStates.isBlockModeActive){
                    val maxRectangleRiseSpeed = 0.000f
                    rectangleRiseSpeed =  0.00000f + (level * 0.00f)
                    if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                        rectangleRiseSpeed = maxRectangleRiseSpeed
                    }
                }
                //set normal at 0.1f //4.0 or 2.5 for game over tests
                redrawListener?.onRedrawRequested()
            }
        } else if (isRectangleActive) {
            rectangleY -= rectangleRiseSpeed
        }

        // Handle bubble updates and collision with the rectangle
        val rectangleRect = RectF(rectangleX, rectangleY, rectangleX + rectangleWidth, rectangleY + rectangleHeight)

        // Iterate over a copy to avoid ConcurrentModificationException
        //TODO fix power up bubbles getting points when hitting rectangle

        for (bubble in bubbles.toList()) {
            try {
                val bubbleRect = RectF(bubble.x - bubble.radius, bubble.y - bubble.radius, bubble.x + bubble.radius, bubble.y + bubble.radius)
                if (GameModeStates.isSpikeTrapModeActive) {
                    var hitBySpike = false
                    for (spike in spikeTraps) {
                        if (spike.collidesWithBubble(bubble.x, bubble.y, bubble.radius)) {
                            bubblesToRemove.add(bubble)
                            hitBySpike = true
                            break // Bubble hit, no need to check other spikes for this bubble
                        }
                    }
                    if (hitBySpike) {
                        continue // Move to the next bubble as this one is destroyed
                    }
                }
                if (isRectangleActive && RectF.intersects(bubbleRect, rectangleRect)) {
                    bubblesToRemove.add(bubble) // Add to removal list
                    if (bubble.bubbleType == BubbleType.NORMAL) {
                        missedBubbles++
                        Log.d("Game", "Normal bubble hit! Missed bubbles increased. Current missed: $missedBubbles")
                        missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                    }
                    continue // Important: Continue to the next bubble
                }
                // Regular bubble movement and shrinking
                if (bubble.bubbleType == BubbleType.NORMAL || bubble.bubbleType == BubbleType.POWER_UP) {
                    if (bubble.y > screenHeight / 2) {
                        bubble.radius -= currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime))) * shrinkingSpeedMultiplier
                        if (bubble.radius <= 0) {
                            bubblesToRemove.add(bubble)
                            continue
                        }
                    } else {
                        bubble.radius += currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime)))
                        if (bubble.radius > maxBubbleRadius) {
                            bubble.radius = maxBubbleRadius
                        }
                    }

                    if (level > 50) {
                        bubble.isRed = false
                        bubble.popLifespanMultiplier = 0.7f
                        increaseDifficulty()
                    }

                    if (bubble.shouldPop()) {
                        bubblesToRemove.add(bubble)
                    }

                    // Removed the simple y/x update here, as it's now handled by bubble.update() based on flags
                    // if (bubble.y < screenHeight) { bubble.y += negativeBubbleSpeed; }
                    // if (bubble.y >= screenHeight) { bubblesToRemove.add(bubble) }

                } else if (bubble.bubbleType == BubbleType.NEGATIVE) {
                    // Negative bubbles should still move down, but their 'update' call handles general movement too
                    bubble.y += negativeBubbleSpeed
                    if (bubble.y > screenHeight + bubble.radius) {
                        bubblesToRemove.add(bubble)
                    }
                }

                // --- THE FIX IS HERE ---
                // Update bubble movement (including chaotic and orbital if enabled)
                // Pass the global chaos mode flag to the bubble's internal flag
                bubble.isChaoticMovementEnabled = MainActivity.GameModeStates.isChaosModeActive
                // Pass the screen dimensions AND the orbital motion toggle state
                bubble.update(screenWidth.toInt(), screenHeight.toInt(), MainActivity.GameModeStates.isOrbitalModeActive)
                // --- END FIX ---

                if (bubble.y >= screenHeight) {  // Check if bubble went off screen after update
                    bubblesToRemove.add(bubble)
                }

            } catch (e: Exception) {
                Log.e("Game", "Error processing bubble ${bubble.id}: ${e.message}")
                // Optionally handle the error, e.g., remove the problematic bubble
                // val remainingBubbles = bubbles.toMutableList()
                // remainingBubbles.remove(bubble)
                // bubbles = remainingBubbles
            }
        }

        bubbles.removeAll(bubblesToRemove) // Remove all marked bubbles outside the loop
        lastUpdateTime = currentTime
        redrawListener?.onRedrawRequested()

        if (isBombActive) {
            val bombEffectedBubbles = mutableListOf<Bubble>()
            if (currentTime <= bombEndTime) {
                val bombRect = RectF(bombX - bombRadius, bombY - bombRadius, bombX + bombRadius, bombY + bombRadius)
                // Iterate over a copy for bomb collision detection
                for (bubble in bubbles.toList()) {
                    try {
                        val bubbleRect = RectF(bubble.x - bubble.radius, bubble.y - bubble.radius, bubble.x + bubble.radius, bubble.y + bubble.radius)
                        if (RectF.intersects(bubbleRect, bombRect)) {
                            bombEffectedBubbles.add(bubble)
                            when (bubble.bubbleType) {
                                BubbleType.NORMAL -> {
                                    score += level
                                    //removed play short seg()
                                    soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                                }
                                BubbleType.POWER_UP -> {
                                    bubble.powerUpType?.let {
                                        Log.d("Game", "Bomb collided with power-up $it at (${bubble.x}, ${bubble.y})")
                                        applyPowerUpEffect(it, bubble.x, bubble.y) // Apply the power-up effect
                                    }
                                }
                                BubbleType.NEGATIVE -> {
                                    if (missedBubbles > 0) {
                                        missedBubbles--
                                        missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                                        Log.d(
                                            "Game",
                                            "Negative bubble hit bomb! Missed count reduced to $missedBubbles"
                                        )
                                        soundPool.play(coinRingSoundId, 1f, 1f, 0, 0, 1f) // Play coin ring sound
                                        rectangleY += negativeBubbleDescentAmount // Move the rectangle down
                                    } else {
                                        Log.d("Game", "Negative bubble hit bomb! Missed count already at 0.")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Game", "Error checking bomb collision with bubble ${bubble.id}: ${e.message}")
                        // Optionally handle the error
                    }
                }
                bubblesToRemove.addAll(bombEffectedBubbles)
            } else {
                // Bomb duration ended
                isBombActive = false
                Log.d("Game", "Bomb deactivated.")
            }
        }
        if (isSlowTimeActive && currentTime >= slowTimeEndTime) {
            isSlowTimeActive = false
            powerUpSpawnProbability = defaultPowerUpProbability
        }

        // Remove all collected bubbles after processing
        try {
            bubbles.removeAll(bubblesToRemove)
        } catch (e: Exception) {
            Log.e("Game", "Error removing bubbles: ${e.message}")
            val remainingBubbles = bubbles.toMutableList()
            remainingBubbles.removeAll(bubblesToRemove)
            bubbles = remainingBubbles
        }

        if (bubbles.isEmpty() && gameActive) {
            levelUp()
        }
        redrawListener?.onRedrawRequested()
        lastUpdateTime = currentTime

        appWideGameData.globalScore=score
    }


    fun processClick(clickX: Float, clickY: Float, isSplitMode: Boolean) {
        if (!gameActive) return
        var bubbleClicked = false
        var clickHandled = false

        // --- NEW: Check for Square Block Tap First ---
        if (handleSquareBlockTapInternal(clickX, clickY)) {
            clickHandled = true
            // No 'return' here, so flow continues, but subsequent 'if (!clickHandled)'
            // blocks will prevent other items from being processed if a block was tapped.
        }


        println("split mode: $isSplitMode")
        // Check for negative bubble click first
        val negativeBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NEGATIVE }
        negativeBubble?.let {
            bubbleClicked = true;
            if (missedBubbles > 0) {
                missedBubbles--
                // missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles) // Fixme: removed listener
                Log.d("Game", "Clicked -1 bubble! Missed count reduced to $missedBubbles")
                soundPool.play(coinRingSoundId, 1f, 1f, 0, 0, 1f) // Play coin ring sound  Fixme: removed soundpool
                rectangleY += negativeBubbleDescentAmount // Move the rectangle down
            } else {
                Log.d("Game", "Clicked -1 bubble! Missed count already at 0.")
            }
            bubbles.remove(it)
            redrawListener?.onRedrawRequested()
            return  // Important:  Only pop one bubble per click
        }

        // If no negative bubble was clicked, check for power-up bubble
        val powerUpBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.POWER_UP }
        powerUpBubble?.let {
            bubbleClicked = true;
            it.powerUpType?.let { type ->
                Log.d("Game", "Power-up bubble clicked! Type: $type")
                applyPowerUpEffect(type, it.x, it.y)
                bubbles.remove(it)
                // soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                redrawListener?.onRedrawRequested()
                return // Exit after handling power-up
            }
        }

        // If no negative or power-up bubble was clicked, handle normal bubbles
        val clickedNormalBubbles = bubbles.filter { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NORMAL }

        if (clickedNormalBubbles.isNotEmpty()) {
            bubbleClicked = true;
            val removedCount = clickedNormalBubbles.size
            bubbles.removeAll(clickedNormalBubbles)
            if (MainActivity.GameModeStates.gameDifficulty=="Easy"){
                score += removedCount * level
            }
            if (MainActivity.GameModeStates.gameDifficulty=="Normal"){
                score += removedCount * level
            }
            if (MainActivity.GameModeStates.gameDifficulty=="Hard"||
                MainActivity.GameModeStates.isSplitModeActive==true||
                MainActivity.GameModeStates.isChaosModeActive==true){
                var scoreBooster=50
                if (MainActivity.GameModeStates.gameDifficulty=="Hard"){
                score += removedCount * (level+scoreBooster)
                }
                if (MainActivity.GameModeStates.isSplitModeActive==true){
                    scoreBooster+=15
                    score += removedCount * (level+scoreBooster)
                }
                if (MainActivity.GameModeStates.isChaosModeActive==true){
                    scoreBooster+=10
                    score += removedCount * (level+scoreBooster)
                }

            }



            soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
            if (isSplitModeActive) { //check if split mode is active.
                for (poppedBubble in clickedNormalBubbles) {
                    if (poppedBubble.canSplit) {
                        val newBubbles = Bubble.createSplitBubbles(poppedBubble)
                        bubbles.addAll(newBubbles)
                    }
                }
            }
            if (bubbles.isEmpty()) {
                levelUp()
            }
            redrawListener?.onRedrawRequested()
        }

        if (!bubbleClicked && isBombActive) {
            // Check if the user clicked near the bomb
            val distance =
                sqrt(Math.pow((clickX - bombX).toDouble(), 2.0) + Math.pow((clickY - bombY).toDouble(), 2.0))
            if (distance < bombRadius) {
                setBombStopped(true) //set the bomb stopped
                popAdjacentBubbles()
                setBombActive(false)
                redrawListener?.onRedrawRequested()
                return
            }
        }
    }

    private var isBombActive = false
    private var isBombStopped = false

    private var isGreenRectangleActive = false
    private var greenRectangleEndTime: Long = 0
    private val greenRectangleDuration = 5000L // 5 seconds duration
    private val greenRectangleDescentSpeed = 0.1f // Slower descent speed
    var rectangleColor: Int = Color.RED  // Default color
    private var isSlowTimeActive = false
    private var slowTimeEndTime: Long = 0
    private val slowTimeDuration = 8000L // Example duration: 8 seconds
    private val increasedPowerUpProbability = 0.15f // Example: 50% chance during slow time
    private val defaultPowerUpProbability = 0.05f
    private var powerUpText: String = ""  // To store the text to display
    var isDisplayingPowerUpText: Boolean = false // Flag to control text display
    private var powerUpTextEndTime: Long = 0 // Time when the text should disappear
    fun showPowerUpText(text: String) {
        powerUpText = text
        isDisplayingPowerUpText = true
        powerUpTextEndTime = System.currentTimeMillis() + 2000
        redrawListener?.onRedrawRequested()
    }

    fun getDisplayingPowerUpText(): Boolean{
        return isDisplayingPowerUpText
    }

    fun getPowerUpText(): String{
        return powerUpText
    }
    fun getPowerUpTextEndTime(): Long{
        return powerUpTextEndTime
    }

    private fun applyPowerUpEffect(type: PowerUpType, powerUpX: Float, powerUpY: Float) {
        when (type) {
            PowerUpType.BOMB -> {
                bombX = powerUpX
                bombY = powerUpY
                bombRadius = 100f // Initial bomb radius
                bombEndTime = System.currentTimeMillis() + bombDuration
                isBombActive = true
                Log.d("Game", "Bomb activated! x=$bombX, y=$bombY, endTime=$bombEndTime, radius=$bombRadius")
                showPowerUpText("Black Hole") // Show "Bomb" text
                redrawListener?.onRedrawRequested()
            }
            PowerUpType.SLOW_TIME -> {
                isSlowTimeActive = true
                slowTimeEndTime = System.currentTimeMillis() + slowTimeDuration
                powerUpSpawnProbability = increasedPowerUpProbability
                showPowerUpText("2x Power Ups") // Show "Slow Time" text

            }
            PowerUpType.EXTRA_LIFE -> {
                rectangleColor = Color.GREEN
                isGreenRectangleActive = true
                greenRectangleEndTime = System.currentTimeMillis() + greenRectangleDuration
                rectangleRiseSpeed = greenRectangleDescentSpeed
                Log.d("Game", "Extra Life activated!  endTime: $greenRectangleEndTime")
                showPowerUpText("Reverse Rectangle") // Show "Extra Life" text
                redrawListener?.onRedrawRequested()
            }
            PowerUpType.GROWTH_STOPPER -> {
                // Only activate if no other effect is active, or if EXTRA_LIFE is not active
                if (!isGreenRectangleActive || (isGreenRectangleActive && greenRectangleEndTime <= System.currentTimeMillis())) {
                    rectangleColor = Color.CYAN //set to cyan
                    isGreenRectangleActive = true
                    isCyanRectangleActive = true
                    greenRectangleEndTime = System.currentTimeMillis() + 5000 // 5 seconds
                    cyanRectangleEndTime = System.currentTimeMillis() + 5000
                    rectangleRiseSpeed = 0f // Stop movement.
                    Log.d("Game", "Growth Stopper activated! endTime: $greenRectangleEndTime")
                    showPowerUpText("Freeze Rectangle") // Show "Growth Stopper"
                    redrawListener?.onRedrawRequested()
                }
            }
            PowerUpType.GREEN_RECTANGLE -> {
                rectangleColor = Color.GREEN
                isGreenRectangleActive = true
                isCyanRectangleActive = false
                greenRectangleEndTime = System.currentTimeMillis() + greenRectangleDuration
                if(MainActivity.GameModeStates.isPowerUpModeActive){
                    greenRectangleEndTime=2000
                }
                Log.d("Game", "Green Rectangle activated!  endTime: $greenRectangleEndTime")
                // Show "Green Rectangle" text
                redrawListener?.onRedrawRequested()
            }
        }
    }





    fun getMissedBubbles(): Int {
        return missedBubbles
    }

    fun levelUp() {
        level++
        score += 100 * level
        increaseDifficulty()
        // Increase difficulty more aggressively.
        currentSpawnInterval = (currentSpawnInterval - 400L).coerceAtLeast(minSpawnInterval)
        currentBubbleGrowthRate *= 1.30f
        currentBubbleLifespan = (currentBubbleLifespan - 400L).coerceAtLeast(minBubbleLifespan)


        // Enforce upper limit for minBubbleRadius (ensure it doesn't exceed max)
        if (minBubbleRadius > maxAllowedBubbleRadius) {
            minBubbleRadius = maxAllowedBubbleRadius * 0.7f // Or some other reasonable proportion
        } else if (minBubbleRadius > maxBubbleRadius) {
            minBubbleRadius = maxBubbleRadius * 0.7f // Keep min consistent with max
        }

        var GamePause=false
        if (GamePause) {
            gameActive = false
            mainThreadHandler.post {
            }
        } else {
            addRandomBubble()

        }
    }


    private fun increaseDifficulty() {
        currentSpawnInterval = (currentSpawnInterval - 200L).coerceAtLeast(minSpawnInterval)
        currentSpawnInterval = currentSpawnInterval.coerceAtMost(maxSpawnInterval)
        currentBubbleGrowthRate = (currentBubbleGrowthRate + 5.0f).coerceAtMost(maxGrowthRate)
        currentBubbleGrowthRate = currentBubbleGrowthRate.coerceAtLeast(minGrowthRate)
        currentBubbleLifespan = (currentBubbleLifespan - 200L).coerceAtLeast(minBubbleLifespan)
        currentBubbleLifespan = currentBubbleLifespan.coerceAtMost(maxBubbleLifespan)
        Log.d("Game", "Difficulty increased! Spawn interval: $currentSpawnInterval, Growth rate: $currentBubbleGrowthRate, Lifespan: $currentBubbleLifespan, Level: $level")
    }


    private fun spawnInitialBubbles() {
        for (i in 0 until initialBubbles) {
            addRandomBubble()
        }
    }





    fun isBombActive(): Boolean {
        return isBombActive
    }

    fun setBombActive(isActive: Boolean) {
        isBombActive = isActive
    }

    fun isBombStopped(): Boolean {
        return isBombStopped
    }

    fun setBombStopped(isStopped: Boolean) {
        isBombStopped = isStopped
        if (isStopped) {
            redrawListener?.onRedrawRequested() // Request redraw when bomb stops
        }
    }




    fun getBubbles(): List<Bubble> {
        return bubbles.toList() // Return a read-only copy
    }

    fun getScore(): Int {
        return score
    }

    fun getLevel(): Int {
        return level
    }

    fun isGameOver(): Boolean {
        return !gameActive
    }
    fun isDraggingBomb(): Boolean {
        return draggingBomb
    }

    fun getBombDetailsForDrawing(): Triple<Float, Float, Float> {
        return Triple(bombX, bombY, bombRadius)
    }
    fun setBombCoordinates(x: Float, y: Float) {
        bombX = x
        bombY = y
    }

    fun setDraggingBomb(dragging: Boolean) {
        draggingBomb = dragging
    }





    fun endGame() {
        gameActive = false
        println("Game Over! Final Score: $score")
        handleGameOver()
        musicPlayer.stop() // Ensure music stops on explicit end game
        soundPool.release() // Release SoundPool resources
    }
    private fun saveHighScore(newScore: Int) {
        val sharedPref = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt("high_score", newScore)
            apply()
        }
        Log.d("Game", "High score saved: $newScore")
    }

    private fun getHighScore(): Int {
        val sharedPref = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) ?: return 0
        val highScore = sharedPref.getInt("high_score", 0)
        Log.d("Game", "Retrieved high score: $highScore")
        return highScore
    }

    private fun handleGameOver() {
        val highScore = getHighScore()
        Log.d("Game", "Current score: $score, High score: $highScore")
        val isNewHighScore = score > highScore
        Log.d("Game", "Is new high score: $isNewHighScore")
        saveHighScore(score)
        gameOverListener?.onGameOver(isNewHighScore, score)
        gameActive = false
    }
    fun popAdjacentBubbles() {
        if (isBombStopped && isBombActive) {
            Log.d("Game", "popAdjacentBubbles() called. Bomb at ($bombX, $bombY) with radius $bombRadius")
            Log.d("Game", "Number of bubbles: ${bubbles.size}")
            val bubblesToPop = mutableListOf<Bubble>()

            // Iterate through a copy of the bubbles list to avoid ConcurrentModificationException
            for (bubble in bubbles.toList()) {
                try {
                    val distance = sqrt((bubble.x - bombX).pow(2) + (bubble.y - bombY).pow(2))
                    Log.d("Game", "Checking bubble ${bubble.id} at (${bubble.x}, ${bubble.y}) with radius ${bubble.radius}, distance to bomb: $distance")
                    if (distance < bombRadius + bubble.radius) {
                        bubblesToPop.add(bubble)
                        Log.d("Game", "Bomb colliding with bubble ${bubble.id}")
                    }
                } catch (e: Exception) {
                    Log.e("Game", "Error while checking bubble ${bubble.id}: ${e.message}")
                    // Optionally handle the error, e.g., remove the problematic bubble
                    // bubbles.remove(bubble)
                }
            }

            val normalBubblesToRemove = bubblesToPop.filter { it.bubbleType == BubbleType.NORMAL }
            if (normalBubblesToRemove.isNotEmpty()) {
                val removedCount = normalBubblesToRemove.size
                Log.d("Game", "Popping $removedCount normal bubbles.")
                try {
                    bubbles.removeAll(normalBubblesToRemove)
                    if (removedCount > 0) {
                        score += removedCount * level
                        soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                        // Level up is checked after all bubbles are processed
                    } else {
                        Log.d("Game", "No normal bubbles to pop.")
                    }
                } catch (e: Exception) {
                    Log.e("Game", "Error removing normal bubbles: ${e.message}")
                    val remainingBubbles = bubbles.toMutableList()
                    remainingBubbles.removeAll(normalBubblesToRemove)
                    bubbles = remainingBubbles
                }
            }

            val powerUpsToRemove = bubblesToPop.filter { it.bubbleType == BubbleType.POWER_UP }
            for (powerUp in powerUpsToRemove) {
                try {
                    powerUp.powerUpType?.let { type ->
                        Log.d("Game", "Bomb triggered power-up: $type at x=${powerUp.x}, y=${powerUp.y}")
                        applyPowerUpEffect(type, powerUp.x, powerUp.y)
                    }
                } catch (e: Exception) {
                    Log.e("Game", "Error applying power-up effect for bubble ${powerUp.id}: ${e.message}")
                }
            }
            try {
                bubbles.removeAll(powerUpsToRemove) // Remove power-ups after applying effect
            } catch (e: Exception) {
                Log.e("Game", "Error removing power-up bubbles: ${e.message}")
                val remainingBubbles = bubbles.toMutableList()
                remainingBubbles.removeAll(powerUpsToRemove)
                bubbles = remainingBubbles
            }

            val negativeBubblesToRemove = bubblesToPop.filter { it.bubbleType == BubbleType.NEGATIVE }
            try {
                bubbles.removeAll(negativeBubblesToRemove)
            } catch (e: Exception) {
                Log.e("Game", "Error removing negative bubbles: ${e.message}")
                val remainingBubbles = bubbles.toMutableList()
                remainingBubbles.removeAll(negativeBubblesToRemove)
                bubbles = remainingBubbles
            }

            // Reset bomb state after attempting to pop
            isBombActive = false
            isBombStopped = false // Ensure bomb stopped is also reset
            redrawListener?.onRedrawRequested()

            // Check for level up after all bubbles have been processed
            if (bubbles.isEmpty() && gameActive) {
                levelUp()
            }
        }
    }
    fun isPointInsideBomb(x: Float, y: Float): Boolean {
        return if (isBombActive) {
            val distanceSquared = (x - bombX).pow(2) + (y - bombY).pow(2)
            distanceSquared <= bombRadius.pow(2)
        } else {
            false
        }
    }
    private fun startGameLoop() {
        executor.scheduleAtFixedRate({
            try {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastLoopTime
                lastLoopTime = currentTime // Update for the next iteration

                update(deltaTime) // Call update() with deltaTime on the background thread
                // Post the redraw request back to the main thread
                redrawListener?.let {
                    // Assuming context can be cast to GamePlay or you have a way to run on UI thread
                    // This part depends on how your MainActivity/GamePlay is structured to receive redraw requests
                    if (context is GamePlay) { // Example: if MainActivity implements GamePlay
                        (context as GamePlay).runOnUiThread {
                            it.onRedrawRequested()
                        }
                    } else if (context is Context) { // Fallback if not GamePlay, might need specific handler
                        // If GameView is directly attached to MainActivity, and MainActivity is the context
                        // you might need a Handler for the main thread or ensure invalidate() is safe.
                        // For a View, invalidate() implicitly runs on UI thread.
                        it.onRedrawRequested() // invalidate() is safe to call from any thread
                    }
                }
            } catch (e: Exception) {
                Log.e("Game", "Error in game loop: ${e.message}", e)
            }
        }, 0, 16, TimeUnit.MILLISECONDS) // ~60 FPS, adjust as needed. Make this a property.
    }

    fun cleanup() {
        executor.shutdown() // Shut down the executor when the game is destroyed
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS) // Wait for termination
        } catch (e: InterruptedException) {
            Log.e("Game", "Error waiting for executor termination: ${e.message}", e)
        }
        soundPool.release()
    }
    fun getBombX(): Float {
        return bombX
    }
    fun stopMusic() {
        Log.d("MusicControl", "Stopping music from Game class.")
        musicPlayer.stop()
    }

    fun startMusic() {
        Log.d("MusicControl", "Starting music from Game class.")
        musicPlayer.startLooping()
    }

    fun releaseSoundPool() {
        Log.d("SoundControl", "Releasing SoundPool from Game class.")
        soundPool?.release()

    }
    fun getBombY(): Float {
        return bombY
    }

    fun getBombRadius(): Float {
        return bombRadius
    }
    // Getter methods for the red rectangle properties
    fun getRectangleX(): Float {
        return rectangleX
    }
    fun getBombEndTime(): Long {
        return bombEndTime
    }
    fun getRectangleY(): Float {
        return rectangleY
    }

    fun getRectangleWidth(): Float {
        return rectangleWidth
    }

    fun getRectangleHeight(): Float {
        return rectangleHeight
    }

    fun isRectangleActive(): Boolean {
        return isRectangleActive
    }

    fun getMissedBubbleThreshold(): Int {
        return missedBubbleThreshold
    }
    // New method to get spike traps for drawing in GameView
    fun getSpikeTraps(): List<SpikeTrap> {
        return spikeTraps
    }

    // New method to handle a tap on a spike trap
    fun handleSpikeTrapTap(tapX: Float, tapY: Float): Boolean {
        if (!GameModeStates.isSpikeTrapModeActive) return false

        val tappedSpikes = mutableListOf<SpikeTrap>()
        var spikeTapped = false
        for (spike in spikeTraps) {
            if (spike.isTapped(tapX, tapY)) {
                tappedSpikes.add(spike)
                spikeTapped = true
                // Play sound effect for spike destruction
                // soundPlayer.play(spikeDestroySoundId, 1f, 1f, 0, 0, 1f) // Pass soundPool or a sound manager to Game class
                // Or if you have a sound manager:
                // gameSoundPlayer.playSpikeDestroySound()
            }
        }
        spikeTraps.removeAll(tappedSpikes) // Remove tapped spikes
        return spikeTapped // Return true if any spike was tapped
    }

    // NEW: Method to add a random square block (called internally by Game.update)
    private fun addRandomSquareBlock() {
        if (gameWidth == 0 || gameHeight == 0) {
            Log.w("Game", "Game dimensions not set, cannot spawn square block.")
            return
        }

        val size = 200f
        val x = (Math.random() * (gameWidth - size)).toFloat()
        val y = -size
        val fillColor = Color.LTGRAY // <--- Changed this to Light Gray!
        val speed = 5f + (Math.random() * 5).toFloat()
        // Pass fillColor to the SquareBlock constructor
        squareBlocks.add(SquareBlock(x, y, size, fillColor, speed))
    }
    private fun handleSquareBlockTapInternal(tapX: Float, tapY: Float): Boolean {
        val tappedBlock = squareBlocks.find { it.bounds.contains(tapX, tapY) }
        if (tappedBlock != null) {
            blocksToRemove.add(tappedBlock) // <--- Block is added to the removal list here
            // Do not call redrawListener here, it will be called after update
            Log.d("Game", "Square block tapped and added to removal list! Score: $score")
            return true // Indicate that a block was tapped
        }
        return false // No block was tapped
    }
    // You'll likely have a similar getBubbles() method for GameView to draw


}


