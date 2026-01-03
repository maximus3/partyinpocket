package com.m3games.partyinpocket.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.m3games.partyinpocket.domain.model.hat.HatRound

data class Team(
    val id: Int,
    val name: String,
    val colorArgb: Int,
    val scores: Map<HatRound, Int> = emptyMap()
) {
    val totalScore: Int get() = scores.values.sum()

    val color: Color get() = Color(colorArgb)

    fun withScore(round: HatRound, points: Int): Team {
        val newScores = scores.toMutableMap()
        newScores[round] = (newScores[round] ?: 0) + points
        return copy(scores = newScores)
    }

    companion object {
        fun create(id: Int, name: String, color: Color): Team {
            return Team(id, name, color.toArgb())
        }
    }
}
