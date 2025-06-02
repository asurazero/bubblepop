package com.game.bubblepop

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.game.bubblepop.MainActivity.GameModeStates

class GamePlay : AppCompatActivity(), Game.MissedBubbleChangeListener,
    Game.GameOverListener {
    var isSplitModeActive = false
    // We'll also need a way to check if OrbitMode is active, likely from MainActivity.GameModeStates
    var isOrbitModeActive = false // Add this property to mirror the global state

    private lateinit var gameView: GameView
    private lateinit var game: Game
    private lateinit var continueMessageTextView: TextView
    private lateinit var endGameTextView: TextView
    private lateinit var gameOverTextView: TextView // Add TextView for game over message
    private var localScore = 0
    private var isGameOver = false // Add a flag to track game over state
    private var isReadyToEnd = false // New flag to control when finish() is called
    private var endGameOnAdDismiss = false
    private var adShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MainActivity.GameModeStates.isSplitModeActive) {
            isSplitModeActive = true
        }
        // Get the initial OrbitModeActive state from MainActivity.GameModeStates
        isOrbitModeActive = MainActivity.GameModeStates.isOrbitalModeActive


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_play)

        gameView = findViewById(R.id.gameView)
        continueMessageTextView = findViewById(R.id.continueMessageTextView)
        gameOverTextView = findViewById(R.id.gameOverTextView) // Initialize the game over TextView
        endGameTextView = findViewById<TextView>(R.id.endGameTextView)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()

        // Pass the orbit mode state to the Game constructor
        game = Game(screenWidth, screenHeight, this, isOrbitModeActive)
        game.missedBubbleChangeListener = this
        game.gameOverListener = this
        gameView.game = game

        gameOverTextView.visibility = View.GONE // Initially hide the game over text
        continueMessageTextView.visibility = View.GONE



        endGameTextView.setOnClickListener {
            isGameOver = true // Set the flag when game over occurs.
            continueMessageTextView.visibility = View.GONE // Hide continue message when game over is shown.
            endGameTextView.visibility = View.GONE
            onGameOver(isNewHighScore = false, score = Game.appWideGameData.globalScore) // Redraw to show the game over text.
        }
    }

    override fun onMissedBubbleCountChanged(newCount: Int) {
        gameView.invalidate()
    }

    override fun onGameOver(isNewHighScore: Boolean, score: Int) {
        runOnUiThread {
            isGameOver = true
            isReadyToEnd = false
            GameModeStates.gameover = true
            println(score)
            println(Game.appWideGameData.globalScore)
            val gameOverMessage = if (isNewHighScore) {
                "New High Score!\nScore: $score"
            } else {
                "Game Over!\nScore: $score"
            }
            localScore = score
            Game.appWideGameData.playerXP = score
            gameOverTextView.text = gameOverMessage
            gameOverTextView.visibility = View.VISIBLE
            continueMessageTextView.visibility = View.GONE
            gameView.invalidate() // Redraw to show the game over text.

            // --- ADD THIS CLICK LISTENER ---
            gameOverTextView.setOnClickListener {
                Log.d("GamePlay", "Game Over screen clicked! Finishing activity to return to Main Menu.")
                val resultIntent = Intent()
                resultIntent.putExtra("finalScore", localScore)
                setResult(RESULT_OK, resultIntent)
                finish() // This will return to MainActivity and trigger onActivityResult
            }
            // --- END ADDITION ---

            isReadyToEnd = true
        }
        println("  Game Over in Activity! Score: $score, New High Score: $isNewHighScore  ")
    }

    override fun onResume() {
        super.onResume()
        game.startMusic() // Start music when the activity resumes
    }

    override fun onPause() {
        super.onPause()
        game.stopMusic() // Stop music when the activity is paused
    }

    override fun onDestroy() {
        super.onDestroy()
        // It's good practice to release resources in onDestroy
        game.setGameActive(false) // Ensure game loop stops
        game.releaseSoundPool() // Release sound pool resources
        // Consider releasing other resources held by the Game class if necessary
    }

    override fun onStop() {
        super.onStop()
        // No need to explicitly stop music here as onPause() will be called before onStop()
    }

    override fun onBackPressed() {
        if (game.isGameActive()) {
            Log.d("GamePlay", "Back button disabled while game is active.")
        } else {
            super.onBackPressed()
        }
    }
}