package com.game.bubblepop

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.game.bubblepop.Game

class GamePlay : AppCompatActivity(), Game.AdDismissedListener, Game.MissedBubbleChangeListener, Game.GameOverListener {
    var isSplitModeActive = false
    private lateinit var gameView: GameView
    private lateinit var game: Game
    private lateinit var continueMessageTextView: TextView
    private lateinit var gameOverTextView: TextView // Add TextView for game over message
    private var local_score = 0
    private var isGameOver = false // Add a flag to track game over state

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

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        game = Game(screenWidth, screenHeight, this)
        game.adDismissedListener = this
        game.missedBubbleChangeListener = this
        game.gameOverListener = this // CORRECTED LINE: Assign 'this' to the listener property
        gameView.game = game

        gameOverTextView.visibility = View.GONE // Initially hide the game over text
        continueMessageTextView.visibility = View.GONE

        gameView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!game.isGameActive()) {
                        if (continueMessageTextView.visibility == View.VISIBLE) {
                            continueMessageTextView.visibility = View.GONE
                            game.startMusic()
                            game.setGameActive(true)
                            gameView.invalidate() // Make sure to redraw the view
                            return@setOnTouchListener true
                        } else if (isGameOver) { // Use the isGameOver flag here
                            //  No need to start a new MainActivity here, just let the activity finish
                            val resultIntent = Intent()
                            resultIntent.putExtra("finalScore", local_score) //changed from score to Game.score
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            return@setOnTouchListener true
                        }
                    } else if (game.isGameActive()) {
                        game.processClick(event.x, event.y, isSplitModeActive)
                        gameView.invalidate()
                        return@setOnTouchListener true
                    }
                    return@setOnTouchListener false
                }
                else -> return@setOnTouchListener false
            }
        }
    }

    override fun onAdDismissed() {
        runOnUiThread {
            continueMessageTextView.visibility = View.VISIBLE
            game.setGameActive(false)
            gameView.invalidate() // Redraw
        }
        // Removed game.startMusic() from here
    }
    //TODO fix ads crashing game
    //TODO fix sound breaking bug after ads

    override fun onMissedBubbleCountChanged(newCount: Int) {
        gameView.invalidate()
    }

    override fun onGameOver(isNewHighScore: Boolean, score: Int) {
        runOnUiThread {
            isGameOver = true // Set the flag when game over occurs.
            val gameOverMessage = if (isNewHighScore) {
                "New High Score!\nScore: $score"
            } else {
                "Game Over!\nScore: $score"
            }
            local_score = score
            Game.appWideGameData.playerXP = score
            gameOverTextView.text = gameOverMessage
            gameOverTextView.visibility = View.VISIBLE
            continueMessageTextView.visibility = View.GONE // Hide continue message when game over is shown.
            gameView.invalidate() // Redraw to show the game over text.

            // No need to do anything here regarding intents.  The touch listener will handle it.
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
}
