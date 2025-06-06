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
import com.game.bubblepop.MainActivity.GameModeStates.isTurretModeActive
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
        fun onReplenishAmmo()
        fun onReplenishHalfAmmo()
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
    private val blockbreak:Int
    private val popSoundId: Int
    private val coinRingSoundId: Int
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val shrinkingProbability = 0.2f

    // Red Rectangle properties
    private val rectangleWidth = 2000f
    private val rectangleHeight = 2000000f
    private var rectangleX = (screenWidth - rectangleWidth) / 2
    private var rectangleY = screenHeight + 50 // Start below the screen
    private var rectangleRiseSpeed = .1f
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
    private var timeSinceLastBlockSpawn: Long = 0L

    // --- ADD THIS FIXED SPAWN INTERVAL ---
    private var FIXED_BLOCK_SPAWN_INTERVAL_MS: Long = 5000L // Blocks will spawn every 2.5 seconds (adjust as desired)
    // --- END ADDITION ---

    // ... (Your existing 'blocksToSpawnPerEvent' and 'MAX_BLOCKS_TO_SPAWN_PER_EVENT') ...
    private var blocksToSpawnPerEvent: Int = 1
    private val MAX_BLOCKS_TO_SPAWN_PER_EVENT: Int = 5

    private var blocksToRemove = mutableListOf<SquareBlock>()
    private val squareBlocksToAdd = mutableListOf<SquareBlock>() // New: for pending additions
    private val bubblesToAdd = mutableListOf<Bubble>()
    // Add this new property:
    private var isRectangleStoppedByLine: Boolean = false

// ---

    // Add these new public functions to your Game class (anywhere after your properties, before update)
    fun getRedRectangleBounds(): RectF {
        return RectF(rectangleX, rectangleY, rectangleX + rectangleWidth, rectangleY + rectangleHeight)
    }

    fun setRedRectangleStoppedByLine(stopped: Boolean) {
        isRectangleStoppedByLine = stopped
    }

    fun isRedRectangleCurrentlyActive(): Boolean {
        // This getter is needed by GameView to know if the rectangle should be checked
        return isRectangleActive
    }

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
        blockbreak= soundPool.load(context, R.raw.pr, 1)
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

        bubblesToAdd.add(newBubble)
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
    //TODO debug blocks mode some more to make stable
    fun getSquareBlocks(): List<SquareBlock> {
        return squareBlocks
    }

    private fun createRandomSquareBlockInstance(): SquareBlock {
        val size = 200f
        val x = (Math.random() * (gameWidth - size)).toFloat()
        val y = -size
        val fillColor = Color.LTGRAY // <--- Changed this to Light Gray!
        val speed = 5f + (Math.random() * 5).toFloat()
        // Pass fillColor to the SquareBlock constructor
        return SquareBlock(x, y, size, fillColor, speed) // Assuming SquareBlock constructor
    }

    // Rename this for clarity. It now queues a block for addition.
    private fun queueRandomSquareBlock() {
        squareBlocksToAdd.add(createRandomSquareBlockInstance())
    }


    // Queue a bubble for addition

    fun update(deltaTime: Long) {
        val currentBlocksSnapshot = squareBlocks.toList()
        println("block mode check ${GameModeStates.isBlockModeActive}")
        if (squareBlocksToAdd.isNotEmpty()) {
            squareBlocks.addAll(squareBlocksToAdd)
            squareBlocksToAdd.clear()
        }
        if (bubblesToAdd.isNotEmpty()) { // <-- ADD THIS SECTION
            bubbles.addAll(bubblesToAdd)
            bubblesToAdd.clear()
        }



        // 1. Game Mode & Spawning Logic
        if (GameModeStates.isBlockModeActive) {
            timeSinceLastBlockSpawn += deltaTime // Accumulate time

            // Check if enough time has passed based on the fixed interval
            if (timeSinceLastBlockSpawn >= FIXED_BLOCK_SPAWN_INTERVAL_MS) { // Use the fixed interval here
                // Loop to spawn multiple blocks based on blocksToSpawnPerEvent
                for (i in 0 until blocksToSpawnPerEvent) {
                    addRandomSquareBlock() // Call your existing function to add a block
                }
                timeSinceLastBlockSpawn = 0L // Reset the timer for the next spawn event
            }
            Log.d("PhysicsTrace", "--- New Frame ---")
            // These lines related to rectangleRiseSpeed and isRectangleActive seem like remnants or placeholders.
            // If they don't serve a current purpose, you might consider removing them or updating their logic.
            val maxRectangleRiseSpeed = 0.000f // This variable will always be 0.0f given the next line
            rectangleRiseSpeed = 0.00000f + (level * 0.00f) // This calculation will always result in 0.0f
            if (rectangleRiseSpeed > maxRectangleRiseSpeed) {
                rectangleRiseSpeed = maxRectangleRiseSpeed
            }
            isRectangleActive = false // Hardcoding this to false might affect other parts of your game

            val currentTime = System.currentTimeMillis()

        }

        // `currentRectangleBounds` is declared but not used. You can remove this line.
        val currentRectangleBounds = RectF(rectangleX, rectangleY,
            rectangleX + rectangleWidth,
            rectangleY + rectangleHeight)


        // 2. Apply Movement (Gravity) to all active objects
        // Square Blocks: Move vertically if not stopped
        // The inner loop for ground collision was removed from here.
        // Ground collision is now handled in its own dedicated section below.
        Log.d("BlockUpdateDebug", "Processing ${currentBlocksSnapshot.size} blocks this frame.")
        for (block in currentBlocksSnapshot) { // <-- MODIFIED HERE
            if (!block.isStopped) {
                block.moveVertically(deltaTime)
                Log.d("PhysicsTrace", "Block ${block.hashCode()} (Y:${block.y}) - After moveVertically, isStopped: ${block.isStopped}")
            }
        }



        // 3. Collision Resolution Phase

        // 3a. Block vs. Ground Collision (Primary Stopping Mechanism)
        // This loop ensures blocks stop correctly at the bottom of the screen.
        for (block in currentBlocksSnapshot) { // <-- MODIFIED HERE
            if (!block.isStopped && block.bounds.bottom >= gameHeight) {
                block.y = (gameHeight - block.size).toFloat()
                block.isStopped = true
                if (block.getCurrentFillColor() != Color.RED) {
                    block.setFillColor(Color.RED)
                    Log.d("PhysicsTrace", "Block ${block.hashCode()} (Y:${block.y}) - Ground Collision, isStopped: ${block.isStopped}, color set to RED.")
                }
            }
        }

        // 3b. Square Block to Square Block Collision Resolution (for stacking)
        // Use a copy for safe iteration, ensuring correct block interactions without concurrent modification issues.
        val blocksToProcessForStacking = squareBlocks.toList()
        val numBlocks = blocksToProcessForStacking.size
        for (i in 0 until numBlocks) {
            val block1 = blocksToProcessForStacking[i]
            for (j in i + 1 until numBlocks) {
                val block2 = blocksToProcessForStacking[j]

                // Skip if it's the same block or if either block is already marked for removal
                if (block1 == block2 || blocksToRemove.contains(block1) || blocksToRemove.contains(block2)) continue

                if (android.graphics.RectF.intersects(block1.bounds, block2.bounds)) {
                    val overlapX = min(block1.bounds.right, block2.bounds.right) - max(block1.bounds.left, block2.bounds.left)
                    val overlapY = min(block1.bounds.bottom, block2.bounds.bottom) - max(block1.bounds.top, block2.bounds.top)

                    if (overlapX > 0 && overlapY > 0) {
                        if (overlapY <= overlapX) { // Prioritize vertical resolution for proper stacking
                            // Check if block1 is above block2
                            if (block1.bounds.centerY() < block2.bounds.centerY()) {
                                // block1 is above block2: Push block1 up onto block2
                                block1.y = block2.bounds.top - block1.size

                                // CRITICAL CHANGE: Only stop block1 if block2 is itself STOPPED (stable)
                                if (block2.isStopped) {
                                    block1.isStopped = true
                                    if (block1.getCurrentFillColor() != Color.RED) { // Ensure it's not already red
                                        block1.setFillColor(Color.RED) // Turn red when it lands on another stable block
                                        Log.d("PhysicsTrace", "Block ${block1.hashCode()} (Y:${block1.y}) - B2B Collision: STOPPED by STABLE block ${block2.hashCode()}, color set to RED.")
                                    }
                                    Log.d("PhysicsTrace", "Block ${block1.hashCode()} (Y:${block1.y}) - B2B Collision: STOPPED by STABLE block ${block2.hashCode()}.")
                                } else {
                                    // If block2 is not stopped (meaning it's falling), block1 should also continue to fall.
                                    // We still adjust position to prevent over-penetration, but don't set isStopped.
                                    Log.d("PhysicsTrace", "Block ${block1.hashCode()} (Y:${block1.y}) - B2B Collision: Adjusting position over falling block ${block2.hashCode()}.")
                                }
                            } else {
                                // block1 is below block2 (meaning block2 is above block1): Push block2 up onto block1
                                block2.y = block1.bounds.top - block2.size

                                // CRITICAL CHANGE: Only stop block2 if block1 is itself STOPPED (stable)
                                if (block1.isStopped) {
                                    block2.isStopped = true
                                    if (block2.getCurrentFillColor() != Color.RED) { // Ensure it's not already red
                                        block2.setFillColor(Color.RED) // Turn red when it lands on another stable block
                                        Log.d("PhysicsTrace", "Block ${block2.hashCode()} (Y:${block2.y}) - B2B Collision: STOPPED by STABLE block ${block1.hashCode()}, color set to RED.")
                                    }
                                    Log.d("PhysicsTrace", "Block ${block2.hashCode()} (Y:${block2.y}) - B2B Collision: STOPPED by STABLE block ${block1.hashCode()}.")
                                } else {
                                    // If block1 is not stopped (meaning it's falling), block2 should also continue to fall.
                                    Log.d("PhysicsTrace", "Block ${block2.hashCode()} (Y:${block2.y}) - B2B Collision: Adjusting position over falling block ${block1.hashCode()}.")
                                }
                            }
                        } else { // Resolve horizontally
                            if (block1.bounds.centerX() < block2.bounds.centerX()) {
                                block1.x -= overlapX / 2f
                                block2.x += overlapX / 2f
                            } else {
                                block1.x += overlapX / 2f
                                block2.x -= overlapX / 2f
                            }
                            Log.d("PhysicsTrace", "Horizontal push: Block ${block1.hashCode()} and ${block2.hashCode()}.")
                        }
                    }
                }
            }
        }
        // 3c. Bubble vs. Square Block Collision
        // Removed the duplicate bubble collision logic. This section is now the single source.
        // Ensure `bubblesToRemove` is a class member or passed appropriately.
        val bubblesToRemove = mutableListOf<Bubble>() // Keep this line if it's a local variable for this function

        Log.d("CollisionDebug", "Checking collision. Number of bubbles: ${bubbles.size}, Number of stopped blocks: ${squareBlocks.count { it.isStopped }}")

        // Iterate over a copy of bubbles to safely modify the original list.
        val bubblesToProcessForCollision = bubbles.toList()
        for (block in currentBlocksSnapshot)  {
            if (block.isStopped) { // Only stopped blocks can pop bubbles
                for (bubble in bubblesToProcessForCollision) {
                    val closestX = max(block.bounds.left, min(bubble.x, block.bounds.right))
                    val closestY = max(block.bounds.top, min(bubble.y, block.bounds.bottom))

                    val distanceX = bubble.x - closestX
                    val distanceY = bubble.y - closestY
                    val distanceSquared = (distanceX * distanceX) + (distanceY * distanceY)

                    if (distanceSquared < (bubble.radius * bubble.radius)) {
                        // Collision detected!
                        if (bubble.bubbleType != BubbleType.NEGATIVE && bubble.bubbleType != BubbleType.POWER_UP) {
                            missedBubbles++
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


        // 4. Removal Phase: Apply all pending removals to the main lists

        // Remove bubbles from the main list
        if (bubblesToRemove.isNotEmpty()) {
            bubbles.removeAll(bubblesToRemove.toSet()) // Using `toSet()` can be more efficient for multiple removals
            redrawListener?.onRedrawRequested()
        }

        // Remove blocks from the main list (if any were marked for removal)
        if (blocksToRemove.isNotEmpty()) {
            squareBlocks.removeAll(blocksToRemove) // Actual removal happens here
            blocksToRemove.clear() // Clear the list after removal
            //redrawListener?.onRedrawRequested()

            // --- PLACE "LOSS OF SUPPORT" LOGIC HERE ---
            // After blocks have been definitively removed, re-evaluate support for remaining blocks.
            val tolerance =
                30f // INCREASED TOLERANCE HERE. Start with 5f, then try higher if needed.


            var changedStatusInPass = true
            var passCounter = 0 // Add a counter for passes
            while (changedStatusInPass) {
                passCounter++
                changedStatusInPass = false
                Log.d("PhysicsTrace", "--- Loss of Support Pass $passCounter ---")

                for (block in squareBlocks) {
                    val originalIsStopped = block.isStopped
                    Log.d(
                        "PhysicsTrace",
                        "  Checking Block ${block.hashCode()} (Y:${block.y}, isStopped:$originalIsStopped)"
                    )

                    if (originalIsStopped) { // Only check blocks that were previously stopped
                        val isOnGround = abs(block.bounds.bottom - gameHeight) < tolerance
                        Log.d(
                            "PhysicsTrace",
                            "    isOnGround check: ${isOnGround} (bottom: ${block.bounds.bottom}, gameHeight: $gameHeight, tolerance: $tolerance)"
                        )

                        if (!isOnGround) {
                            var isSupportedByAnotherBlock = false
                            Log.d(
                                "PhysicsTrace",
                                "    Block ${block.hashCode()} is NOT on ground. Checking for other support."
                            )


                            val otherBlocksForCollisionCheck = squareBlocks.toList() // <--- Add this nested snapshot
                            for (otherBlock in otherBlocksForCollisionCheck) {
                                if (block == otherBlock) continue

                                // Optimize: otherBlock must be below block's center to be a support
                                if (otherBlock.bounds.top < block.bounds.centerY()) continue

                                val verticalAlignment =
                                    abs(block.bounds.bottom - otherBlock.bounds.top)
                                val horizontalOverlap = max(
                                    0f,
                                    min(
                                        block.bounds.right,
                                        otherBlock.bounds.right
                                    ) - max(block.bounds.left, otherBlock.bounds.left)
                                )

                                Log.d(
                                    "PhysicsTrace",
                                    "    vs OtherBlock ${otherBlock.hashCode()} (Y:${otherBlock.y}, isStopped:${otherBlock.isStopped}): vertAlign: $verticalAlignment, horizOverlap: $horizontalOverlap"
                                )

                                // CRITICAL: A supporting block must ALSO be stopped (stable)
                                if (!otherBlock.isStopped) {
                                    Log.d(
                                        "PhysicsTrace",
                                        "      OtherBlock ${otherBlock.hashCode()} is NOT stopped, cannot provide support."
                                    )
                                    continue // If the other block is falling, it cannot provide stable support.
                                }

                                if (verticalAlignment < tolerance && horizontalOverlap > 0) {
                                    isSupportedByAnotherBlock = true
                                    Log.d(
                                        "PhysicsTrace",
                                        "      FOUND SUPPORT: Block ${block.hashCode()} supported by ${otherBlock.hashCode()}."
                                    )
                                    break // Found stable support, no need to check other blocks
                                }
                            }

                            if (!isSupportedByAnotherBlock) {
                                Log.d(
                                    "PhysicsTrace",
                                    "  !!! Block ${block.hashCode()} (Y:${block.y}) LOST SUPPORT. isStopped -> FALSE. Pass:$passCounter !!!"
                                )
                                block.isStopped = false
                                changedStatusInPass = true
                                // This next log is from your original code, keep it too
                                Log.d(
                                    "GameLogic",
                                    "Block ${block.hashCode()} (Y:${block.y}) lost support and will fall."
                                )
                            } else {
                                Log.d(
                                    "PhysicsTrace",
                                    "    Block ${block.hashCode()} REMAINS STOPPED (supported)."
                                )
                            }
                        }
                    }
                }
            }
            Log.d("PhysicsTrace", "--- END LOSS OF SUPPORT CHECK (Total passes: $passCounter) ---")
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

        // --- UNIFIED RECTANGLE MOVEMENT AND STATE MANAGEMENT ---

// 1. Handle Rectangle Activation (This part remains the same)
// The rectangle becomes active after a delay, if it's not already active.
        if (!isRectangleActive && currentTime >= rectangleActivationTime) {
            isRectangleActive = true
            // Log for debugging:
            Log.d("RectangleDebug", "Rectangle ACTIVATED at $currentTime")
        }

// 2. Main Movement Logic: Only move if active AND not stopped by a line
        if (isRectangleActive && !isRectangleStoppedByLine) {
            // Log for debugging:
            Log.d("RectangleDebug", "DEBUG: deltaTime used: $deltaTime")
            Log.d("RectangleDebug", "DEBUG: rectangleRiseSpeed used: $rectangleRiseSpeed")
            Log.d("RectangleDebug", "Rectangle movement block entered. isGreenRectangleActive: $isGreenRectangleActive")

            // If the rectangle is currently in its "green" state:
            if (isGreenRectangleActive) {
                // Check if the green state duration is still active
                if (currentTime < greenRectangleEndTime) {
                    // Move DOWNWARDS (Y increases) when green.
                    // Ensure deltaTime is used for frame-rate independence.
                    rectangleY += rectangleRiseSpeed * (deltaTime / 1000f) // Assuming rectangleRiseSpeed is px/sec
                    Log.d("RectangleDebug", "Green moving DOWN. Y: $rectangleY")
                } else {
                    // Green state has ended: Transition back to the "red" (default active) state
                    isGreenRectangleActive = false
                    rectangleColor = Color.RED // Reset to default color (assuming you draw with this color)
                    Log.d("RectangleDebug", "Green state ENDED. Transitioning to RED.")

                    // Re-calculate rectangleRiseSpeed for the "red" state based on difficulty/modes.
                    // This is the extensive speed calculation logic you already have.
                    val maxRectangleRiseSpeedDefault = 0.5f // Default max if no specific mode matches
                    var currentMaxSpeed = maxRectangleRiseSpeedDefault

                    rectangleRiseSpeed = 0.1f // Base speed when resetting to red state

                    if (MainActivity.GameModeStates.gameDifficulty == "Easy") {
                        rectangleRiseSpeed = 5.0f + (level * 0.1f)
                        currentMaxSpeed = 15.0f
                    } else if (MainActivity.GameModeStates.gameDifficulty == "Normal") {
                        rectangleRiseSpeed = 10.1f + (level * 0.1f)
                        currentMaxSpeed = 25.0f // From your previous snippet for Normal
                    } else if (MainActivity.GameModeStates.gameDifficulty == "Hard") {
                        rectangleRiseSpeed = 15.0f + (level * 0.2f)
                        currentMaxSpeed = 30.0f // From your previous snippet for Hard
                    }



                    if (GameModeStates.isBlockModeActive) {
                        currentMaxSpeed = 0.000f // Your existing logic sets speed to 0 in BlockMode
                        rectangleRiseSpeed = 0.00000f + (level * 0.00f) // This will be 0
                        // **IMPORTANT:** If you want the rectangle to move in Block Mode, you MUST
                        // change how its speed is set here, and also ensure `isRectangleActive`
                        // isn't hardcoded to false elsewhere in `Game.kt` when Block Mode is active.
                    }

                    // Apply the max speed limit for the calculated speed
                    if (rectangleRiseSpeed > currentMaxSpeed) {
                        rectangleRiseSpeed = currentMaxSpeed
                    }
                    redrawListener?.onRedrawRequested() // Request redraw after speed/color change
                    Log.d("RectangleDebug", "New RED state speed: $rectangleRiseSpeed")
                }
            } else {
                // If the rectangle is NOT green (i.e., it's in its "red" or default active state):
                // It should move UPWARDS (Y decreases).
                rectangleY -= rectangleRiseSpeed * (deltaTime / 1000f) // Assuming rectangleRiseSpeed is px/sec
                Log.d("RectangleDebug", "Red moving UP. Y: $rectangleY")
            }
        } else {
            // Log why movement is not happening
            if (isRectangleActive) {
                Log.d("RectangleDebug", "Rectangle is ACTIVE but STOPPED by line: $isRectangleStoppedByLine")
            } else {
                Log.d("RectangleDebug", "Rectangle is INACTIVE. ActivationTime: $rectangleActivationTime, CurrentTime: $currentTime")
                // Check for the conflicting `isRectangleActive = false` line mentioned in previous responses.
            }
        }

// --- END UNIFIED RECTANGLE MOVEMENT AND STATE MANAGEMENT ---



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

        // Handle bubble updates and collision with the rectangle
        val rectangleRect = RectF(rectangleX, rectangleY, rectangleX + rectangleWidth, rectangleY + rectangleHeight)

        // Iterate over a copy to avoid ConcurrentModificationException


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
                    // --- NEW: Add this conditional check for turret mode ---
                    // --- NEW/MODIFIED LOGIC FOR MISSED BUBBLES ---
                    // Check if the bubble has gone off the bottom of the screen
                    if (bubble.y - bubble.radius > gameHeight&&isTurretModeActive) { // Use gameHeight for the bottom boundary
                        if (bubble.bubbleType == BubbleType.NORMAL) {
                            missedBubbles++ // Increment missed count only for normal/power-up bubbles
                            Log.d("Game", "Bubble missed! Total missed: $missedBubbles") // Log for debugging
                        }
                        bubblesToRemove.add(bubble) // Mark for removal
                        continue // Skip further processing for this bubble
                    }
                    if (!isTurretModeActive) { // Only shrink if turret mode is NOT active
                        if (bubble.y > screenHeight / 2) {
                            bubble.radius -= currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime))) * shrinkingSpeedMultiplier
                            if (bubble.radius <= 0) {
                                bubblesToRemove.add(bubble)
                                continue // Skip to the next bubble as this one is removed
                            }
                        } else {
                            bubble.radius += currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime)))
                            if (bubble.radius > maxBubbleRadius) {
                                bubble.radius = maxBubbleRadius
                            }
                        }
                    }
                    // --- END NEW conditional check ---


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
        if (bubblesToRemove.isNotEmpty()) {
            bubbles.removeAll(bubblesToRemove.toSet())
            bubblesToRemove.clear()
        }
        if (blocksToRemove.isNotEmpty()) {
            squareBlocks.removeAll(blocksToRemove)
            blocksToRemove.clear()
        }
        // ... and then request redraw
        redrawListener?.onRedrawRequested()

        lastUpdateTime = currentTime

        appWideGameData.globalScore=score
    }
    fun popBubble(bubble: Bubble) {
        // Ensure the bubble exists in the current bubbles list before attempting to remove it
        if (bubbles.remove(bubble)) {
            when (bubble.bubbleType) {
                BubbleType.NORMAL -> {
                    // Calculate score based on difficulty and mode
                    var baseScore = 1 // Default score per normal bubble
                    var scoreMultiplier = level

                    if (GameModeStates.gameDifficulty == "Hard") {
                        scoreMultiplier += 50
                    }
                    if (GameModeStates.isSplitModeActive) {
                        scoreMultiplier += 25 // Additional bonus for split mode
                    }
                    if (GameModeStates.isChaosModeActive) {
                        scoreMultiplier += 10 // Additional bonus for chaos mode
                    }
                    score += baseScore * scoreMultiplier

                    // Play pop sound for normal bubbles
                    soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)

                    // Handle split mode for normal bubbles
                    if (GameModeStates.isSplitModeActive && bubble.canSplit) {
                        val newBubbles = Bubble.createSplitBubbles(bubble)
                        bubbles.addAll(newBubbles)
                    }

                    // Activate power-up if the normal bubble contained one
                    bubble.powerUpType?.let { powerUp ->
                        // Pass bubble's coordinates if applyPowerUpEffect needs them
                        applyPowerUpEffect(powerUp, bubble.x, bubble.y)
                    }
                }
                BubbleType.NEGATIVE -> {
                    if (missedBubbles > 0) {
                        missedBubbles--
                        soundPool.play(coinRingSoundId, 1f, 1f, 0, 0, 1f) // Play coin ring sound
                        rectangleY += negativeBubbleDescentAmount // Move the rectangle down
                    }
                    score = (score - 1).coerceAtLeast(0) // Decrease score, ensure it doesn't go below 0
                }
                BubbleType.POWER_UP -> {
                    // Power-up bubbles are handled by their powerUpType,
                    // but you might still want to add a base score for popping them
                    score += 5 // Example score for power-up bubble
                    soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                    bubble.powerUpType?.let { powerUp ->
                        // Pass bubble's coordinates for power-up activation
                        applyPowerUpEffect(powerUp, bubble.x, bubble.y)
                    }
                }
            }

            // After popping, check if all bubbles are cleared for level up
            if (bubbles.isEmpty()) {
                levelUp()
            }
            redrawListener?.onRedrawRequested() // Request redraw after state change
        }
    }

    fun processClick(clickX: Float, clickY: Float, isSplitMode: Boolean): Bubble? { // Changed return type to Bubble?
        if (!gameActive) return null // Return null if game is not active

        // Check for Square Block Tap First
        if (handleSquareBlockTapInternal(clickX, clickY)) {
            return null // Consume the click if a block was tapped, no bubble popped
        }

        // Check for negative bubble click first
        val negativeBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NEGATIVE }
        negativeBubble?.let {
            popBubble(it) // Use the popBubble function
            return it  // Return the popped negative bubble
        }

        // If no negative bubble was clicked, check for power-up bubble
        val powerUpBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.POWER_UP }
        powerUpBubble?.let {
            popBubble(it) // Use the popBubble function
            return it // Return the popped power-up bubble
        }

        // If no negative or power-up bubble was clicked, handle normal bubbles
        val clickedNormalBubbles = bubbles.filter { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NORMAL }

        if (clickedNormalBubbles.isNotEmpty()) {
            val firstPoppedBubble = clickedNormalBubbles.first() // Get the first popped bubble to return
            for (bubble in clickedNormalBubbles) {
                popBubble(bubble) // Use the popBubble function for each
            }
            redrawListener?.onRedrawRequested()
            return firstPoppedBubble // Return one of the popped normal bubbles
        }

        // Original bomb handling remains outside as it's not a bubble pop
        if (isBombActive) {
            // Check if the user clicked near the bomb
            val distance =
                sqrt(Math.pow((clickX - bombX).toDouble(), 2.0) + Math.pow((clickY - bombY).toDouble(), 2.0))
            if (distance < bombRadius) {
                setBombStopped(true) //set the bomb stopped
                popAdjacentBubbles() // This will now use the new popBubble internally
                setBombActive(false)
                redrawListener?.onRedrawRequested()
                return null // Bomb action doesn't return a specific bubble
            }
        }
        return null // No bubble was clicked or handled
    }


    private var isBombActive = false
    private var isBombStopped = false

    private var isGreenRectangleActive = false
    private var greenRectangleEndTime: Long = 0
    private val greenRectangleDuration = 5000L // 5 seconds duration
    private val greenRectangleDescentSpeed = 10.0f // Slower descent speed
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
                if(!MainActivity.GameModeStates.isTurretModeActive) {
                    showPowerUpText("Reverse Rectangle") // Show "Extra Life" text
                }

                if (MainActivity.GameModeStates.isTurretModeActive) {
                    showPowerUpText("Full Ammo")
                    redrawListener?.onReplenishAmmo() // Call the new method on GameView
                    Log.d("Game", "Ammo replenished due to Extra Life power-up!")
                }
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
                // --- NEW LOGIC FOR HALF AMMO REPLENISHMENT ---
                if (MainActivity.GameModeStates.isTurretModeActive) {

                    redrawListener?.onReplenishHalfAmmo() // Assuming you'll add this to GameView.kt
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
        //Blocks mode difficulty management
        if(MainActivity.GameModeStates.isBlockModeActive){
            if(GameModeStates.gameDifficulty=="Easy"){
                var spawnRateMax=4000L
                var levelConv1=level*2
                var levelConv2=levelConv1.toLong()
                FIXED_BLOCK_SPAWN_INTERVAL_MS-=levelConv2
                if(FIXED_BLOCK_SPAWN_INTERVAL_MS<spawnRateMax){
                    FIXED_BLOCK_SPAWN_INTERVAL_MS=spawnRateMax
                }
                println("BSR Block Spawn Interval: $FIXED_BLOCK_SPAWN_INTERVAL_MS")
            }
            if(GameModeStates.gameDifficulty=="Normal"){
                var spawnRateMax=3000L
                var levelConv1=level*6
                var levelConv2=levelConv1.toLong()
                FIXED_BLOCK_SPAWN_INTERVAL_MS-=levelConv2
                if(FIXED_BLOCK_SPAWN_INTERVAL_MS<spawnRateMax){
                    FIXED_BLOCK_SPAWN_INTERVAL_MS=spawnRateMax
                }
            }
            if(GameModeStates.gameDifficulty=="Hard") {
                var spawnRateMax=2000L
                var levelConv1=level*8
                var levelConv2=levelConv1.toLong()
                FIXED_BLOCK_SPAWN_INTERVAL_MS-=levelConv2
                if(FIXED_BLOCK_SPAWN_INTERVAL_MS<spawnRateMax){
                    FIXED_BLOCK_SPAWN_INTERVAL_MS=spawnRateMax
                }
            }
        }
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
    // This function needs to be updated to use popBubble for consistency
    fun popAdjacentBubbles() {
        val bubblesToPop = mutableListOf<Bubble>()
        for (bubble in bubbles) {
            val distance = sqrt(
                Math.pow((bubble.x - bombX).toDouble(), 2.0) +
                        Math.pow((bubble.y - bombY).toDouble(), 2.0)
            )
            if (distance < bombRadius + bubble.radius) { // Check if bubble is within bomb radius
                bubblesToPop.add(bubble)
            }
        }
        for (bubble in bubblesToPop) {
            popBubble(bubble) // Use the new popBubble function
        }
        // No need for bubbles.removeAll(bubblesToPop) here as popBubble handles removal
        // No need for score updates here as popBubble handles it
        // No need for sound here as popBubble handles it
        // No need for level up check here as popBubble handles it
        redrawListener?.onRedrawRequested()
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
        Log.d("BlockSpawnDebug", "SquareBlocks count: ${squareBlocks.size}. New block added. Last block Y: ${squareBlocks.lastOrNull()?.y}")

    }
    private fun handleSquareBlockTapInternal(tapX: Float, tapY: Float): Boolean {
        var blockTapEffectVolume: Float = 1.0f
        // You should iterate over a copy of squareBlocks if 'squareBlocks'
        // itself is being iterated over elsewhere in the same update cycle
        // to prevent ConcurrentModificationException.
        // However, if 'find' doesn't cause issues, this is fine.
        val tappedBlock = squareBlocks.find { it.bounds.contains(tapX, tapY) }
        if (tappedBlock != null) {
            blocksToRemove.add(tappedBlock) // Block is added to the removal list here
            // Update the score when a block is tapped
            score += 50 * level // Assuming 10 points per block, adjust as needed
            soundPool.play(blockbreak, blockTapEffectVolume, blockTapEffectVolume, 0, 0, 1f)
            Log.d("Sound", "Played block tap sound for block ${tappedBlock.hashCode()}.")
            // --- END NEW ---

            // Do not call redrawListener here, it will be called after update
            Log.d("Game", "Square block tapped and added to removal list! Score: $score")
            return true // Indicate that a block was tapped
        }
        return false // No block was tapped
    }

    // You'll likely have a similar getBubbles() method for GameView to draw


}


