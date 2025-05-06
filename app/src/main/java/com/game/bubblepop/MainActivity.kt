package com.game.bubblepop

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    object GameModeStates {
        var isSplitModeActive = false
        var gameDifficulty = "Normal"
    }

    private var soundPool: SoundPool? = null
    private var popSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val scoresButton = findViewById<ImageView>(R.id.scoresbutton)
        val scoreDisplay = findViewById<ImageView>(R.id.scoredisplay)
        val settingsButton = findViewById<ImageView>(R.id.settingsbutton)
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val startButton = findViewById<ImageView>(R.id.startbutton)
        val intent = Intent(this, GamePlay::class.java)
        // val musicPlayer: LoopingMusicPlayer // Consider initializing and managing lifecycle
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Adjust as needed
            .setAudioAttributes(audioAttributes)
            .build()
        popSoundId = soundPool?.load(this, R.raw.pop, 1) ?: 0
        startButton.setOnClickListener {
            soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
            this.startActivity(intent)
        }

        scoresButton.setOnClickListener {
            soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
            displayHighScore()
            scoreDisplay.visibility = View.VISIBLE
            scoreDisplayText.visibility = View.VISIBLE
            scoreDisplay.setOnClickListener {
                soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
                scoreDisplay.visibility = View.GONE
                scoreDisplayText.visibility = View.GONE
            }
        }
        settingsButton.setOnClickListener {
            soundPool?.play(popSoundId, 1f, 1f, 0, 0, 1f)
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        MobileAds.initialize(this) {}
        FirebaseApp.initializeApp(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getHighScore(): Int {
        print("getting score")
        val sharedPref = getSharedPreferences("game_prefs", Context.MODE_PRIVATE) ?: return 0
        return sharedPref.getInt("high_score", 0)
    }

    override fun onResume() {
        super.onResume()
        // Consider resuming music here if implemented
    }



    private fun displayHighScore() {
        val scoreDisplayText = findViewById<TextView>(R.id.textViewscoredisp)
        val highScore = getHighScore()
        scoreDisplayText.text = "High Score:\n$highScore"
        println("displaying score")
    }
}