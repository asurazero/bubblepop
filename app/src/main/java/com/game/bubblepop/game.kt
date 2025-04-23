package com.game.bubblepop

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

interface GameOverListener {
    fun onGameOver(isNewHighScore: Boolean, score: Int)
}

var gameOverListener: GameOverListener? = null
class Game(private val screenWidth: Float, private val screenHeight: Float, private val context: Context) {
        // Public getter for gameActive

    fun isGameActive(): Boolean {
        return gameActive
    }

    // Public setter for gameActive
    fun setGameActive(isActive: Boolean) {
        gameActive = isActive
    }


    interface MissedBubbleChangeListener {
        fun onMissedBubbleCountChanged(newCount: Int)
    }

    interface RedrawListener {
        fun onRedrawRequested()
    }

    var redrawListener: RedrawListener? = null
    var missedBubbleChangeListener: MissedBubbleChangeListener? = null

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
    private val bubbles = mutableListOf<Bubble>()
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
    private val initialBubbles = 5
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




    init {
        // ... (SoundPool initialization and sound loading) ...
        spawnInitialBubbles()
        loadInterstitialAd()
        startGameLoop() // Start the game loop on the executor
        // Initialize SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Adjust as needed
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sound effects
        popSoundId = soundPool.load(context, R.raw.pop, 1) // Assuming 'pop_sound' is in res/raw
        coinRingSoundId = soundPool.load(context, R.raw.pop, 1) // Assuming 'coin_ring' is in res/raw

        val gameMusicResourceId = R.raw.tgs // Replace with your music file
        musicPlayer = LoopingMusicPlayer(context, gameMusicResourceId)
        musicPlayer.setSegmentDuration(1000) // Adjust the duration as needed

        spawnInitialBubbles()
        loadInterstitialAd()
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

                interstitialAd?.show(context as GamePlay)
            } else {
                Log.e("AdFlow", "Invalid context to show ad.")
                gameActive = true
                addRandomBubble()
            }
        } else {
            Log.d("AdFlow", "Interstitial ad is null, cannot show.")
            gameActive = true
            addRandomBubble()
        }
    }
    private val powerUpSpawnProbability = 0.05f
    private val negativeBubbleSpawnProbability = 0.03f // 3% chance to spawn a negative bubble
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

        val powerUpType: PowerUpType? = if (Random.nextFloat() < powerUpSpawnProbability) {
            PowerUpType.values().random()
        } else {
            null
        }

        val bubbleType: BubbleType = if (powerUpType == null && Random.nextFloat() < negativeBubbleSpawnProbability) {
            BubbleType.NEGATIVE
        } else {
            BubbleType.NORMAL
        }

        val finalY = -startRadius // Ensure bubbles start at the top
        val isShrinking = Random.nextFloat() < shrinkingProbability

        bubbles.add(
            Bubble(
                id = nextBubbleId++,
                radius = startRadius,
                initialRadius = initialBubbleRadius,
                x = x,
                y = finalY,
                lifespan = Random.nextLong(3000, 8000),
                powerUpType = powerUpType,
                bubbleType = bubbleType,
                isShrinking = isShrinking
            )
        )
    }



    fun update() {
        if (!gameActive) return

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSpawnTime > currentSpawnInterval) {
            addRandomBubble()
            lastSpawnTime = currentTime
        }

        val poppedBubbles = mutableListOf<Bubble>()
        val removedNegativeBubbles = mutableListOf<Bubble>()
        for (bubble in bubbles) {
            if (bubble.bubbleType == BubbleType.NORMAL || bubble.bubbleType == BubbleType.POWER_UP) {
                if (bubble.y > screenHeight / 2) { // Only shrink if past halfway
                    bubble.radius -= currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime))) * shrinkingSpeedMultiplier
                    if (bubble.radius <= 0) {
                        poppedBubbles.add(bubble)
                        missedBubbles++
                        missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                        continue
                    }
                } else {
                    bubble.radius += currentBubbleGrowthRate * (currentTime - (bubble.creationTime + (currentTime - lastUpdateTime)))
                    // Apply the maximum radius cap
                    if (bubble.radius > maxBubbleRadius) { //added this if statement
                        bubble.radius = maxBubbleRadius
                    }
                }


                // Make bubbles red and pop faster after level 50
                if (level > 50) {
                    bubble.isRed = true
                    bubble.popLifespanMultiplier = 0.7f
                    //Increase the difficulty
                    increaseDifficulty()
                }

                if (bubble.shouldPop()) {
                    poppedBubbles.add(bubble)
                    missedBubbles++
                    missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                }
                //make bubbles always fall
                if (bubble.y < screenHeight) {
                    bubble.y += negativeBubbleSpeed;
                }
                if (bubble.y >= screenHeight) { //if a bubble reaches the bottom
                    poppedBubbles.add(bubble) //add it to the popped bubbles list
                    missedBubbles++
                    missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                }


            } else if (bubble.bubbleType == BubbleType.NEGATIVE) {
                bubble.y += negativeBubbleSpeed
                if (bubble.y > screenHeight + bubble.radius) {
                    removedNegativeBubbles.add(bubble)
                    missedBubbles++
                    missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                }
            } else if (bubble.bubbleType == BubbleType.NORMAL) {
                //  Move regular bubbles downwards
                bubble.y += negativeBubbleSpeed;
            }
        }
        bubbles.removeAll(poppedBubbles)
        bubbles.removeAll(removedNegativeBubbles)
        lastUpdateTime = currentTime

        if (missedBubbles >= missedBubbleThreshold) {
            gameActive = false
            println("Game Over! Too many missed bubbles!")
            musicPlayer.stop()
            soundPool.release()
        }
    }





    fun processClick(clickX: Float, clickY: Float) {
        if (!gameActive) return

        // Check for negative bubble click first
        val negativeBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NEGATIVE }
        negativeBubble?.let {
            if (missedBubbles > 0) {
                missedBubbles--
                missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                Log.d("Game", "Clicked -1 bubble! Missed count reduced to $missedBubbles")
                soundPool.play(coinRingSoundId, 1f, 1f, 0, 0, 1f) // Play coin ring sound
            } else {
                Log.d("Game", "Clicked -1 bubble! Missed count already at 0.")
            }
            bubbles.remove(it)
            musicPlayer.playShortSegment()
            redrawListener?.onRedrawRequested()
            return // Exit the function after handling the negative bubble
        }

        // If no negative bubble was clicked, check for power-up bubble
        val powerUpBubble = bubbles.find { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.POWER_UP }
        powerUpBubble?.let {
            it.powerUpType?.let { type ->
                applyPowerUpEffect(type, it.x, it.y)
                bubbles.remove(it)
                musicPlayer.playShortSegment()
                soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f) // Play pop sound for power-up
                redrawListener?.onRedrawRequested()
                return // Exit after handling power-up
            }
        }

        // If no negative or power-up bubble was clicked, handle normal bubbles
        val clickedNormalBubbles = bubbles.filter { it.isClicked(clickX, clickY) && it.bubbleType == BubbleType.NORMAL }
        if (clickedNormalBubbles.isNotEmpty()) {
            val removedCount = clickedNormalBubbles.size // Get the number of clicked bubbles
            bubbles.removeAll(clickedNormalBubbles)
            if (removedCount > 0) {
                score += removedCount * level
                musicPlayer.playShortSegment()
                soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f) // Play pop sound for normal bubbles
                if (bubbles.isEmpty()) {
                    levelUp()
                }
                redrawListener?.onRedrawRequested()
            }
        }
    }


    private fun applyPowerUpEffect(type: PowerUpType, powerUpX: Float, powerUpY: Float) {
        when (type) {
            PowerUpType.BOMB -> {
                val bombRadius = minOf(screenWidth, screenHeight) * 0.3f
                val bombRadiusSquared = bombRadius * bombRadius
                bubbles.removeAll { bubble ->
                    val dx = bubble.x - powerUpX
                    val dy = bubble.y - powerUpY
                    val distanceSquared = dx * dx + dy * dy
                    distanceSquared < bombRadiusSquared
                }
                score += 5 * level
                println("Bomb activated!")
                redrawListener?.onRedrawRequested() // Notify for redraw after bomb
            }
            PowerUpType.SLOW_TIME -> {
                baseBubbleGrowthRate *= 0.5f
                println("Slow Time activated!")
                redrawListener?.onRedrawRequested() // Notify for redraw
            }
            PowerUpType.EXTRA_LIFE -> {
                missedBubbles = 0
                missedBubbleChangeListener?.onMissedBubbleCountChanged(missedBubbles)
                println("Extra Life gained!")
                redrawListener?.onRedrawRequested() // Notify for redraw
            }
            PowerUpType.GROWTH_STOPPER -> {
                println("Growth Stopper activated!")
                redrawListener?.onRedrawRequested() // Notify for redraw
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
        return sharedPref.getInt("high_score", 0)
    }

    private fun handleGameOver() {
        val highScore = getHighScore()
        val isNewHighScore = score > highScore

        saveHighScore(score)
        gameOverListener?.onGameOver(isNewHighScore, score)
        gameActive = false
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
                Log.e("Game", "Error in game loop: ${e.message}", e)
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
}

