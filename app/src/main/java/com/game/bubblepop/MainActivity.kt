package com.game.bubblepop

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.setConsent
import java.io.Serializable

//  ScoreListener is in interfaces.kt

class MainActivity : AppCompatActivity(), ScoreListener {
    // GameModeStates object to hold game mode states
    object GameModeStates {
        var debugMode = false
        var isChaosModeActive = false
        var isSplitModeActive = false
        var isPowerUpModeActive = false
        var gameDifficulty = "Normal"
        var  unlockedMutators = mutableSetOf<String>("No Mutator")
        var unlockedForTesting = mutableSetOf("No Mutator", "Split", "Chaos")

        // Function to get available mutators based on level
        fun getAvailableMutators(level: Int, levelUpThresholds: Map<Int, String>): Set<String> {
            val availableMutators = mutableSetOf("No Mutator") // Always include "No Mutator"
            for ((unlockLevel, mutatorName) in levelUpThresholds) {
                if (level >= unlockLevel) {
                    availableMutators.add(mutatorName)
                    unlockedMutators=availableMutators

                }
            }
            return availableMutators
        }
    }
    private lateinit var umpConsentButton: ImageView
    private lateinit var consentInformation: ConsentInformation
    private lateinit var umpImage: ImageView
    private lateinit var levelTextView: TextView
    private lateinit var xpProgressBar: ProgressBar
    private lateinit var nextUnlockTextView: TextView
    private var soundPool: SoundPool? = null
    private var popSoundId: Int = 0
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "game_prefs"
    private val KEY_HIGH_SCORE = "high_score"
    private val KEY_XP = "xp"
    private val KEY_LEVEL = "level"
    private var currentXP: Int = 0 //backing property for display
        private set(value) {
            field = value
        }
    private var totalXP: Int = 0 // Separate variable to track total XP
    private var currentLevel: Int = 1
        private set(value) { //use set() to update
            field = value
            previousLevel = value //update previous level
        }
    private val xpPerScore = 5f
    private val levelUpThresholds = mapOf(
        2 to "Split",
        5 to "Chaos",
        7 to "PowerUp"
    )
    private val handler = Handler(Looper.getMainLooper())
    private var isLevelUpInProgress = false //track level up
    private var previousLevel = 1 //keep track of the previous level
    private var dataLoaded = false // Track if data is loaded

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        umpImage = findViewById(R.id.umpimage)
        umpConsentButton = findViewById(R.id.umpconsentbutton)// Assuming it's a Button in XML
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        // Initialize UI elements
        levelTextView = findViewById(R.id.levelTextView)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        nextUnlockTextView = findViewById(R.id.nextUnlockTextView)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadGameProgress() // Load game progress  -  Moved to its own call

        // Initialize and set up UI elements
        val scoresButton = findViewById<ImageView>(R.id.scoresbutton)
        val scoreDisplay = findViewById<ImageView>(R.id.scoredisplay)
        val settingsButton = findViewById<ImageView>(R.id.settingsbutton)
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val startButton = findViewById<ImageView>(R.id.startbutton)
        // For testing purposes, you can enable debug settings to force a consent dialog
        val debugSettings = ConsentDebugSettings.Builder(this)
        // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE) // Ensure this is correct
        // .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID") // Uncomment and replace with your test device ID if needed
        // .build()

        val params = ConsentRequestParameters.Builder()
            // .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            { // Consent Info Update Succeeded
                Log.d("UMP", "Consent info update successful. Consent Status: ${consentInformation.consentStatus}")
                Log.d("UMP", "Before loadAndShowConsentFormIfRequired()")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w("UMP", "Consent form load/show failed: ${loadAndShowError.message}, code: ${loadAndShowError.errorCode}")
                    } else {
                        Log.d("UMP", "Consent form loaded and shown (if required).")
                    }
                    updateFirebaseAnalyticsConsent()
                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}
                        FirebaseApp.initializeApp(this)
                        umpConsentButton.visibility = View.GONE
                        umpImage.visibility = View.GONE
                    } else {
                        umpConsentButton.visibility = View.VISIBLE
                        umpImage.visibility = View.VISIBLE
                    }
                }
                Log.d("UMP", "After loadAndShowConsentFormIfRequired()")
            },
            { requestConsentError -> // Consent Info Update Failed
                Log.w("UMP", "Consent info update failed: ${requestConsentError.message}, code: ${requestConsentError.errorCode}")
                umpConsentButton.visibility = View.VISIBLE // Show button even if update fails for manual retry
                umpImage.visibility = View.VISIBLE
                updateFirebaseAnalyticsConsent(false) // Set denied on failure
            })


        // Intent for starting the GamePlay activity.
        val intent = Intent(this, GamePlay::class.java)
        // Pass the activity as ScoreListener
        // Debug button to reset progress
        val resetProgressButton = findViewById<android.widget.Button>(R.id.resetProgressButton)
        if (GameModeStates.debugMode) {

            resetProgressButton.setOnClickListener {
                resetGameProgress()
            }
        } else resetProgressButton.visibility = View.GONE

        // SoundPool for playing sound effects
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        popSoundId = soundPool?.load(this, R.raw.pop, 1) ?: 0 // Load the pop sound

        // Set click listener for the start button to begin the game.
        startButton.setOnClickListener {
            if (dataLoaded) { // Only start if data is loaded
                soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f) // Play sound effect
                startActivityForResult(intent, 123) // Start GamePlay and expect a result
            } else {
                Log.w("MainActivity", "Start button clicked before data loaded")
                // Optionally, show a message to the user that the game is loading.
            }
        }
        umpConsentButton.setOnClickListener {
            UserMessagingPlatform.showPrivacyOptionsForm(this) { formError ->
                if (formError != null) {
                    println("UMP form available but form error")
                    Log.w(
                        "UMP",
                        String.format("%s: %s", formError.message, formError.errorCode)
                    )
                }
            }
        }
        //show high score
        scoresButton.setOnClickListener {
            if (dataLoaded) {
                soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
                displayHighScore()
                scoreDisplay.visibility = View.VISIBLE
                scoreDisplayText.visibility = View.VISIBLE
                scoreDisplay.setOnClickListener {
                    soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
                    scoreDisplay.visibility = View.GONE
                    scoreDisplayText.visibility = View.GONE
                }
            }
        }

        // Start the settings activity when the settings button is clicked.
        settingsButton.setOnClickListener {
            if (dataLoaded) {
                soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
                val intent = Intent(this, Settings::class.java)
                startActivity(intent)
            }
        }

        // Initial visibility of the UMP elements
        umpConsentButton.visibility = View.GONE
        umpImage.visibility = View.GONE

        // Set up window insets to handle screen edges.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun updateFirebaseAnalyticsConsent(granted: Boolean = consentInformation.canRequestAds()) {
        val consentMap = mutableMapOf<ConsentType, ConsentStatus>().apply {
            val consentStatus = if (granted) ConsentStatus.GRANTED else ConsentStatus.DENIED
            this[ConsentType.ANALYTICS_STORAGE] = consentStatus
            this[ConsentType.AD_STORAGE] = consentStatus
            this[ConsentType.AD_USER_DATA] = consentStatus
            this[ConsentType.AD_PERSONALIZATION] = consentStatus
        }
        Firebase.analytics.setConsent(consentMap)
        Log.d("UMP", "Firebase Analytics consent updated. Granted: $granted")
    }
    // Handle the result from GamePlay
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) { // Make sure the request code matches
            if (resultCode == RESULT_OK) {
                val finalScore = data?.getIntExtra("finalScore", 0) ?: 0
                Log.d("MainActivity", "Received final score: $finalScore")
                onScoreEarned(finalScore) // Award XP based on the received score
            } else {
                Log.d("MainActivity", "GamePlay finished without a result or with an error.")
            }
        }
    }

    // Implementation of the onScoreEarned method from the ScoreListener interface.
    override fun onScoreEarned(score: Int) {
        Log.d("MainActivity", "onScoreEarned called with score: $score")
        awardXP(score) // Award XP based on the score earned.
        updateHighScore(score) // Moved highScore update here
    }

    // Method to award XP to the player.
    private fun awardXP(score: Int) {
        Log.d("MainActivity", "awardXP called with score: $score")
        val earnedXP = (score * xpPerScore).toInt()
        Log.d("MainActivity", "Earned XP: $earnedXP")
        totalXP += earnedXP // Update the total XP
        animateLevelProgress(earnedXP)
    }

    private fun animateLevelProgress(earnedXP: Int) {
        if (isLevelUpInProgress) {
            Log.w("MainActivity", "Level up animation in progress, delaying XP animation.")
            handler.postDelayed({ animateLevelProgress(earnedXP) }, 500) //try again after delay
            return
        }

        var startXP = currentXP
        var endXP = startXP + earnedXP
        var startLevel = currentLevel
        var endLevel = calculateLevel(totalXP) //calculate target level using totalXP

        val levelDuration = 1000L  // 1 second per level
        val xpDuration = 1500L // Increased duration for smoother animation
        val totalDuration = if (startLevel != endLevel) levelDuration + xpDuration else xpDuration

        if (startLevel != endLevel) {
            isLevelUpInProgress = true
            val levelIncrement = endLevel - startLevel
            val levelAnimationDuration = levelIncrement * levelDuration
            ObjectAnimator.ofInt(this, "currentLevel", startLevel, endLevel).apply {
                duration = levelAnimationDuration
                addUpdateListener {
                    val newLevel = it.animatedValue as Int
                    handler.post {
                        setLevelTextView(newLevel)
                    }
                }
                addListener(object: Animator.AnimatorListener{
                    override fun onAnimationEnd(animation: Animator) {
                        currentLevel = endLevel
                        saveGameProgress()
                    }

                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }

            // Animate xp progress to 100
            val startProgress = calculateProgress(startXP, startLevel)
            val endProgress = 100
            ValueAnimator.ofInt(startProgress, endProgress).apply {
                duration = levelDuration
                addUpdateListener {
                    val animatedValue = it.animatedValue as Int
                    handler.post { setXpProgress(animatedValue) }
                }
                start()
            }

            startXP = getXpThresholdForLevel(startLevel + 1)
            val remainingXP = earnedXP - (getXpThresholdForLevel(startLevel + 1) - startXP)

            ValueAnimator.ofInt(0, remainingXP).apply {
                duration = xpDuration
                startDelay = levelAnimationDuration
                addUpdateListener {
                    val animatedValue = it.animatedValue as Int
                    val newProgress = calculateProgress(animatedValue, endLevel)
                    handler.post {
                        setXpProgress(newProgress)
                    }
                    this@MainActivity.currentXP = endXP
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) {
                        handler.post {
                            updateLevelUI()
                            // unlockNewMutator(endLevel) <- REMOVED
                            isLevelUpInProgress = false;
                        }
                    }

                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }



        } else {
            // Animate XP progress within the same level
            val startProgress = calculateProgress(startXP, startLevel)
            val endProgress = calculateProgress(endXP, endLevel)
            ValueAnimator.ofInt(startProgress, endProgress).apply {
                duration = xpDuration
                addUpdateListener {
                    val animatedValue = it.animatedValue as Int
                    handler.post { setXpProgress(animatedValue) }
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) {
                        handler.post {
                            this@MainActivity.currentXP = endXP
                            updateLevelUI()
                            checkLevelUp()
                            saveGameProgress() //moved here
                        }
                    }

                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        }
    }

    private fun calculateProgress(xp: Int, level: Int): Int {
        val levelStartXP = getXpThresholdForLevel(level)
        val levelEndXP = getXpThresholdForLevel(level + 1)
        return if (levelEndXP > levelStartXP) {
            ((xp - levelStartXP).toFloat() / (levelEndXP - levelStartXP).toFloat() * 100).toInt()
        } else {
            0
        }
    }

    // Method to check if the player has leveled up.  RETAINED FOR CHECKING ONLY
    private fun checkLevelUp() {
        Log.d("MainActivity", "checkLevelUp - Current XP: $currentXP, Current Level: $currentLevel")
        var playerLeveledUp = false

        if (totalXP >= getXpThresholdForLevel(currentLevel + 1)) {
            playerLeveledUp = true
            val nextLevel = currentLevel + 1
            Log.d("MainActivity", "Level UP! From level $currentLevel to $nextLevel")
        }

        if (playerLeveledUp) {
            Log.d("MainActivity", "Player Leveled Up!  updateLevelUI")
        } else {
            Log.d("MainActivity", "Player did NOT level up.")
        }
    }

    //calculate level based on xp
    private fun calculateLevel(xp: Int): Int {
        var level = 1
        while (xp >= getXpThresholdForLevel(level + 1)) {
            level++
        }
        return level
    }

    // Method to get the XP threshold for a given level.
    private fun getXpThresholdForLevel(level: Int): Int {
        val threshold = level * 900000
        Log.d("MainActivity", "getXpThresholdForLevel - Level: $level, Threshold: $threshold")
        return threshold
    }

    // Method to unlock a new mutator based on the player's level.  REMOVED
    /*
    private fun unlockNewMutator(level: Int) {
        Log.d("MainActivity", "unlockNewMutator called for level: $level")
        // Iterate through the levelUpThresholds map to find mutators that should be unlocked
        for ((unlockLevel, mutatorName) in levelUpThresholds) {
            Log.d(
                "MainActivity",
                "unlockNewMutator: Checking level $level against unlockLevel $unlockLevel for mutator $mutatorName"
            )
            if (level >= unlockLevel) { //changed from == to >=
                if (!GameModeStates.unlockedMutators.contains(mutatorName)) {
                    Log.d("MainActivity", "Mutator '$mutatorName' unlocked!")
                    GameModeStates.unlockedMutators.add(mutatorName)
                    Log.d("MainActivity", "Unlocked Mutators: ${GameModeStates.unlockedMutators}")
                } else {
                    Log.d("MainActivity", "Mutator '$mutatorName' already unlocked!")
                }
            } else {
                Log.d(
                    "MainActivity",
                    "Level $level is not high enough to unlock  Mutator '$mutatorName'. unlockLevel: $unlockLevel"
                )
            }
        }
    }
    */

    // Method to load game progress from shared preferences.
    private fun loadGameProgress() {
        Thread {
            totalXP = sharedPreferences.getInt(KEY_XP, 0) // Load total XP
            currentLevel = sharedPreferences.getInt(KEY_LEVEL, 1) // Load level, and set initial
            previousLevel = currentLevel

            Log.d(
                "MainActivity",
                "loadGameProgress - XP loaded: $totalXP, Level loaded: $currentLevel"
            )


            handler.post {
                // This code runs on the main thread after the data is loaded
                dataLoaded = true;
                updateLevelUI() //update level UI after data is loaded.
            }
        }.start() // Load data in a background thread
    }

    // Method to save game progress to shared preferences.
    private fun saveGameProgress() {
        Log.d(
            "MainActivity",
            "saveGameProgress - Saving XP: $totalXP, Level: $currentLevel"
        )
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_XP, totalXP) // Save total XP
        editor.putInt(KEY_LEVEL, currentLevel) // Save level
        editor.apply() // Apply the changes.
    }

    // Method to get the player's high score from shared preferences.
    private fun getHighScore(): Int {
        return sharedPreferences.getInt(KEY_HIGH_SCORE, 0)
    }

    // Method to update the high score in shared preferences.
    fun updateHighScore(newScore: Int) {
        val currentHighScore = getHighScore()
        if (newScore > currentHighScore) {
            val editor = sharedPreferences.edit()
            editor.putInt(KEY_HIGH_SCORE, newScore)
            editor.apply()
        }
    }

    //refresh
    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        if (dataLoaded) {
            updateLevelUI() //update level, but only if data is loaded.
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause called")
    }

    // Method to display the high score.
    private fun displayHighScore() {
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val highScore = getHighScore()
        scoreDisplayText.text = "High Score:\n$highScore"
    }

    // Method to update the level UI.  This method should only be called after dataLoaded is true
    private fun updateLevelUI() {
        if (!dataLoaded) return;
        levelTextView.text = "Level: $currentLevel" // Display current level
        val nextLevelThreshold = getXpThresholdForLevel(currentLevel + 1)
        val currentThreshold = if (currentLevel == 1) 0 else getXpThresholdForLevel(currentLevel) // Corrected line
        val progress = if (nextLevelThreshold > currentThreshold) {
            ((totalXP - currentThreshold).toFloat() / (nextLevelThreshold - currentThreshold)
                .toFloat() * 100).toInt()
        } else {
            0 // Ensure progress doesn't go below 0
        }
        xpProgressBar.max = 100
        xpProgressBar.progress = progress

        // Display the next unlock information.
        val nextUnlockLevel = levelUpThresholds.keys.sorted().find { it > currentLevel }
        if (nextUnlockLevel != null) {
            val mutatorName = levelUpThresholds[nextUnlockLevel]
            nextUnlockTextView.text = "Next Unlock: Level $nextUnlockLevel - $mutatorName"
        } else {
            nextUnlockTextView.text = "All mutators unlocked \nUntil Next Update!"
        }
        val availableMutators = GameModeStates.getAvailableMutators(currentLevel, levelUpThresholds)
        Log.d("MainActivity", "updateLevelUI - Current Level: $currentLevel, Available Mutators: $availableMutators")
    }

    private fun resetGameProgress() {
        Log.d("MainActivity", "Resetting game progress")
        totalXP = 0
        currentXP = 0
        currentLevel = 1
        previousLevel = 1
        //GameModeStates.unlockedMutators.clear()  <- REMOVED
        //GameModeStates.unlockedMutators.add("No Mutator") <- REMOVED
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        loadGameProgress() // Reload data
    }

    //Setter for xp progress bar.
    fun setXpProgress(progress: Int) {
        xpProgressBar.progress = progress
    }

    //Setter for current level
    fun setLevelTextView(level: Int) { // Changed the name to setLevelTextView
        levelTextView.text = "Level: $level"
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }
}
