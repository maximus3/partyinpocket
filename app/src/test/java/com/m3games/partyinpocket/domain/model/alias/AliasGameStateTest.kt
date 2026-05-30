package com.m3games.partyinpocket.domain.model.alias

import com.m3games.partyinpocket.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AliasGameStateTest {

    private fun state(
        teams: List<Player> = listOf(
            Player(id = 0, name = "A", colorArgb = 0),
            Player(id = 1, name = "B", colorArgb = 0),
            Player(id = 2, name = "C", colorArgb = 0)
        ),
        currentTeamIndex: Int = 0,
        remainingWords: List<AliasWord> = listOf(AliasWord("слово1"), AliasWord("слово2")),
        teamSkipsLeft: Map<Int, Int> = mapOf(0 to 3, 1 to 3, 2 to 3),
        settings: AliasSettings = AliasSettings()
    ) = AliasGameState(
        settings = settings,
        teams = teams,
        remainingWords = remainingWords,
        currentWord = remainingWords.firstOrNull(),
        currentTeamIndex = currentTeamIndex,
        roundNumber = 1,
        teamSkipsLeft = teamSkipsLeft,
        phase = AliasGamePhase.READY_TO_START,
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
    fun `nextTeamIndex wraps last team back to first`() {
        val s = state(currentTeamIndex = 2)

        assertEquals(0, s.nextTeamIndex)
    }

    @Test
    fun `isLastTeamInRound is true when currentTeamIndex equals teams size minus one`() {
        val s = state(currentTeamIndex = 2)

        assertTrue(s.isLastTeamInRound)
    }

    @Test
    fun `isLastTeamInRound is false for non-last team`() {
        val s = state(currentTeamIndex = 0)

        assertFalse(s.isLastTeamInRound)
    }

    @Test
    fun `anyTeamReachedTarget is true when at least one team meets target score`() {
        val teams = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 10),
            Player(id = 1, name = "B", colorArgb = 0, score = 30),
            Player(id = 2, name = "C", colorArgb = 0, score = 5)
        )
        val s = state(teams = teams, settings = AliasSettings(targetScore = 30))

        assertTrue(s.anyTeamReachedTarget)
    }

    @Test
    fun `anyTeamReachedTarget is false when no team reached target`() {
        val teams = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 10),
            Player(id = 1, name = "B", colorArgb = 0, score = 20),
            Player(id = 2, name = "C", colorArgb = 0, score = 5)
        )
        val s = state(teams = teams, settings = AliasSettings(targetScore = 30))

        assertFalse(s.anyTeamReachedTarget)
    }

    @Test
    fun `anyTeamReachedTarget treats exact target as reached`() {
        val teams = listOf(
            Player(id = 0, name = "A", colorArgb = 0, score = 30),
            Player(id = 1, name = "B", colorArgb = 0, score = 20),
            Player(id = 2, name = "C", colorArgb = 0, score = 5)
        )
        val s = state(teams = teams, settings = AliasSettings(targetScore = 30))

        assertTrue(s.anyTeamReachedTarget)
    }
}
