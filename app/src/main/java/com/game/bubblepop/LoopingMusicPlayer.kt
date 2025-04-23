package com.game.bubblepop

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log

class LoopingMusicPlayer(private val context: Context, private val songResourceId: Int) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPosition = 0
    private val handler = Handler()
    private var playDurationMillis: Long = 2000 // Example: Play for 2 seconds

    init {
        mediaPlayer = MediaPlayer.create(context, songResourceId)
        mediaPlayer?.isLooping = false // We'll handle the progression manually
    }

    fun playShortSegment() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.seekTo(currentPosition)
                player.start()

                // Schedule a pause after the specified duration
                handler.postDelayed({
                    pausePlayback()
                }, playDurationMillis)
            } else {
                // If already playing, we might just extend the pause timer
                handler.removeCallbacksAndMessages(null) // Clear any pending pause
                handler.postDelayed({
                    pausePlayback()
                }, playDurationMillis)
            }
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                currentPosition = player.currentPosition
                Log.d("MusicPlayer", "Paused at: $currentPosition")
            }
        }
    }

    fun setSegmentDuration(durationMillis: Long) {
        playDurationMillis = durationMillis
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentPosition = 0
    }

    fun reset() {
        mediaPlayer?.seekTo(0)
        currentPosition = 0
    }
}