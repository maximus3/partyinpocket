package com.m3games.partyinpocket.domain.model.crocodile

import com.m3games.partyinpocket.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrocodileGameStateTest {

    private fun state(
        players: List<Player> = listOf(
            Player(id = 0, name = "A", colorArgb = 0),
            Player(id = 1, name = "B", colorArgb = 0),
            Player(id = 2, name = "C", colorArgb = 0)
        ),
        currentPlayerIndex: Int = 0,
        remainingWords: List<String> = listOf("слово1", "слово2"),
        playerSkipsLeft: Map<Int, Int> = mapOf(0 to 3, 1 to 3, 2 to 3),
        settings: CrocodileSettings = CrocodileSettings()
    ) = CrocodileGameState(
        settings = settings,
        players = players,
        remainingWords = remainingWords,
        currentWord = remainingWords.firstOrNull(),
        currentPlayerIndex = currentPlayerIndex,
        playerSkipsLeft = playerSkipsLeft,
        phase = CrocodileGamePhase.READY_TO_START,
        remainingTimeSeconds = 60
    )

    @Test
    fun `currentPlayer returns player at currentPlayerIndex`() {
        val s = state(currentPlayerIndex = 1)

        assertEquals("B", s.currentPlayer.name)
    }

    @Test
    fun `currentPlayerSkipsLeft reads from playerSkipsLeft`() {
        val s = state(
            currentPlayerIndex = 2,
            playerSkipsLeft = mapOf(0 to 3, 1 to 2, 2 to 0)
        )

        assertEquals(0, s.currentPlayerSkipsLeft)
    }

    @Test
    fun `currentPlayerSkipsLeft returns 0 when player not in map`() {
        val s = state(
            currentPlayerIndex = 0,
            playerSkipsLeft = emptyMap()
        )

        assertEquals(0, s.currentPlayerSkipsLeft)
    }

    @Test
    fun `nextPlayerIndex wraps around`() {
        val s = state(currentPlayerIndex = 2)

        assertEquals(0, s.nextPlayerIndex)
    }

    @Test
    fun `targetTotalWords equals targetWordsPerPlayer times players count`() {
        val s = state(settings = CrocodileSettings(targetWordsPerPlayer = 4))

        assertEquals(12, s.targetTotalWords)
    }

    @Test
    fun `totalScored sums scores across all players`() {
        val players = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 3),
            Player(id = 1, name = "B", colorArgb = 0, score = 2),
            Player(id = 2, name = "C", colorArgb = 0, score = 5)
        )
        val s = state(players = players)

        assertEquals(10, s.totalScored)
    }

    @Test
    fun `isGameFinished true when totalScored reaches target`() {
        val players = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 4),
            Player(id = 1, name = "B", colorArgb = 0, score = 4),
            Player(id = 2, name = "C", colorArgb = 0, score = 4)
        )
        val s = state(players = players, settings = CrocodileSettings(targetWordsPerPlayer = 4))

        assertTrue(s.isGameFinished)
    }

    @Test
    fun `isGameFinished true when remainingWords empty`() {
        val s = state(remainingWords = emptyList())

        assertTrue(s.isGameFinished)
    }

    @Test
    fun `isGameFinished false when neither condition met`() {
        val players = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 1),
            Player(id = 1, name = "B", colorArgb = 0, score = 1),
            Player(id = 2, name = "C", colorArgb = 0, score = 0)
        )
        val s = state(
            players = players,
            settings = CrocodileSettings(targetWordsPerPlayer = 3),
            remainingWords = listOf("ещё одно слово")
        )

        assertFalse(s.isGameFinished)
    }
}
