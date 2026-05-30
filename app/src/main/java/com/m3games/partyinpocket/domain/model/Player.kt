package com.m3games.partyinpocket.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class Player(
    val id: Int,
    val name: String,
    val colorArgb: Int,
    val score: Int = 0
) {
    val color: Color get() = Color(colorArgb)

    fun withScore(delta: Int): Player = copy(score = score + delta)

    companion object {
        fun create(id: Int, name: String, color: Color): Player {
            return Player(id, name, color.toArgb())
        }
    }
}
