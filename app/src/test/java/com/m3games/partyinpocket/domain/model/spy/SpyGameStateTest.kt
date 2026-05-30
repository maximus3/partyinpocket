package com.m3games.partyinpocket.domain.model.spy

import com.m3games.partyinpocket.domain.model.common.HiddenDealState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpyGameStateTest {

    private val location = SpyLocation(name = "Бар", roles = listOf("Бармен", "Гость"))
    private val players = listOf(
        SpyPlayer(index = 0, name = "Alice", role = SpyRole.Civilian(location.name, "Бармен")),
        SpyPlayer(index = 1, name = "Bob", role = SpyRole.Spy),
        SpyPlayer(index = 2, name = "Charlie", role = SpyRole.Civilian(location.name, "Гость"))
    )

    private fun state(
        accusedPlayerIndex: Int? = null,
        phase: SpyGamePhase = SpyGamePhase.DEALING_ROLES
    ) = SpyGameState(
        settings = SpySettings(),
        players = players,
        location = location,
        possibleLocations = listOf("Бар", "Самолёт"),
        dealState = HiddenDealState(playerNames = players.map { it.name }, cards = emptyList()),
        phase = phase,
        remainingTimeSeconds = 60,
        accusedPlayerIndex = accusedPlayerIndex
    )

    @Test
    fun `spyPlayer returns the player with Spy role`() {
        val s = state()

        assertEquals("Bob", s.spyPlayer?.name)
    }

    @Test
    fun `accusedPlayer returns null when accusedPlayerIndex is null`() {
        val s = state(accusedPlayerIndex = null)

        assertNull(s.accusedPlayer)
    }

    @Test
    fun `accusedPlayer returns player at accusedPlayerIndex`() {
        val s = state(accusedPlayerIndex = 2)

        assertNotNull(s.accusedPlayer)
        assertEquals("Charlie", s.accusedPlayer!!.name)
    }

    @Test
    fun `accusedPlayer returns null when accusedPlayerIndex is out of bounds`() {
        val s = state(accusedPlayerIndex = 99)

        assertNull(s.accusedPlayer)
    }
}
