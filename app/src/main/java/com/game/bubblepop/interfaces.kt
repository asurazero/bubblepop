package com.game.bubblepop



import java.io.Serializable

interface ScoreListener : Serializable {
    fun onScoreEarned(score: Int)
}
