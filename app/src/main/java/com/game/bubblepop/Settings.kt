package com.game.bubblepop

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log // Import Log
import android.view.View
import java.io.IOException
import java.io.InputStream

class Settings : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "BubblePopSettings"
    private val KEY_MUTATOR = "mutator"
    private val KEY_DIFFICULTY = "difficulty"

    private lateinit var legalButton: ImageView
    private lateinit var legalText: TextView
    private lateinit var legalScrollView: ScrollView
    private lateinit var mutatorRightButton: ImageView
    private lateinit var mutatorLeftButton: ImageView
    private lateinit var difficultyRightButton: ImageView
    private lateinit var difficultyLeftButton: ImageView
    private lateinit var mutatorTextView: TextView
    private lateinit var difficultyTextView: TextView
    private lateinit var creditsButton: ImageView
    private lateinit var dismissButton: ImageView
    private lateinit var dismissButtonText: TextView
    private lateinit var image1: ImageView
    private lateinit var image2: ImageView
    private lateinit var image3: ImageView
    private lateinit var image4: ImageView
    private lateinit var legalButtonText: TextView
    private lateinit var creditsButtonText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        legalButton = findViewById(R.id.legalbutton)
        legalText = findViewById(R.id.legalTextView)
        legalScrollView = findViewById(R.id.legalScrollView)
        mutatorRightButton = findViewById(R.id.mutatorbuttonright)
        mutatorLeftButton = findViewById(R.id.mutatorbuttonleft)
        difficultyRightButton = findViewById(R.id.difficultyright)
        difficultyLeftButton = findViewById(R.id.difficultyleft)
        mutatorTextView = findViewById(R.id.mutatortext)
        difficultyTextView = findViewById(R.id.difficultytext)
        creditsButton = findViewById(R.id.creditsbutton)
        dismissButton = findViewById(R.id.dismissbutton)
        dismissButtonText = findViewById(R.id.textViewDismiss)
        image1 = findViewById(R.id.imageView1)
        image2 = findViewById(R.id.imageViewL)
        image3 = findViewById(R.id.imageView)
        image4 = findViewById(R.id.imageView3)
        legalButtonText = findViewById(R.id.textView3)
        creditsButtonText = findViewById(R.id.textView)

        //TODO switch this back to the Main. unlockedMutators when done testing
        val availableMutators = MainActivity.GameModeStates.unlockedMutators.toMutableList()
        //val availableMutators = MainActivity.GameModeStates.unlockedForTesting.toMutableList()
        val difficultyList = mutableListOf("Easy", "Normal", "Hard")

        // Load saved settings or use defaults
        var currentMutatorIndex = availableMutators.indexOf(sharedPreferences.getString(KEY_MUTATOR, "No Mutator"))
        var currentDifficultyIndex = difficultyList.indexOf(sharedPreferences.getString(KEY_DIFFICULTY, "Normal"))

        // Ensure indices are valid
        if (currentMutatorIndex == -1) currentMutatorIndex = 0
        if (currentDifficultyIndex == -1) currentDifficultyIndex = 1

        mutatorTextView.text = availableMutators[currentMutatorIndex]
        difficultyTextView.text = difficultyList[currentDifficultyIndex]
        MainActivity.GameModeStates.gameDifficulty = difficultyTextView.text.toString()
        updateMutatorState(mutatorTextView.text.toString())

        mutatorRightButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex + 1) % availableMutators.size
            mutatorTextView.text = availableMutators[currentMutatorIndex]
            updateMutatorState(mutatorTextView.text.toString())
            saveSettings()
            Log.d("Settings", "Mutator Right Clicked. New Mutator: ${mutatorTextView.text}") //added logs
        }
        mutatorLeftButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex - 1 + availableMutators.size) % availableMutators.size
            mutatorTextView.text = availableMutators[currentMutatorIndex]
            updateMutatorState(mutatorTextView.text.toString())
            saveSettings()
            Log.d("Settings", "Mutator Left Clicked. New Mutator: ${mutatorTextView.text}")//added logs
        }

        //difficulty buttons logic
        difficultyRightButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex + 1) % difficultyList.size
            difficultyTextView.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyTextView.text.toString()
            saveSettings()
            Log.d("Settings", "Difficulty Right Clicked. New Difficulty: ${difficultyTextView.text}")//added logs
        }
        difficultyLeftButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex - 1 + difficultyList.size) % difficultyList.size
            difficultyTextView.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyTextView.text.toString()
            saveSettings()
            Log.d("Settings", "Difficulty Left Clicked. New Difficulty: ${difficultyTextView.text}")//added logs
        }
        legalButton.setOnClickListener {
            hideUI()
            legalScrollView.visibility = View.VISIBLE
            legalText.visibility = View.VISIBLE
            dismissButton.visibility = View.VISIBLE
            dismissButtonText.visibility = View.VISIBLE
            loadLegalText()
        }
        creditsButton.setOnClickListener {
            hideUI()
            legalScrollView.visibility = View.VISIBLE
            legalText.visibility = View.VISIBLE
            dismissButton.visibility = View.VISIBLE
            dismissButtonText.visibility = View.VISIBLE
            loadCreditsText()
        }
        dismissButton.setOnClickListener {
            defaultUI()
            dismissButton.visibility = View.GONE
            dismissButtonText.visibility = View.GONE
            legalScrollView.visibility = View.GONE
            legalText.visibility = View.GONE
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadLegalText() {
        legalText.text = loadTextFromAsset("bubblepoplegal.txt")
    }

    private fun loadCreditsText() {
        legalText.text = loadTextFromAsset("credits.txt")
    }

    private fun hideUI() {
        image1.visibility = View.GONE
        image2.visibility = View.GONE
        image3.visibility = View.GONE
        image4.visibility = View.GONE
        legalButtonText.visibility = View.GONE
        creditsButtonText.visibility = View.GONE
        mutatorRightButton.visibility = View.GONE
        mutatorLeftButton.visibility = View.GONE
        difficultyRightButton.visibility = View.GONE
        difficultyLeftButton.visibility = View.GONE
        mutatorTextView.visibility = View.GONE
        difficultyTextView.visibility = View.GONE
        legalButton.visibility = View.GONE
        creditsButton.visibility = View.GONE
    }

    private fun defaultUI() {
        image1.visibility = View.VISIBLE
        image2.visibility = View.VISIBLE
        image3.visibility = View.VISIBLE
        image4.visibility = View.VISIBLE
        legalButtonText.visibility = View.VISIBLE
        creditsButtonText.visibility = View.VISIBLE
        mutatorRightButton.visibility = View.VISIBLE
        mutatorLeftButton.visibility = View.VISIBLE
        difficultyRightButton.visibility = View.VISIBLE
        difficultyLeftButton.visibility = View.VISIBLE
        mutatorTextView.visibility = View.VISIBLE
        difficultyTextView.visibility = View.VISIBLE
        legalButton.visibility = View.VISIBLE
        creditsButton.visibility = View.VISIBLE
    }

    private fun loadTextFromAsset(fileName: String): String {
        return try {
            val inputStream: InputStream = assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            Log.e("Settings", "Error loading text from asset: $fileName", e)
            ""
        }
    }

    private fun updateMutatorState(mutator: String) {
        MainActivity.GameModeStates.isSplitModeActive = mutator == "Split"
        MainActivity.GameModeStates.isChaosModeActive = mutator == "Chaos"
        MainActivity.GameModeStates.isPowerUpModeActive = mutator == "PowerUp"
        MainActivity.GameModeStates.isOrbitalModeActive = mutator == "Orbit"
        MainActivity.GameModeStates.isSpikeTrapModeActive = mutator == "Spikes"
        Log.d("Settings", "updateMutatorState called. isSplitModeActive: ${MainActivity.GameModeStates.isSplitModeActive}, isChaosModeActive: ${MainActivity.GameModeStates.isChaosModeActive}")
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        val currentMutator = findViewById<TextView>(R.id.mutatortext).text.toString()
        val currentDifficulty = findViewById<TextView>(R.id.difficultytext).text.toString()
        editor.putString(KEY_MUTATOR, currentMutator)
        editor.putString(KEY_DIFFICULTY, currentDifficulty)
        editor.apply()
        Log.d("Settings", "saveSettings called. Mutator saved: $currentMutator, Difficulty saved: $currentDifficulty") //added logs
    }
}