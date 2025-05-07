package com.game.bubblepop

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import java.io.Serializable

//  ScoreListener is in interfaces.kt  (Correct - defined in a separate file)

class MainActivity : AppCompatActivity(), ScoreListener {
    // GameModeStates object to hold game mode states
   object GameModeStates {
        var isChaosModeActive = false
        var isSplitModeActive = false
        var gameDifficulty = "Normal"
        var unlockedMutators = mutableSetOf<String>("No Mutator")
        var unlockedForTesting=  mutableSetOf<String>("No Mutator","Split","Chaos")
   }

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
    private val KEY_UNLOCKED_MUTATORS = "unlocked_mutators"
    private var currentXP: Int = 0
    private var currentLevel: Int = 1
    private val xpPerScore = 10f // Changed to Float for more reasonable XP gain
    private val levelUpThresholds = mapOf(
        2 to "Split",
        5 to "Chaos"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        levelTextView = findViewById(R.id.levelTextView)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        nextUnlockTextView = findViewById(R.id.nextUnlockTextView)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadGameProgress() // Load game progress

        updateLevelUI() // Initial update of level UI

        // Initialize and set up UI elements
        val scoresButton = findViewById<ImageView>(R.id.scoresbutton)
        val scoreDisplay = findViewById<ImageView>(R.id.scoredisplay)
        val settingsButton = findViewById<ImageView>(R.id.settingsbutton)
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val startButton = findViewById<ImageView>(R.id.startbutton)

        // Intent for starting the GamePlay activity.
        val intent = Intent(this, GamePlay::class.java)
        // Pass the activity as ScoreListener

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
            soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f) // Play sound effect
            startActivity(intent) // Start the game activity
        }

        //show high score
        scoresButton.setOnClickListener {
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

        // Start the settings activity when the settings button is clicked.
        settingsButton.setOnClickListener {
            soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        // Initialize Mobile Ads and Firebase.
        MobileAds.initialize(this) {}
        FirebaseApp.initializeApp(this)

        // Set up window insets to handle screen edges.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Implementation of the onScoreEarned method from the ScoreListener interface.
    override fun onScoreEarned(score: Int) {
        Log.d("MainActivity", "onScoreEarned called with score: $score")
        awardXP(score) // Award XP based on the score earned.
    }

    // Method to award XP to the player.
    private fun awardXP(score: Int) {
        Log.d("MainActivity", "awardXP called with score: $score")
        currentXP += (score * xpPerScore).toInt() // Calculate and add XP.
        Log.d("MainActivity", "Current XP after award: $currentXP")
        checkLevelUp() // Check if the player has leveled up.
    }

    // Method to check if the player has leveled up.
    private fun checkLevelUp() {
        val nextLevel = currentLevel + 1
        val nextLevelThreshold = getXpThresholdForLevel(nextLevel)
        Log.d("MainActivity", "Checking level up - Current XP: $currentXP, Next Level Threshold ($nextLevel): $nextLevelThreshold")
        if (currentXP >= nextLevelThreshold) {
            Log.d("MainActivity", "Level UP! From level $currentLevel to ${currentLevel + 1}")
            currentLevel = nextLevel // Increment the player's level.
            unlockNewMutator(currentLevel) // Unlock mutator
            saveGameProgress() // Save
        }
        updateLevelUI() //update
    }

    // Method to get the XP threshold for a given level.
    private fun getXpThresholdForLevel(level: Int): Int {
        val threshold = level * 100
        Log.d("MainActivity", "getXpThresholdForLevel - Level: $level, Threshold: $threshold")
        return threshold
    }

    // Method to unlock a new mutator based on the player's level.
    private fun unlockNewMutator(level: Int) {
        val mutatorToUnlock = levelUpThresholds[level] // Get the mutator for the level.
        Log.d("MainActivity", "unlockNewMutator called for level: $level, Mutator to unlock: $mutatorToUnlock")
        mutatorToUnlock?.let {
            if (!GameModeStates.unlockedMutators.contains(it)) {
                Log.d("MainActivity", "Mutator '$it' unlocked!")
                GameModeStates.unlockedMutators.add(it) // Add the mutator to the unlocked list.
                saveGameProgress() // Save
            }
        }
    }

    // Method to load game progress from shared preferences.
    private fun loadGameProgress() {
        currentXP = sharedPreferences.getInt(KEY_XP, 0) // Load XP
        currentLevel = sharedPreferences.getInt(KEY_LEVEL, 1) // Load level
        val savedMutators = sharedPreferences.getStringSet(KEY_UNLOCKED_MUTATORS, setOf("No Mutator"))
        GameModeStates.unlockedMutators.clear()
        GameModeStates.unlockedMutators.addAll(savedMutators ?: setOf("No Mutator"))
        Log.d("MainActivity", "loadGameProgress - XP loaded: $currentXP, Level loaded: $currentLevel, Mutators loaded: $savedMutators")
    }

    // Method to save game progress to shared preferences.
    private fun saveGameProgress() {
        Log.d("MainActivity", "saveGameProgress - Saving XP: $currentXP, Level: $currentLevel, Mutators: ${GameModeStates.unlockedMutators}")
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_XP, currentXP) // Save XP
        editor.putInt(KEY_LEVEL, currentLevel) // Save level
        editor.putStringSet(KEY_UNLOCKED_MUTATORS, GameModeStates.unlockedMutators) // Save mutators
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
        updateLevelUI() //update level UI when returning to main activity
    }

    // Method to display the high score.
    private fun displayHighScore() {
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val highScore = getHighScore()
        scoreDisplayText.text = "High Score:\n$highScore"
    }

    // Method to update the level UI.
    private fun updateLevelUI() {
        levelTextView.text = "Level: $currentLevel" // Display current level
        val nextLevelThreshold = getXpThresholdForLevel(currentLevel + 1)
        val currentThreshold = getXpThresholdForLevel(currentLevel)
        val progress = if (nextLevelThreshold > currentThreshold) {
            ((currentXP - currentThreshold).toFloat() / (nextLevelThreshold - currentThreshold).toFloat() * 100).toInt()
        } else {
            100
        }
        xpProgressBar.max = 100
        xpProgressBar.progress = progress

        // Display the next unlock information.
        val nextUnlockLevel = levelUpThresholds.keys.sorted().find { it > currentLevel }
        if (nextUnlockLevel != null) {
            val mutatorName = levelUpThresholds[nextUnlockLevel]
            nextUnlockTextView.text = "Next Unlock: Level $nextUnlockLevel - $mutatorName"
        } else {
            nextUnlockTextView.text = "All mutators unlocked!"
        }
    }
}

