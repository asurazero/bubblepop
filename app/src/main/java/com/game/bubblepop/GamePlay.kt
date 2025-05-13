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

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_play)

        gameView = findViewById(R.id.gameView)
        continueMessageTextView = findViewById(R.id.continueMessageTextView)
        gameOverTextView = findViewById(R.id.gameOverTextView) // Initialize the game over TextView
        endGameTextView = findViewById<TextView>(R.id.endGameTextView)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        game = Game(screenWidth, screenHeight, this)
        game.missedBubbleChangeListener = this
        game.gameOverListener = this
        gameView.game = game

        gameOverTextView.visibility = View.GONE // Initially hide the game over text
        continueMessageTextView.visibility = View.GONE

        gameView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!game.isGameActive()) {
                        if (continueMessageTextView.visibility == View.VISIBLE) {
                            continueMessageTextView.visibility = View.GONE
                            endGameTextView.visibility = View.GONE
                            game.startMusic()
                            game.setGameActive(true)
                            gameView.invalidate()
                            return@setOnTouchListener true
                        } else if (isReadyToEnd) { // Check the 'isReadyToEnd' flag
                            val resultIntent = Intent()
                            resultIntent.putExtra("finalScore", localScore)
                            setResult(RESULT_OK, resultIntent)
                            Log.d("GameOverFlow", "Finishing GamePlay activity with score: $localScore")
                            finish()
                            return@setOnTouchListener true
                        } else if (!adShown) { //show ad only once
                            endGameOnAdDismiss = true
                            return@setOnTouchListener true
                        }
                    } else if (game.isGameActive()) {
                        game.processClick(event.x, event.y, isSplitModeActive)
                        localScore = game.getScore() // Get the score here!
                        gameView.invalidate()
                        return@setOnTouchListener true
                    }
                    return@setOnTouchListener false
                }
                else -> return@setOnTouchListener false
            }
        }

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
            isGameOver = true // Set the flag when game over occurs.
            isReadyToEnd = false // Reset the flag when game over starts
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
            isReadyToEnd = true // Set the flag after UI updates are complete
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