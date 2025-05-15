package com.game.bubblepop

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoadingScreen : AppCompatActivity() {

    private lateinit var powerUpInfoImageView: ImageView
    private lateinit var tipsTextView: TextView
    private val tips = listOf(
        "Tip: Pop multiple bubbles at once for a higher score!",
        "Tip: Negative bubbles decrease your missed count!",
        "Tip: Collect power-ups to gain special abilities!",
        "Tip: In Split Mode, normal bubbles break into smaller ones!",
        "Tip: Keep an eye on the rectangle's color for special effects!"
    )
    private val handler = Handler(Looper.getMainLooper())
    private var currentTipIndex = 0
    private val tipRotationInterval = 15000L // 15 seconds
    private val loadingDuration = 20000L // 20 seconds

    private val rotateTipRunnable = object : Runnable {
        override fun run() {
            tipsTextView.text = tips[currentTipIndex]
            currentTipIndex = (currentTipIndex + 1) % tips.size
            handler.postDelayed(this, tipRotationInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_screen) // Ensure this matches your layout file name

        powerUpInfoImageView = findViewById(R.id.imageView2) // Assuming imageView2 is your power-up sheet
        tipsTextView = findViewById(R.id.textViewprotips)

        // Start rotating tips
        handler.postDelayed(rotateTipRunnable, tipRotationInterval)

        // Start GamePlay after the loading duration
        handler.postDelayed({
            val intent = Intent(this, GamePlay::class.java)
            startActivity(intent)
            finish() // Prevent going back to the loading screen
        }, loadingDuration)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(rotateTipRunnable) // Stop the tip rotation
    }
}