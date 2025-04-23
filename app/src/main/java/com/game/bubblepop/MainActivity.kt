package com.game.bubblepop

import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val startButton=findViewById<ImageView>(R.id.startbutton)
        val  intent= Intent(this,GamePlay::class.java)
        val musicPlayer: LoopingMusicPlayer
        val soundPool: SoundPool
        val popSoundId: Int
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2) // Adjust as needed
            .setAudioAttributes(audioAttributes)
            .build()
        popSoundId = soundPool.load(this, R.raw.pop, 1)
        startButton.setOnClickListener {
            soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
            this.startActivity(intent)
        }
        MobileAds.initialize(this) {}
        FirebaseApp.initializeApp(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}