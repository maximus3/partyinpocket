package com.m3games.partyinpocket.domain.model.hat

import com.m3games.partyinpocket.domain.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HatGameStateTest {

    private fun state(
        teams: List<Team> = listOf(
            Team(id = 0, name = "A", colorArgb = 0),
            Team(id = 1, name = "B", colorArgb = 0),
            Team(id = 2, name = "C", colorArgb = 0)
        ),
        currentTeamIndex: Int = 0,
        remainingWords: List<String> = listOf("слово1", "слово2"),
        allWords: List<String> = listOf("слово1", "слово2"),
        teamSkipsLeft: Map<Int, Int> = mapOf(0 to 3, 1 to 3, 2 to 3),
        currentRound: HatRound = HatRound.EXPLAIN
    ) = HatGameState(
        settings = HatSettings(),
        teams = teams,
        allWords = allWords,
        remainingWords = remainingWords,
        currentRound = currentRound,
        currentTeamIndex = currentTeamIndex,
        currentWord = remainingWords.firstOrNull(),
        teamSkipsLeft = teamSkipsLeft,
        phase = HatGamePhase.READY_TO_START,
        remainingTimeSeconds = 60
    )

    @Test
    fun `currentTeam returns team at currentTeamIndex`() {
        val s = state(currentTeamIndex = 1)

        assertEquals("B", s.currentTeam.name)
    }

    @Test
    fun `currentTeamSkipsLeft reads from teamSkipsLeft map`() {
        val s = state(
            currentTeamIndex = 2,
            teamSkipsLeft = mapOf(0 to 3, 1 to 2, 2 to 0)
        )

        assertEquals(0, s.currentTeamSkipsLeft)
    }

    @Test
    fun `currentTeamSkipsLeft returns 0 when team has no entry`() {
        val s = state(
            currentTeamIndex = 0,
            teamSkipsLeft = emptyMap()
        )

        assertEquals(0, s.currentTeamSkipsLeft)
    }

    @Test
    fun `nextTeamIndex increments by one`() {
        val s = state(currentTeamIndex = 0)

        assertEquals(1, s.nextTeamIndex)
    }

    @Test
    fun `nextTeamIndex wraps around last team back to first`() {
        val s = state(currentTeamIndex = 2)

        assertEquals(0, s.nextTeamIndex)
    }

    @Test
    fun `isRoundFinished is true when remainingWords is empty`() {
        val s = state(remainingWords = emptyList())

        assertTrue(s.isRoundFinished)
    }

    @Test
    fun `isRoundFinished is false when remainingWords is not empty`() {
        val s = state(remainingWords = listOf("слово"))

        assertFalse(s.isRoundFinished)
    }

    @Test
    fun `isGameFinished is true only on ASSOCIATION with empty remainingWords`() {
        val s = state(currentRound = HatRound.ASSOCIATION, remainingWords = emptyList())

        assertTrue(s.isGameFinished)
    }

    @Test
    fun `isGameFinished is false on EXPLAIN with empty remainingWords`() {
        val s = state(currentRound = HatRound.EXPLAIN, remainingWords = emptyList())

        assertFalse(s.isGameFinished)
    }

    @Test
    fun `isGameFinished is false on PANTOMIME with empty remainingWords`() {
        val s = state(currentRound = HatRound.PANTOMIME, remainingWords = emptyList())

        assertFalse(s.isGameFinished)
    }

    @Test
    fun `isGameFinished is false on ASSOCIATION with non-empty remainingWords`() {
        val s = state(currentRound = HatRound.ASSOCIATION, remainingWords = listOf("слово"))

        assertFalse(s.isGameFinished)
    }
}
