package com.game.bubblepop



import android.media.MediaPlayer
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random





class Game(private val screenWidth: Float, private val screenHeight: Float, private val context: Context) {
    // Public getter for gameActive

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

    private var interstitialAd: InterstitialAd? = null
    private val adUnitId = context.getString(R.string.inter_test)// Replace with your actual AdMob Interstitial Ad Unit ID
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
    private val soundPool: SoundPool
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
        loadInterstitialAd()
        startGameLoop()
    }





    interface AdDismissedListener {
        fun onAdDismissed()
    }

    var adDismissedListener: AdDismissedListener? = null

    private fun loadInterstitialAd() {
        Log.d("AdFlow", "loadInterstitialAd() called.")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                Log.e("AdFlow", "onAdFailedToLoad() called. Error: ${adError.message}, code: ${adError.code}, domain: ${adError.domain}")
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                Log.d("AdFlow", "onAdLoaded() called. Ad instance: $ad")
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdFlow", "onAdDismissedFullScreenContent() called. Ad instance: $interstitialAd")
                        interstitialAd = null
                        loadInterstitialAd() // Load the next ad
                        gameActive = false // Keep game inactive until user clicks
                        adDismissedListener?.onAdDismissed() // Notify the activity
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e("AdFlow", "onAdFailedToShowFullScreenContent() called. Error: ${adError.message}, code: ${adError.code}")
                        interstitialAd = null
                        loadInterstitialAd() // Attempt to load another ad
                        gameActive = true // Resume the game (ad failed to show)
                        // Optionally notify the activity here as well if you want the message
                        // even if the ad failed to show.
                    }

                    override fun onAdShowedFullScreenContent() {
                        decrementMissedBubbles()
                        Log.d("AdFlow", "onAdShowedFullScreenContent() called. Ad instance: $interstitialAd")
                        // Called when ad is shown.
                    }
                }
            }
        })
    }
    private fun showInterstitialAd() {
        Log.d("AdFlow", "showInterstitialAd() called. Ad instance: $interstitialAd")
        if (interstitialAd != null) {
            Log.d("AdFlow", "Interstitial ad is not null, attempting to show.")
            if (context is GamePlay) {
                mainThreadHandler.post { // Wrap the show call in this block
                    try {
                        Log.d("AdFlow", "About to show interstitial ad. Context: ${context::class.java.name}") // Log context
                        interstitialAd?.show(context as GamePlay)
                        Log.d("AdFlow", "Interstitial ad shown successfully.") // Log success
                    } catch (e: Exception) {
                        Log.e("AdFlow", "Error showing interstitial ad: ${e.message}, Context: ${context::class.java.name}, Thread: ${Thread.currentThread().name}") // Log the exception and context and thread
                        gameActive = true
                        addRandomBubble()
                    } finally {
                        Log.d("AdFlow", "Finally block executed after ad show attempt. Thread: ${Thread.currentThread().name}")
                    }
                }
            } else {
                Log.e("AdFlow", "Invalid context to show ad. context = ${context::class.java.name}") // Log context
                gameActive = true
                addRandomBubble()
            }
        } else {
            Log.e("AdFlow", "Interstitial ad is null, cannot show.")
            gameActive = true
            addRandomBubble()
        }
    }


    // ... rest of your Game class ...

    private var powerUpSpawnProbability = 0.25f //was .05f
    private val negativeBubbleSpawnProbability = 0.25f // 3% chance to spawn a negative bubble
    private val negativeBubbleSpeed = 5f // Adjust falling speed
    private val shrinkingSpeedMultiplier = 0.08f // Adjust to make shrinking slower
    // ... existing init and other methods ...
    private val fallingBubbleProbability = 1f //make this 1 so all bubbles fall

    fun addRandomBubble() {
        val initialRadius = Random.nextFloat() * (minBubbleRadius * 0.5f) + (minBubbleRadius * 0.25f)
        val startRadius = initialRadius
        val x = Random.nextFloat() * (screenWidth - 2 * startRadius) + startRadius
        // All bubbles start at the top
        val y = -startRadius

        var chosenBubbleType: BubbleType
        val chosenPowerUpType: PowerUpType?

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
        }
        Log.d("Game", "addRandomBubble: bubbleType = $chosenBubbleType, powerUpType = $chosenPowerUpType")

        val finalY = -startRadius // Ensure bubbles start at the top
        val isShrinking = Random.nextFloat() < shrinkingProbability

        bubbles.add(
            Bubble(
                id = nextBubbleId++,
                radius = startRadius,
                initialRadius = initialBubbleRadius,
                x = x,
                y = finalY,
                lifespan = Random.nextLong(9999, 99999),
                powerUpType = chosenPowerUpType,
                bubbleType = chosenBubbleType,
                isShrinking = isShrinking
            )
        )
    }




    var isCyanRectangleActive = false
        private set
    var cyanRectangleEndTime = 0L

    fun isGreenRectangleEffectActive(): Boolean {
        return isGreenRectangleActive
    }



    fun update() {
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

        val bubblesToRemove = mutableListOf<Bubble>() // List to hold bubbles to be removed

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
                rectangleRiseSpeed = 0.1f //set normal at 0.1f //4.0 or 2.5 for game over tests
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
                        bubble.isRed = true
                        bubble.popLifespanMultiplier = 0.7f
                        increaseDifficulty()
                    }

                    if (bubble.shouldPop()) {
                        bubblesToRemove.add(bubble)
                    }

                    if (bubble.y < screenHeight) {
                        bubble.y += negativeBubbleSpeed;
                    }
                    if (bubble.y >= screenHeight) {
                        bubblesToRemove.add(bubble)
                    }
                } else if (bubble.bubbleType == BubbleType.NEGATIVE) {
                    bubble.y += negativeBubbleSpeed
                    if (bubble.y > screenHeight + bubble.radius) {
                        bubblesToRemove.add(bubble)
                    }
                }
            } catch (e: Exception) {
                Log.e("Game", "Error processing bubble ${bubble.id}: ${e.message}")
                // Optionally handle the error, e.g., remove the problematic bubble
                // val remainingBubbles = bubbles.toMutableList()
                // remainingBubbles.remove(bubble)
                // bubbles = remainingBubbles
            }
        }

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


    }



    fun processClick(clickX: Float, clickY: Float) {
        if (!gameActive) return
        println("bubble clicked")
        println(bubbles)
        // Check for negative bubble click first
        val negativeBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NEGATIVE }
        negativeBubble?.let {
            if (missedBubbles > 0) {
                missedBubbles--
                missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                Log.d("Game", "Clicked -1 bubble! Missed count reduced to $missedBubbles")
                soundPool.play(coinRingSoundId, 1f, 1f, 0, 0, 1f) // Play coin ring sound
                rectangleY += negativeBubbleDescentAmount // Move the rectangle down
            } else {
                Log.d("Game", "Clicked -1 bubble! Missed count already at 0.")
            }
            bubbles.remove(it)
            //removed play short seg()
            redrawListener?.onRedrawRequested()
            return // Exit the function after handling the negative bubble
        }

        // If no negative bubble was clicked, check for power-up bubble
        val powerUpBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.POWER_UP }
        powerUpBubble?.let {
            println("POWER UP")
            it.powerUpType?.let { type ->
                Log.d("Game", "Power-up bubble clicked! Type: $type")
                println("POWER UP")
                println("Power-up bubble clicked! Type: $type")  // Corrected log
                applyPowerUpEffect(type, it.x, it.y)
                bubbles.remove(it)
                //removed play short seg()
                soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                redrawListener?.onRedrawRequested()

                return // Exit after handling power-up
            }
        }

        // If no negative or power-up bubble was clicked, handle normal bubbles
        val clickedNormalBubbles = bubbles.filter { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NORMAL }
        if (clickedNormalBubbles.isNotEmpty()) {
            val removedCount = clickedNormalBubbles.size
            bubbles.removeAll(clickedNormalBubbles)
            if (removedCount > 0) {
                score += removedCount * level
                //removed play short seg()
                soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                if (bubbles.isEmpty()) {
                    levelUp()
                }
                redrawListener?.onRedrawRequested()
            }
        } else if (isDraggingBomb) {
            // Check if the user clicked near the bomb
            val distance = Math.sqrt(Math.pow((clickX - bombX).toDouble(), 2.0) + Math.pow((clickY - bombY).toDouble(), 2.0))
            if (distance < bombRadius) {
                isDraggingBomb = true
                setBombCoordinates(clickX, clickY) // Initialize bomb coordinates
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

        var adFrequency = 10 // Default ad frequency

        if (level > 10) {
            adFrequency = 10
            increaseDifficulty()
        }
        if (level > 20) {
            adFrequency = 20
            increaseDifficulty()
        }
        if (level > 10) {
            currentSpawnInterval = (currentSpawnInterval * 0.9f).toLong().coerceAtLeast(minSpawnInterval)
            currentBubbleGrowthRate *= 1.4f
            currentBubbleLifespan = (currentBubbleLifespan * 0.9f).toLong().coerceAtLeast(minBubbleLifespan)
        }
        levelsSinceAd++
        if (level % adFrequency == 0 && levelsSinceAd >= adFrequency && interstitialAd != null) {
            Log.d("AdFlow", "Attempting to show interstitial ad at level $level (frequency: $adFrequency).")
            gameActive = false
            showInterstitialAd()
            levelsSinceAd = 0
        } else {
            Log.d("AdFlow", "Interstitial ad conditions not met at level $level (frequency: $adFrequency). Ad ready: ${interstitialAd != null}")
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




    interface AdCallback {
        fun showInterstitial()
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




    var adCallback: AdCallback? = null
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
                        //removed play short seg()
                        soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
                        if (bubbles.isEmpty()) {
                            levelUp()
                        }
                    } else {
                        Log.d("Game", "No normal bubbles to pop.")
                    }
                } catch (e: Exception) {
                    Log.e("Game", "Error removing normal bubbles: ${e.message}")
                    // Handle the error, perhaps by creating a new list and assigning it to bubbles
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
                update() // Call update() on the background thread
                // Post the redraw request back to the main thread
                redrawListener?.let {
                    (context as? GamePlay)?.runOnUiThread {
                        it.onRedrawRequested()
                    }
                }
            } catch (e: Exception) {
                Log.e("Game", "Error ingame loop: ${e.message}", e)
            }
        }, 0, 16, TimeUnit.MILLISECONDS) // ~60 FPS, adjust as needed.  Make this a property.
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
}

