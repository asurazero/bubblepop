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

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MainActivity.GameModeStates.isSplitModeActive == true) {
            isSplitModeActive = true
        } else isSplitModeActive = false

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

        gameView.setOnTouchListener { _, event ->
            if (!game.isGameActive() && continueMessageTextView.visibility == View.VISIBLE && event.action == MotionEvent.ACTION_DOWN) {
                continueMessageTextView.visibility = View.GONE
                game.startMusic()
                game.setGameActive(true)
                true
            } else if (!game.isGameActive() && gameOverTextView.visibility == View.VISIBLE && event.action == MotionEvent.ACTION_DOWN) {
                // Handle restart game logic here if needed
                // For now, let's just go back to MainActivity
                println("Game Over screen touched.")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Finish the GamePlay activity
                true
            } else if (event.action == MotionEvent.ACTION_DOWN && game.isGameActive()) {
                game.processClick(event.x, event.y, isSplitModeActive)
                gameView.invalidate()
                true
            } else {
                false
            }
        }
    }

    override fun onAdDismissed() {
        continueMessageTextView.visibility = View.VISIBLE
        game.setGameActive(false)
        // Removed game.startMusic() from here
    }

    //TODO fix sound breaking bug after ads

    override fun onMissedBubbleCountChanged(newCount: Int) {
        gameView.invalidate()
    }

    override fun onGameOver(isNewHighScore: Boolean, score: Int) {
        runOnUiThread {
            val gameOverMessage = if (isNewHighScore) {
                "New High Score!\nScore: $score"
            } else {
                "Game Over!\nScore: $score"
            }
            gameOverTextView.text = gameOverMessage
            gameOverTextView.visibility = View.VISIBLE
            continueMessageTextView.visibility = View.GONE
        }
        println("  Game Over in Activity! Score: $score, New High Score: $isNewHighScore  ")
        // High score saving is already handled in the Game class
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