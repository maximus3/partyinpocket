package com.m3games.partyinpocket.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTest {

    @Test
    fun `withScore adds delta to score`() {
        val player = Player(id = 0, name = "A", colorArgb = 0)

        val updated = player.withScore(3)

        assertEquals(3, updated.score)
    }

    @Test
    fun `withScore accumulates positive deltas`() {
        val player = Player(id = 0, name = "A", colorArgb = 0)
            .withScore(2)
            .withScore(5)

        assertEquals(7, player.score)
    }

    @Test
    fun `withScore accepts negative deltas for penalties`() {
        val player = Player(id = 0, name = "A", colorArgb = 0)
            .withScore(5)
            .withScore(-3)

        assertEquals(2, player.score)
    }

    @Test
    fun `withScore allows score to go below zero`() {
        val player = Player(id = 0, name = "A", colorArgb = 0)
            .withScore(-2)

        assertEquals(-2, player.score)
    }

    @Test
    fun `withScore returns a new instance and does not mutate original`() {
        val original = Player(id = 0, name = "A", colorArgb = 0)
        val updated = original.withScore(1)

        assertEquals(0, original.score)
        assertEquals(1, updated.score)
    }

    @Test
    fun `Player create converts Color to ARGB Int`() {
        val player = Player.create(id = 1, name = "Red", color = Color.Red)

        assertEquals(Color.Red.toArgb(), player.colorArgb)
    }

    @Test
    fun `color property reconstructs Color from ARGB`() {
        val original = Color(red = 0.5f, green = 0.25f, blue = 0.75f, alpha = 1f)
        val player = Player.create(id = 0, name = "A", color = original)

        assertEquals(original.toArgb(), player.color.toArgb())
    }

    @Test
    fun `default score is zero`() {
        val player = Player(id = 0, name = "A", colorArgb = 0)

        assertEquals(0, player.score)
    }
}
