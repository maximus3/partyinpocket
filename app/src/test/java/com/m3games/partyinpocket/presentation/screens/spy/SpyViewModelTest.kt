package com.m3games.partyinpocket.presentation.screens.spy

import com.m3games.partyinpocket.domain.model.common.HiddenDealPhase
import com.m3games.partyinpocket.domain.model.spy.SpyGamePhase
import com.m3games.partyinpocket.domain.model.spy.SpyGameWinner
import com.m3games.partyinpocket.domain.model.spy.SpyRole
import com.m3games.partyinpocket.domain.model.spy.SpySettings
import com.m3games.partyinpocket.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SpyViewModel

    @Before
    fun setup() {
        viewModel = SpyViewModel()
    }

    private fun configureSettings(
        playerCount: Int = 4,
        discussionSeconds: Int = 120,
        useRoles: Boolean = true,
        locationPacks: List<String> = listOf("default_spy")
    ) {
        viewModel.updateSettings(
            SpySettings(
                playerCount = playerCount,
                discussionDurationSeconds = discussionSeconds,
                useRoles = useRoles,
                selectedLocationPacks = locationPacks
            )
        )
    }

    private fun startGame(
        playerCount: Int = 4,
        discussionSeconds: Int = 120,
        useRoles: Boolean = true
    ) {
        configureSettings(playerCount, discussionSeconds, useRoles)
        viewModel.initializePlayers()
        viewModel.startGame()
    }

    // ───── Settings ─────

    @Test
    fun `updatePlayerCount updates settings`() {
        viewModel.updatePlayerCount(6)

        assertEquals(6, viewModel.settings.value.playerCount)
    }

    @Test
    fun `updateDiscussionDuration updates settings`() {
        viewModel.updateDiscussionDuration(360)

        assertEquals(360, viewModel.settings.value.discussionDurationSeconds)
    }

    @Test
    fun `updateUseRoles updates settings`() {
        viewModel.updateUseRoles(false)

        assertFalse(viewModel.settings.value.useRoles)
    }

    @Test
    fun `toggleLocationPack adds new pack to selection`() {
        configureSettings(locationPacks = listOf("default_spy"))

        viewModel.toggleLocationPack("fantasy_spy")

        assertEquals(listOf("default_spy", "fantasy_spy"), viewModel.settings.value.selectedLocationPacks)
    }

    @Test
    fun `toggleLocationPack keeps last pack when removing single selection`() {
        configureSettings(locationPacks = listOf("default_spy"))

        viewModel.toggleLocationPack("default_spy")

        assertEquals(listOf("default_spy"), viewModel.settings.value.selectedLocationPacks)
    }

    // ───── Players ─────

    @Test
    fun `initializePlayers creates N default player names`() {
        viewModel.updatePlayerCount(5)

        viewModel.initializePlayers()

        val names = viewModel.playerNames.value
        assertEquals(5, names.size)
        assertEquals("Игрок 1", names[0])
        assertEquals("Игрок 5", names[4])
    }

    @Test
    fun `updatePlayerName mutates only the target slot`() {
        viewModel.updatePlayerCount(3)
        viewModel.initializePlayers()

        viewModel.updatePlayerName(index = 1, name = "Алиса")

        assertEquals(listOf("Игрок 1", "Алиса", "Игрок 3"), viewModel.playerNames.value)
    }

    // ───── startGame ─────

    @Test
    fun `startGame does nothing when fewer than 3 players`() {
        configureSettings(playerCount = 2)
        viewModel.initializePlayers()

        viewModel.startGame()

        assertNull(viewModel.gameState.value)
    }

    @Test
    fun `startGame does nothing when no location packs selected`() {
        configureSettings(locationPacks = listOf("unknown_pack"))
        viewModel.initializePlayers()

        viewModel.startGame()

        assertNull(viewModel.gameState.value)
    }

    @Test
    fun `startGame creates exactly one spy among players`() {
        startGame(playerCount = 5)

        val state = viewModel.gameState.value!!
        val spyCount = state.players.count { it.role is SpyRole.Spy }
        assertEquals(1, spyCount)
    }

    @Test
    fun `startGame assigns the same location to every civilian`() {
        startGame(playerCount = 5)

        val state = viewModel.gameState.value!!
        val locations = state.players
            .mapNotNull { (it.role as? SpyRole.Civilian)?.location }
            .distinct()
        assertEquals(1, locations.size)
        assertEquals(state.location.name, locations.first())
    }

    @Test
    fun `startGame assigns unique roles to civilians when useRoles is true`() {
        startGame(playerCount = 4, useRoles = true)

        val state = viewModel.gameState.value!!
        val civilianRoles = state.players
            .mapNotNull { (it.role as? SpyRole.Civilian)?.role }
        assertEquals("Все роли должны быть уникальны", civilianRoles.size, civilianRoles.distinct().size)
    }

    @Test
    fun `startGame leaves role null for civilians when useRoles is false`() {
        startGame(playerCount = 4, useRoles = false)

        val state = viewModel.gameState.value!!
        val rolesNull = state.players
            .filter { it.role is SpyRole.Civilian }
            .all { (it.role as SpyRole.Civilian).role == null }
        assertTrue(rolesNull)
    }

    @Test
    fun `startGame transitions to DEALING_ROLES phase`() {
        startGame()

        assertEquals(SpyGamePhase.DEALING_ROLES, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `startGame initialises HiddenDealState with one card per player`() {
        startGame(playerCount = 5)

        val deal = viewModel.gameState.value!!.dealState!!
        assertEquals(5, deal.cards.size)
        assertEquals(5, deal.playerNames.size)
        assertEquals(0, deal.currentIndex)
        assertEquals(HiddenDealPhase.WAITING_FOR_PLAYER, deal.phase)
    }

    @Test
    fun `startGame includes the actual location in possibleLocations`() {
        startGame()

        val state = viewModel.gameState.value!!
        assertTrue(state.location.name in state.possibleLocations)
    }

    // ───── Hidden Deal ─────

    @Test
    fun `dealReady transitions WAITING to REVEALING`() {
        startGame()

        viewModel.dealReady()

        assertEquals(HiddenDealPhase.REVEALING_CARD, viewModel.gameState.value!!.dealState!!.phase)
    }

    @Test
    fun `dealHide transitions REVEALING to HIDING`() {
        startGame()
        viewModel.dealReady()

        viewModel.dealHide()

        assertEquals(HiddenDealPhase.HIDING_CARD, viewModel.gameState.value!!.dealState!!.phase)
    }

    @Test
    fun `dealNext advances index and returns to WAITING`() {
        startGame()
        viewModel.dealReady()
        viewModel.dealHide()

        viewModel.dealNext()

        val deal = viewModel.gameState.value!!.dealState!!
        assertEquals(1, deal.currentIndex)
        assertEquals(HiddenDealPhase.WAITING_FOR_PLAYER, deal.phase)
    }

    @Test
    fun `completing all deals transitions phase to DISCUSSION`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 60)

        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }

        assertEquals(SpyGamePhase.DISCUSSION, viewModel.gameState.value!!.phase)
    }

    // ───── Discussion timer ─────

    @Test
    fun `discussion timer decrements remaining time`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 10)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        runCurrent()

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(9, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    @Test
    fun `discussion timer transitions to VOTING when time elapses`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 2)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        runCurrent()

        advanceTimeBy(2_000)
        advanceUntilIdle()

        assertEquals(SpyGamePhase.VOTING, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `finishDiscussionEarly transitions to VOTING immediately`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 600)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        runCurrent()

        viewModel.finishDiscussionEarly()

        assertEquals(SpyGamePhase.VOTING, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `finishDiscussionEarly is a no-op outside of DISCUSSION phase`() {
        startGame()
        val before = viewModel.gameState.value!!

        viewModel.finishDiscussionEarly()

        assertEquals(before.phase, viewModel.gameState.value!!.phase)
    }

    // ───── Accusation ─────

    private fun fastForwardToVoting() = runTest(mainDispatcherRule.testDispatcher) {
        repeat(viewModel.playerNames.value.size) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        viewModel.finishDiscussionEarly()
    }

    @Test
    fun `accusing the spy moves to SPY_GUESSING phase`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 60)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        viewModel.finishDiscussionEarly()

        val spyIndex = viewModel.gameState.value!!.spyPlayer!!.index
        viewModel.accusePlayer(spyIndex)

        val state = viewModel.gameState.value!!
        assertEquals(SpyGamePhase.SPY_GUESSING, state.phase)
        assertEquals(spyIndex, state.accusedPlayerIndex)
    }

    @Test
    fun `accusing a civilian finishes game with SPY winner`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 60)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        viewModel.finishDiscussionEarly()

        val civilianIndex = viewModel.gameState.value!!.players
            .first { it.role is SpyRole.Civilian }
            .index
        viewModel.accusePlayer(civilianIndex)

        val state = viewModel.gameState.value!!
        assertEquals(SpyGamePhase.GAME_FINISHED, state.phase)
        assertEquals(SpyGameWinner.SPY, state.winner)
    }

    @Test
    fun `accusePlayer is a no-op outside of VOTING phase`() {
        startGame()
        val before = viewModel.gameState.value!!

        viewModel.accusePlayer(0)

        assertEquals(before, viewModel.gameState.value)
    }

    // ───── Spy guessing ─────

    @Test
    fun `spy correct guess wins the game for SPY`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 60)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        viewModel.finishDiscussionEarly()
        val spyIndex = viewModel.gameState.value!!.spyPlayer!!.index
        viewModel.accusePlayer(spyIndex)
        val actualLocation = viewModel.gameState.value!!.location.name

        viewModel.spyGuessLocation(actualLocation)

        val state = viewModel.gameState.value!!
        assertEquals(SpyGamePhase.GAME_FINISHED, state.phase)
        assertEquals(SpyGameWinner.SPY, state.winner)
        assertEquals(actualLocation, state.spyGuessedLocation)
    }

    @Test
    fun `spy wrong guess wins the game for CIVILIANS`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, discussionSeconds = 60)
        repeat(3) {
            viewModel.dealReady()
            viewModel.dealHide()
            viewModel.dealNext()
        }
        viewModel.finishDiscussionEarly()
        val spyIndex = viewModel.gameState.value!!.spyPlayer!!.index
        viewModel.accusePlayer(spyIndex)
        val actualLocation = viewModel.gameState.value!!.location.name
        val wrongLocation = viewModel.gameState.value!!.possibleLocations.first { it != actualLocation }

        viewModel.spyGuessLocation(wrongLocation)

        val state = viewModel.gameState.value!!
        assertEquals(SpyGamePhase.GAME_FINISHED, state.phase)
        assertEquals(SpyGameWinner.CIVILIANS, state.winner)
    }

    @Test
    fun `spyGuessLocation is a no-op outside of SPY_GUESSING phase`() {
        startGame()
        val before = viewModel.gameState.value!!

        viewModel.spyGuessLocation("Бар")

        assertEquals(before, viewModel.gameState.value)
    }

    // ───── Reset ─────

    @Test
    fun `resetGame clears state and settings`() {
        startGame()
        assertNotNull(viewModel.gameState.value)

        viewModel.resetGame()

        assertNull(viewModel.gameState.value)
        assertEquals(emptyList<String>(), viewModel.playerNames.value)
        assertEquals(SpySettings(), viewModel.settings.value)
    }
}
