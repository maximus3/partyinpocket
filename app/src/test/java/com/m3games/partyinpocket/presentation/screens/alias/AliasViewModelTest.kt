package com.m3games.partyinpocket.presentation.screens.alias

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.Player
import com.m3games.partyinpocket.domain.model.WordPack
import com.m3games.partyinpocket.domain.model.alias.AliasGamePhase
import com.m3games.partyinpocket.domain.model.alias.AliasSettings
import com.m3games.partyinpocket.domain.model.alias.AliasWinCondition
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
class AliasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AliasViewModel

    private val testPackId = "alias_test_pack"
    private val testWords = listOf("слово1", "слово2", "слово3", "слово4", "слово5")
    private val testPack = WordPack(
        id = testPackId,
        name = "Test",
        description = "test",
        words = testWords
    )

    @Before
    fun setup() {
        PresetWordPacks.addGeneratedPack(testPack)
        viewModel = AliasViewModel()
    }

    @After
    fun cleanup() {
        PresetWordPacks.removeGeneratedPack(testPackId)
    }

    private fun configureSettings(
        teamCount: Int = 2,
        durationSeconds: Int = 30,
        winCondition: AliasWinCondition = AliasWinCondition.BY_SCORE,
        targetScore: Int = 30,
        maxRounds: Int = 5,
        maxSkips: Int = 5,
        skipPenalty: Int = 1,
        selectedPacks: List<String> = listOf(testPackId)
    ) {
        viewModel.updateSettings(
            AliasSettings(
                teamCount = teamCount,
                turnDurationSeconds = durationSeconds,
                selectedPacks = selectedPacks,
                winCondition = winCondition,
                targetScore = targetScore,
                maxRounds = maxRounds,
                skipPenalty = skipPenalty,
                maxSkipsPerTurn = maxSkips
            )
        )
    }

    private fun startGame(
        teamCount: Int = 2,
        durationSeconds: Int = 30,
        winCondition: AliasWinCondition = AliasWinCondition.BY_SCORE,
        targetScore: Int = 30,
        maxRounds: Int = 5,
        maxSkips: Int = 5,
        skipPenalty: Int = 1
    ) {
        configureSettings(teamCount, durationSeconds, winCondition, targetScore, maxRounds, maxSkips, skipPenalty)
        viewModel.initializeTeams()
        viewModel.startGame()
    }

    // ───── Settings ─────

    @Test
    fun `updateTeamCount updates settings`() {
        viewModel.updateTeamCount(4)

        assertEquals(4, viewModel.settings.value.teamCount)
    }

    @Test
    fun `updateTurnDuration updates settings`() {
        viewModel.updateTurnDuration(75)

        assertEquals(75, viewModel.settings.value.turnDurationSeconds)
    }

    @Test
    fun `updateWinCondition updates settings`() {
        viewModel.updateWinCondition(AliasWinCondition.BY_ROUNDS)

        assertEquals(AliasWinCondition.BY_ROUNDS, viewModel.settings.value.winCondition)
    }

    @Test
    fun `updateTargetScore updates settings`() {
        viewModel.updateTargetScore(50)

        assertEquals(50, viewModel.settings.value.targetScore)
    }

    @Test
    fun `updateMaxRounds updates settings`() {
        viewModel.updateMaxRounds(7)

        assertEquals(7, viewModel.settings.value.maxRounds)
    }

    @Test
    fun `updateSkipPenalty updates settings`() {
        viewModel.updateSkipPenalty(3)

        assertEquals(3, viewModel.settings.value.skipPenalty)
    }

    @Test
    fun `updateMaxSkips updates settings`() {
        viewModel.updateMaxSkips(8)

        assertEquals(8, viewModel.settings.value.maxSkipsPerTurn)
    }

    @Test
    fun `toggleWordPack adds new pack to selection`() {
        configureSettings(selectedPacks = listOf("default"))

        viewModel.toggleWordPack(testPackId)

        assertEquals(listOf("default", testPackId), viewModel.settings.value.selectedPacks)
    }

    @Test
    fun `toggleWordPack keeps last pack when trying to remove single selection`() {
        configureSettings(selectedPacks = listOf(testPackId))

        viewModel.toggleWordPack(testPackId)

        assertEquals(listOf(testPackId), viewModel.settings.value.selectedPacks)
    }

    // ───── Teams ─────

    @Test
    fun `initializeTeams creates N teams with default names`() {
        viewModel.updateTeamCount(3)

        viewModel.initializeTeams()

        val teams = viewModel.teams.value
        assertEquals(3, teams.size)
        assertEquals("Команда 1", teams[0].name)
        assertEquals("Команда 2", teams[1].name)
        assertEquals("Команда 3", teams[2].name)
    }

    @Test
    fun `updateTeamName mutates only target team`() {
        viewModel.updateTeamCount(3)
        viewModel.initializeTeams()

        viewModel.updateTeamName(teamId = 1, name = "Чемпионы")

        val teams = viewModel.teams.value
        assertEquals("Команда 1", teams[0].name)
        assertEquals("Чемпионы", teams[1].name)
        assertEquals("Команда 3", teams[2].name)
    }

    @Test
    fun `updateTeamColor stores ARGB Int`() {
        viewModel.updateTeamCount(2)
        viewModel.initializeTeams()

        viewModel.updateTeamColor(teamId = 0, color = Color.Cyan)

        assertEquals(Color.Cyan.toArgb(), viewModel.teams.value[0].colorArgb)
    }

    // ───── startGame ─────

    @Test
    fun `startGame does nothing when no words available`() {
        configureSettings(selectedPacks = listOf("unknown_pack"))
        viewModel.initializeTeams()

        viewModel.startGame()

        assertNull(viewModel.gameState.value)
    }

    @Test
    fun `startGame initializes state with READY_TO_START phase and round 1`() {
        startGame()

        val state = viewModel.gameState.value
        assertNotNull(state)
        assertEquals(AliasGamePhase.READY_TO_START, state!!.phase)
        assertEquals(0, state.currentTeamIndex)
        assertEquals(1, state.roundNumber)
    }

    @Test
    fun `startGame puts all collected words in deck`() {
        startGame()

        val state = viewModel.gameState.value!!
        assertEquals(testWords.toSet(), state.remainingWords.map { it.word }.toSet())
    }

    @Test
    fun `startGame assigns max skips to each team`() {
        startGame(teamCount = 3, maxSkips = 4)

        val state = viewModel.gameState.value!!
        assertEquals(mapOf(0 to 4, 1 to 4, 2 to 4), state.teamSkipsLeft)
    }

    @Test
    fun `startGame sets remainingTime to full duration`() {
        startGame(durationSeconds = 45)

        assertEquals(45, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    // ───── startTurn ─────

    @Test
    fun `startTurn moves to PLAYING and picks first word`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()

        viewModel.startTurn()
        runCurrent()

        val state = viewModel.gameState.value!!
        assertEquals(AliasGamePhase.PLAYING, state.phase)
        assertNotNull(state.currentWord)
        assertTrue(state.currentWord!!.word in testWords)
    }

    @Test
    fun `startTurn refreshes skips for the current team`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 5)
        viewModel.startTurn()
        runCurrent()
        viewModel.skipWord()
        viewModel.skipWord()
        assertEquals(3, viewModel.gameState.value!!.teamSkipsLeft[0])

        // Завершаем ход и переключаем команды
        viewModel.guessWord()
        // Тип явно завершён, но мы вручную дернём nextTeam если есть слова
        viewModel.nextTeam()
        viewModel.startTurn()
        runCurrent()

        // У второй команды skips должно быть сброшено на 5
        assertEquals(5, viewModel.gameState.value!!.teamSkipsLeft[1])
    }

    // ───── guessWord ─────

    @Test
    fun `guessWord awards 1 point to current team`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        viewModel.startTurn()
        runCurrent()

        viewModel.guessWord()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.teams[0].score)
        assertEquals(0, state.teams[1].score)
    }

    @Test
    fun `guessWord removes word from deck`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame()
        viewModel.startTurn()
        runCurrent()
        val wordBefore = viewModel.gameState.value!!.currentWord!!.word

        viewModel.guessWord()

        val state = viewModel.gameState.value!!
        assertFalse(wordBefore in state.remainingWords.map { it.word })
    }

    @Test
    fun `guessWord ends turn when all words guessed`() = runTest(mainDispatcherRule.testDispatcher) {
        configureSettings(selectedPacks = listOf(testPackId))
        // На pack=5 слов сделаем команд побольше — таким образом одна команда сможет всё угадать
        viewModel.updateTeamCount(2)
        viewModel.initializeTeams()
        viewModel.startGame()

        viewModel.startTurn()
        runCurrent()
        repeat(testWords.size) { viewModel.guessWord() }

        assertEquals(AliasGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }

    // ───── skipWord ─────

    @Test
    fun `skipWord decrements team skip counter`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 3)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(2, viewModel.gameState.value!!.teamSkipsLeft[0])
    }

    @Test
    fun `skipWord puts word at the bottom of the deck`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 5)
        viewModel.startTurn()
        runCurrent()
        val skipped = viewModel.gameState.value!!.currentWord!!.word

        viewModel.skipWord()

        val state = viewModel.gameState.value!!
        assertEquals(skipped, state.remainingWords.last().word)
    }

    @Test
    fun `skipWord deducts penalty from team score when skipPenalty greater than zero`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(skipPenalty = 2, maxSkips = 5)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(-2, viewModel.gameState.value!!.teams[0].score)
    }

    @Test
    fun `skipWord is a no-op when team has no skips left`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(maxSkips = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.skipWord()
        val stateAfter = viewModel.gameState.value!!

        viewModel.skipWord()

        assertEquals(stateAfter, viewModel.gameState.value)
    }

    // ───── nextTeam ─────

    @Test
    fun `nextTeam switches to next team`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(teamCount = 2)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()

        viewModel.nextTeam()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.currentTeamIndex)
        assertEquals(AliasGamePhase.READY_TO_START, state.phase)
    }

    @Test
    fun `nextTeam resets remaining time to full duration`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 45)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()

        viewModel.nextTeam()

        assertEquals(45, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    @Test
    fun `nextTeam wraps from last team back to first and increments roundNumber`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(teamCount = 2)

        // Команда 0 -> 1
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.nextTeam()
        // Команда 1 -> назад в 0, roundNumber++
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.nextTeam()

        val state = viewModel.gameState.value!!
        assertEquals(0, state.currentTeamIndex)
        assertEquals(2, state.roundNumber)
    }

    @Test
    fun `nextTeam finishes game by BY_SCORE when last team in round and target reached`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(teamCount = 2, targetScore = 1, winCondition = AliasWinCondition.BY_SCORE)

        // Команда 0 угадывает → score=1, достигнут target
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.nextTeam()
        // Команда 1 не угадывает (просто пропускает время), завершает ход
        viewModel.startTurn()
        runCurrent()
        // Игра ещё не финиш, но finishпосле круга
        assertEquals(AliasGamePhase.PLAYING, viewModel.gameState.value!!.phase)
        // Завершаем ход вручную через просрочку таймера
        viewModel.nextTeam()  // imitates next-after-turn-end

        // Это не лучший паттерн в тесте — мы вызвали nextTeam без TURN_ENDED фазы.
        // Но nextTeam не проверяет фазу. Проверяем что игра завершилась корректно.
        val state = viewModel.gameState.value!!
        assertEquals(AliasGamePhase.GAME_FINISHED, state.phase)
    }

    @Test
    fun `nextTeam finishes game by BY_ROUNDS when maxRounds reached`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(teamCount = 2, winCondition = AliasWinCondition.BY_ROUNDS, maxRounds = 2)

        // Round 1: team0 -> team1 -> team0 (новый круг)
        repeat(4) {
            viewModel.startTurn()
            runCurrent()
            viewModel.guessWord()
            viewModel.nextTeam()
        }

        val state = viewModel.gameState.value!!
        assertEquals(AliasGamePhase.GAME_FINISHED, state.phase)
    }

    @Test
    fun `nextTeam finishes game when remainingWords empty`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(teamCount = 2)
        viewModel.startTurn()
        runCurrent()
        repeat(testWords.size) { viewModel.guessWord() }

        viewModel.nextTeam()

        assertEquals(AliasGamePhase.GAME_FINISHED, viewModel.gameState.value!!.phase)
    }

    // ───── resetGame ─────

    @Test
    fun `resetGame clears game state teams and settings`() {
        startGame(teamCount = 3)
        assertNotNull(viewModel.gameState.value)
        assertEquals(3, viewModel.teams.value.size)

        viewModel.resetGame()

        assertNull(viewModel.gameState.value)
        assertEquals(emptyList<Player>(), viewModel.teams.value)
        assertEquals(AliasSettings(), viewModel.settings.value)
    }

    // ───── toggleLastWordAccepted ─────

    @Test
    fun `toggleLastWordAccepted accepts the remaining word and gives team plus one`() = runTest(mainDispatcherRule.testDispatcher) {
        startGame(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()
        val pending = viewModel.gameState.value!!.currentWord!!
        advanceTimeBy(5_000)
        advanceUntilIdle()
        assertEquals(AliasGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)

        viewModel.toggleLastWordAccepted()

        val state = viewModel.gameState.value!!
        assertTrue(pending in state.guessedInTurn)
        assertEquals(1, state.teams[0].score)
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
        assertEquals(0, state.teams[0].score)
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

    // ───── Timer ─────

    @Test
    fun `timer decrements remaining time every second while PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
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

        assertEquals(AliasGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }
}
