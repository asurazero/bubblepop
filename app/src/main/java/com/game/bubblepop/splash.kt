package com.game.bubblepop

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var dataLoaded = false
    private val handler = Handler(Looper.getMainLooper()) // Use a Handler for main thread operations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPreferences = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

        // Start loading game data
        loadGameData()
    }

    private fun loadGameData() {
        Thread {
            // Simulate loading data (replace with your actual loading logic)
            val totalXP = sharedPreferences.getInt("xp", 0)
            val currentLevel = sharedPreferences.getInt("level", 1)

            Log.d("SplashActivity", "Loaded XP: $totalXP, Level: $currentLevel")

            // Simulate a longer loading process if needed
            Thread.sleep(1500)

            dataLoaded = true;
            // Switch back to the main thread to navigate
            handler.post {
                navigateToMainActivity(totalXP, currentLevel)
            }
        }.start()
    }

    private fun navigateToMainActivity(totalXP: Int, currentLevel: Int) {
        // Use handler to navigate to main activity and pass the loaded data.
        handler.post {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("XP_EXTRA", totalXP)
            intent.putExtra("LEVEL_EXTRA", currentLevel)
            startActivity(intent)
            finish()
        }
    }
}