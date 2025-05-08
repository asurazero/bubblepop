package com.game.bubblepop

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log // Import Log

class Settings : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "BubblePopSettings"
    private val KEY_MUTATOR = "mutator"
    private val KEY_DIFFICULTY = "difficulty"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        //TODO switch this back to the Main. unlockedMutators when done testing
        val availableMutators = MainActivity.GameModeStates.unlockedMutators.toMutableList()
        //val availableMutators = MainActivity.GameModeStates.unlockedForTesting.toMutableList()
        val difficultyList = mutableListOf("Easy", "Normal", "Hard")

        val mutatorRightButton = findViewById<ImageView>(R.id.mutatorbuttonright)
        val mutatorLeftButton = findViewById<ImageView>(R.id.mutatorbuttonleft)
        val difficultyRightButton = findViewById<ImageView>(R.id.difficultyright)
        val difficultyLeftButton = findViewById<ImageView>(R.id.difficultyleft)
        val mutatorText = findViewById<TextView>(R.id.mutatortext)
        val difficultyText = findViewById<TextView>(R.id.difficultytext)

        // Load saved settings or use defaults
        var currentMutatorIndex = availableMutators.indexOf(sharedPreferences.getString(KEY_MUTATOR, "No Mutator"))
        var currentDifficultyIndex = difficultyList.indexOf(sharedPreferences.getString(KEY_DIFFICULTY, "Normal"))

        // Ensure indices are valid
        if (currentMutatorIndex == -1) currentMutatorIndex = 0
        if (currentDifficultyIndex == -1) currentDifficultyIndex = 1

        mutatorText.text = availableMutators[currentMutatorIndex]
        difficultyText.text = difficultyList[currentDifficultyIndex]
        MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
        updateMutatorState(mutatorText.text.toString())

        mutatorRightButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex + 1) % availableMutators.size
            mutatorText.text = availableMutators[currentMutatorIndex]
            updateMutatorState(mutatorText.text.toString())
            saveSettings()
            Log.d("Settings", "Mutator Right Clicked. New Mutator: ${mutatorText.text}") //added logs
        }
        mutatorLeftButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex - 1 + availableMutators.size) % availableMutators.size
            mutatorText.text = availableMutators[currentMutatorIndex]
            updateMutatorState(mutatorText.text.toString())
            saveSettings()
            Log.d("Settings", "Mutator Left Clicked. New Mutator: ${mutatorText.text}")//added logs
        }

        //difficulty buttons logic
        difficultyRightButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex + 1) % difficultyList.size
            difficultyText.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
            saveSettings()
            Log.d("Settings", "Difficulty Right Clicked. New Difficulty: ${difficultyText.text}")//added logs
        }
        difficultyLeftButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex - 1 + difficultyList.size) % difficultyList.size
            difficultyText.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
            saveSettings()
            Log.d("Settings", "Difficulty Left Clicked. New Difficulty: ${difficultyText.text}")//added logs
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateMutatorState(mutator: String) {
        MainActivity.GameModeStates.isSplitModeActive = mutator == "Split"
        MainActivity.GameModeStates.isChaosModeActive = mutator == "Chaos"
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
