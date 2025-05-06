package com.game.bubblepop

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //TODO build out code for settings menu
        //TODO dont forget to add terms and conditions and privacy policy to settings menu
        setContentView(R.layout.activity_settings)
        var mutatorList = mutableListOf<String>("No Mutator", "Split")
        val mutatorRightButton = findViewById<ImageView>(R.id.mutatorbuttonright) //corrected ID
        val mutatorLeftButton = findViewById<ImageView>(R.id.mutatorbuttonleft)  //corrected ID
        val difficultyRightButton = findViewById<ImageView>(R.id.difficultyright) //corrected ID
        val difficultyLeftButton = findViewById<ImageView>(R.id.difficultyleft)  //corrected ID
        val mutatorText = findViewById<TextView>(R.id.mutatortext)
        val difficultyText = findViewById<TextView>(R.id.difficultytext)
        var difficultyList = mutableListOf<String>("Easy", "Normal", "Hard") //added difficulty list
        var currentDifficultyIndex = 1; // added difficulty index
        var currentMutatorIndex = 0;
        mutatorText.text = mutatorList[currentMutatorIndex]
        difficultyText.text = difficultyList[currentDifficultyIndex]
        MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
        mutatorRightButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex + 1) % mutatorList.size
            mutatorText.text = mutatorList[currentMutatorIndex]
            if(mutatorText.text == "No Mutator"){
                MainActivity.GameModeStates.isSplitModeActive=false
            }
            if(mutatorText.text == "Split"){
                MainActivity.GameModeStates.isSplitModeActive=true
            }
        }
        mutatorLeftButton.setOnClickListener {
            currentMutatorIndex = (currentMutatorIndex - 1 + mutatorList.size) % mutatorList.size
            mutatorText.text = mutatorList[currentMutatorIndex]
            if(mutatorText.text == "No Mutator"){
                MainActivity.GameModeStates.isSplitModeActive=false
            }
            if(mutatorText.text == "Split"){
                MainActivity.GameModeStates.isSplitModeActive=true
            }
        }

        //difficulty buttons logic
        difficultyRightButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex + 1) % difficultyList.size
            difficultyText.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
        }
        difficultyLeftButton.setOnClickListener {
            currentDifficultyIndex = (currentDifficultyIndex - 1 + difficultyList.size) % difficultyList.size
            difficultyText.text = difficultyList[currentDifficultyIndex]
            MainActivity.GameModeStates.gameDifficulty = difficultyText.text.toString()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}