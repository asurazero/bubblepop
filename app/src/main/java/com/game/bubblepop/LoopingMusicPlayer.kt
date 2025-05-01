package com.game.bubblepop

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.util.Log

class LoopingMusicPlayer(context: Context, resourceId: Int) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    init {
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.isLooping = true // Enable looping
        mediaPlayer?.setOnPreparedListener {
            isPrepared = true;
        }
        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            Log.e("LoopingMusicPlayer", "Error: what=$what, extra=$extra")
            release() // Clean up on error.
            false
        }
    }

    fun startLooping() {
        if (isPrepared && mediaPlayer != null) {
            mediaPlayer?.start()
        } else {
            Log.w("LoopingMusicPlayer", "startLooping called before prepared, or mediaPlayer is null");
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
        }
    }

    fun release() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
            isPrepared = false
        }
    }
}
