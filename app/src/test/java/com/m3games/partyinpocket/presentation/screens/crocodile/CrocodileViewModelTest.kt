package com.m3games.partyinpocket.presentation.screens.crocodile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.Player
import com.m3games.partyinpocket.domain.model.WordPack
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGamePhase
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileSettings
import com.m3games.partyinpocket.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrocodileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CrocodileViewModel

    private val testPackId = "crocodile_test_pack"
    private val testWords = listOf("слово1", "слово2", "слово3", "слово4", "слово5", "слово6", "слово7", "слово8")
    private val testPack = WordPack(
        id = testPackId,
        name = "Test",
        description = "test",
        words = testWords
    )

    @Before
    fun setup() {
        PresetWordPacks.addGeneratedPack(testPack)
        viewModel = CrocodileViewModel()
    }

    @After
    fun cleanup() {
        PresetWordPacks.removeGeneratedPack(testPackId)
    }

    private fun configureSettings(
        playerCount: Int = 3,
        targetWordsPerPlayer: Int = 2,
        durationSeconds: Int = 30,
        useTimer: Boolean = true,
        maxSkips: Int = 3,
        skipPenalty: Int = 0,
        selectedPacks: List<String> = listOf(testPackId)
    ) {
        viewModel.updateSettings(
            CrocodileSettings(
                playerCount = playerCount,
                targetWordsPerPlayer = targetWordsPerPlayer,
                turnDurationSeconds = durationSeconds,
                useTimer = useTimer,
                selectedPacks = selectedPacks,
                skipPenalty = skipPenalty,
                maxSkipsPerTurn = maxSkips
            )
        )
    }

    private fun startGame(
        playerCount: Int = 3,
        targetWordsPerPlayer: Int = 2,
        durationSeconds: Int = 30,
        useTimer: Boolean = true,
        maxSkips: Int = 3,
        skipPenalty: Int = 0
    ) {
        configureSettings(playerCount, targetWordsPerPlayer, durationSeconds, useTimer, maxSkips, skipPenalty)
        viewModel.initializePlayers()
        viewModel.startGame()
    }

    // ───── Settings ─────

    @Test
    fun `updatePlayerCount updates settings`() {
        viewModel.updatePlayerCount(5)

        assertEquals(5, viewModel.settings.value.playerCount)
    }

    @Test
    fun `updateTargetWordsPerPlayer updates settings`() {
        viewModel.updateTargetWordsPerPlayer(7)

        assertEquals(7, viewModel.settings.value.targetWordsPerPlayer)
    }

    @Test
    fun `updateUseTimer updates settings`() {
        viewModel.updateUseTimer(false)

        assertFalse(viewModel.settings.value.useTimer)
    }

    @Test
    fun `toggleWordPack adds pack to selection`() {
        configureSettings(selectedPacks = listOf("default"))

        viewModel.toggleWordPack(testPackId)

        assertEquals(listOf("default", testPackId), viewModel.settings.value.selectedPacks)
    }

    // ───── Players ─────

    @Test
    fun `initializePlayers creates N players with default names`() {
        viewModel.updatePlayerCount(4)

        viewModel.initializePlayers()

        val players = viewModel.players.value
        assertEquals(4, players.size)
        assertEquals("Игрок 1", players[0].name)
        assertEquals("Игрок 4", players[3].name)
    }

    @Test
    fun `updatePlayerName mutates only target player`() {
        viewModel.updatePlayerCount(3)
        viewModel.initializePlayers()

        viewModel.updatePlayerName(playerId = 1, name = "Чемпион")

        val players = viewModel.players.value
        assertEquals("Игрок 1", players[0].name)
        assertEquals("Чемпион", players[1].name)
        assertEquals("Игрок 3", players[2].name)
    }

    @Test
    fun `updatePlayerColor stores ARGB Int`() {
        viewModel.updatePlayerCount(2)
        viewModel.initializePlayers()

        viewModel.updatePlayerColor(playerId = 0, color = Color.Magenta)

        assertEquals(Color.Magenta.toArgb(), viewModel.players.value[0].colorArgb)
    }

    // ───── startGame ─────

    @Test
    fun `startGame does nothing when no words available`() {
        configureSettings(selectedPacks = listOf("unknown"))
        viewModel.initializePlayers()

        viewModel.startGame()

        assertNull(viewModel.gameState.value)
    }

    @Test
    fun `startGame initializes state with READY_TO_START phase`() {
        startGame()

        val state = viewModel.gameState.value!!
        assertEquals(CrocodileGamePhase.READY_TO_START, state.phase)
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun `startGame uses all collected words`() {
        startGame()

        val state = viewModel.gameState.value!!
        assertEquals(testWords.toSet(), state.remainingWords.toSet())
    }

    @Test
    fun `startGame assigns max skips to each player`() {
        startGame(playerCount = 3, maxSkips = 4)

        val state = viewModel.gameState.value!!
        assertEquals(mapOf(0 to 4, 1 to 4, 2 to 4), state.playerSkipsLeft)
    }

    // ───── startTurn ─────

    @Test
    fun `startTurn moves to PLAYING and picks first word`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()

        viewModel.startTurn()
        runCurrent()

        val state = viewModel.gameState.value!!
        assertEquals(CrocodileGamePhase.PLAYING, state.phase)
        assertNotNull(state.currentWord)
        assertTrue(state.currentWord!! in testWords)
    }

    @Test
    fun `startTurn does not start timer when useTimer is false`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(useTimer = false, durationSeconds = 3)
        viewModel.startTurn()
        runCurrent()

        advanceTimeBy(5_000)
        advanceUntilIdle()

        // Phase should still be PLAYING since there's no timer
        assertEquals(CrocodileGamePhase.PLAYING, viewModel.gameState.value!!.phase)
    }

    // ───── guessWord ─────

    @Test
    fun `guessWord awards 1 point to current player`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        viewModel.startTurn()
        runCurrent()

        viewModel.guessWord()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.players[0].score)
        assertEquals(0, state.players[1].score)
    }

    @Test
    fun `guessWord removes word from deck`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        viewModel.startTurn()
        runCurrent()
        val word = viewModel.gameState.value!!.currentWord!!

        viewModel.guessWord()

        assertFalse(word in viewModel.gameState.value!!.remainingWords)
    }

    // ───── skipWord ─────

    @Test
    fun `skipWord decrements player skip counter`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 3)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(2, viewModel.gameState.value!!.playerSkipsLeft[0])
    }

    @Test
    fun `skipWord removes word from deck permanently`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 3)
        viewModel.startTurn()
        runCurrent()
        val skipped = viewModel.gameState.value!!.currentWord!!

        viewModel.skipWord()

        assertFalse(skipped in viewModel.gameState.value!!.remainingWords)
    }

    @Test
    fun `skipWord deducts penalty when skipPenalty greater than zero`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(skipPenalty = 1, maxSkips = 3)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(-1, viewModel.gameState.value!!.players[0].score)
    }

    @Test
    fun `skipWord is no-op when no skips left`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.skipWord()
        val stateAfter = viewModel.gameState.value!!

        viewModel.skipWord()

        assertEquals(stateAfter, viewModel.gameState.value)
    }

    // ───── finishTurn ─────

    @Test
    fun `finishTurn ends turn when phase is PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(useTimer = false)
        viewModel.startTurn()
        runCurrent()

        viewModel.finishTurn()

        assertEquals(CrocodileGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `finishTurn is no-op when phase is not PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        val before = viewModel.gameState.value!!

        viewModel.finishTurn()

        assertEquals(before.phase, viewModel.gameState.value!!.phase)
    }

    // ───── nextPlayer ─────

    @Test
    fun `nextPlayer switches to next player and resets time`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 3, durationSeconds = 30)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.finishTurn()

        viewModel.nextPlayer()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.currentPlayerIndex)
        assertEquals(CrocodileGamePhase.READY_TO_START, state.phase)
        assertEquals(30, state.remainingTimeSeconds)
    }

    @Test
    fun `nextPlayer wraps around back to first`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 2)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.finishTurn()
        viewModel.nextPlayer()
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.finishTurn()

        viewModel.nextPlayer()

        assertEquals(0, viewModel.gameState.value!!.currentPlayerIndex)
    }

    @Test
    fun `nextPlayer finishes game when targetTotalWords reached`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 2, targetWordsPerPlayer = 1)
        // Сначала команда 0 угадывает 1 слово
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.finishTurn()
        viewModel.nextPlayer()
        // Команда 1 угадывает 1 слово → суммарно достигнут target = 2
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.finishTurn()

        viewModel.nextPlayer()

        assertEquals(CrocodileGamePhase.GAME_FINISHED, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `nextPlayer finishes game when remainingWords empty`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(playerCount = 2, targetWordsPerPlayer = 100)
        viewModel.startTurn()
        runCurrent()
        repeat(testWords.size) { viewModel.guessWord() }
        // После последнего guess уже TURN_ENDED, remainingWords пуст

        viewModel.nextPlayer()

        assertEquals(CrocodileGamePhase.GAME_FINISHED, viewModel.gameState.value!!.phase)
    }

    // ───── resetGame ─────

    @Test
    fun `resetGame clears game state players and settings`() {
        startGame(playerCount = 4)
        assertNotNull(viewModel.gameState.value)
        assertEquals(4, viewModel.players.value.size)

        viewModel.resetGame()

        assertNull(viewModel.gameState.value)
        assertEquals(emptyList<Player>(), viewModel.players.value)
        assertEquals(CrocodileSettings(), viewModel.settings.value)
    }

    // ───── toggleLastWordAccepted ─────

    @Test
    fun `toggleLastWordAccepted accepts the remaining word and gives player plus one`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()
        val pending = viewModel.gameState.value!!.currentWord!!
        advanceTimeBy(5_000)
        advanceUntilIdle()
        assertEquals(CrocodileGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)

        viewModel.toggleLastWordAccepted()

        val state = viewModel.gameState.value!!
        assertTrue(pending in state.guessedInTurn)
        assertEquals(1, state.players[0].score)
        assertFalse(pending in state.remainingWords)
    }

    @Test
    fun `toggleLastWordAccepted twice reverts to original state`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()
        val pending = viewModel.gameState.value!!.currentWord!!
        advanceTimeBy(5_000)
        advanceUntilIdle()

        viewModel.toggleLastWordAccepted()
        viewModel.toggleLastWordAccepted()

        val state = viewModel.gameState.value!!
        assertFalse(pending in state.guessedInTurn)
        assertEquals(0, state.players[0].score)
        assertTrue(pending in state.remainingWords)
    }

    @Test
    fun `toggleLastWordAccepted is no-op outside TURN_ENDED phase`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        viewModel.startTurn()
        runCurrent()
        val before = viewModel.gameState.value!!

        viewModel.toggleLastWordAccepted()

        assertEquals(before, viewModel.gameState.value)
    }

    @Test
    fun `updateSkipPenalty updates settings`() {
        viewModel.updateSkipPenalty(2)

        assertEquals(2, viewModel.settings.value.skipPenalty)
    }

    // ───── Timer ─────

    @Test
    fun `timer decrements every second while PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(4, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    @Test
    fun `timer transitions to TURN_ENDED when duration elapses`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 2)
        viewModel.startTurn()
        runCurrent()

        advanceTimeBy(2_000)
        advanceUntilIdle()

        assertEquals(CrocodileGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }
}
