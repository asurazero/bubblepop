package com.game.bubblepop

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity




class GamePlay : AppCompatActivity(), Game.AdDismissedListener, Game.MissedBubbleChangeListener {

    private lateinit var gameView: GameView
    private lateinit var game: Game
    private lateinit var continueMessageTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_play)

        gameView = findViewById(R.id.gameView)
        continueMessageTextView = findViewById(R.id.continueMessageTextView)

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        game = Game(screenWidth, screenHeight, this)
        game.adDismissedListener = this
        game.missedBubbleChangeListener = this // Set the new listener

        gameView.game = game

        gameView.setOnTouchListener { _, event ->
            if (!game.isGameActive() && continueMessageTextView.visibility == View.VISIBLE && event.action == MotionEvent.ACTION_DOWN) {
                continueMessageTextView.visibility = View.GONE
                game.setGameActive(true)
                true
            } else if (event.action == MotionEvent.ACTION_DOWN && game.isGameActive()) {
                game.processClick(event.x, event.y)
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
    }

    override fun onMissedBubbleCountChanged(newCount: Int) {
        // Trigger a redraw of the GameView to update the UI
        gameView.invalidate()
    }





    override fun onResume() {
        super.onResume()
        // No need to call gameView.resume() here unless you have specific logic in GameView
    }

    override fun onPause() {
        super.onPause()
        // No need to call gameView.pause() here unless you have specific logic in GameView
    }

    override fun onDestroy() {
        super.onDestroy()
        game.endGame()
    }
}