package com.game.bubblepop

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class LoopingMusicPlayer(private val context: Context, private val musicResourceId: Int) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var currentPosition = 0
    private var preparationAttempted = false // Add this flag

    init {
        prepareMediaPlayer()
    }

    private fun prepareMediaPlayer() {
        if (preparationAttempted) return // Prevent multiple attempts
        preparationAttempted = true
        try {
            mediaPlayer = MediaPlayer.create(context, musicResourceId).apply {
                setOnPreparedListener { mp ->
                    isPrepared = true
                    Log.d("LoopingMusicPlayer", "MediaPlayer prepared.")
                    if (isPlaying()) { // Check if should be playing
                        mp.start()  // Start if it should be playing
                    }
                    preparationAttempted = false // Reset
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(
                        "LoopingMusicPlayer",
                        "Error preparing MediaPlayer: what=$what, extra=$extra"
                    )
                    isPrepared = false
                    mediaPlayer = null
                    preparationAttempted = false
                    // Consider retrying preparation here if the error is recoverable
                    false // Return true to prevent error popup
                }
                setOnCompletionListener {
                    // Handle completion if needed
                    Log.d("LoopingMusicPlayer", "MediaPlayer completed playback")
                }
                setLooping(true)
            }
        } catch (e: Exception) {
            Log.e("LoopingMusicPlayer", "Exception preparing MediaPlayer: ${e.message}")
            isPrepared = false
            mediaPlayer = null
            preparationAttempted = false
        }
    }

    fun startLooping() {
        if (isPrepared && mediaPlayer != null) {
            try {
                mediaPlayer?.start()
                Log.d("LoopingMusicPlayer", "Started playing music.")
            } catch (e: Exception) {
                Log.e("LoopingMusicPlayer", "Error starting music: ${e.message}")
                prepareMediaPlayer() //try to prepare again
                if (isPrepared && mediaPlayer != null) {
                    mediaPlayer?.start()
                }
            }
        } else if (mediaPlayer != null){
            Log.w("LoopingMusicPlayer", "MediaPlayer not fully prepared, will start onPrepared.")
        }

        else {
            Log.w("LoopingMusicPlayer", "MediaPlayer is null, cannot start. calling prepare.")
            prepareMediaPlayer() //ensure prepared called.
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            try {
                mediaPlayer?.pause()
                currentPosition = mediaPlayer?.currentPosition ?: 0
                Log.d("LoopingMusicPlayer", "Paused music at position: $currentPosition")
            } catch (e: Exception) {
                Log.e("LoopingMusicPlayer", "Error pausing music: ${e.message}")
            }
        } else {
            Log.w("LoopingMusicPlayer", "MediaPlayer not playing, cannot pause.")
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isPrepared = false
                currentPosition = 0
                preparationAttempted = false
                Log.d("LoopingMusicPlayer", "Stopped and released MediaPlayer.")
            } catch (e: Exception) {
                Log.e("LoopingMusicPlayer", "Error stopping music: ${e.message}")
            }

        } else {
            Log.w("LoopingMusicPlayer", "MediaPlayer is null, cannot stop.")
        }
    }

    fun release() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.release()
                mediaPlayer = null
                isPrepared = false
                currentPosition = 0
                preparationAttempted = false
                Log.d("LoopingMusicPlayer", "Released MediaPlayer.")
            } catch (e: Exception) {
                Log.e("LoopingMusicPlayer", "Error releasing MediaPlayer: ${e.message}")
            }

        } else {
            Log.w("LoopingMusicPlayer", "MediaPlayer is null, cannot release.")
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}

