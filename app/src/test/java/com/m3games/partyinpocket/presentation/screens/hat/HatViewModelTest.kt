package com.m3games.partyinpocket.presentation.screens.hat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.WordPack
import com.m3games.partyinpocket.domain.model.hat.HatGamePhase
import com.m3games.partyinpocket.domain.model.hat.HatRound
import com.m3games.partyinpocket.domain.model.hat.HatSettings
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
class HatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HatViewModel

    private val testPackId = "test_pack"
    private val testWords = listOf(
        "слово1", "слово2", "слово3", "слово4", "слово5",
        "слово6", "слово7", "слово8", "слово9", "слово10"
    )
    private val testPack = WordPack(
        id = testPackId,
        name = "Test",
        description = "test",
        words = testWords
    )

    @Before
    fun setup() {
        PresetWordPacks.addGeneratedPack(testPack)
        viewModel = HatViewModel()
    }

    @After
    fun cleanup() {
        PresetWordPacks.removeGeneratedPack(testPackId)
    }

    private fun configureSettings(
        teamCount: Int = 2,
        wordCount: Int = 3,
        skipPenalty: Int = 0,
        maxSkips: Int = 5,
        durationSeconds: Int = 30,
        selectedPacks: List<String> = listOf(testPackId)
    ) {
        viewModel.updateSettings(
            HatSettings(
                teamCount = teamCount,
                wordCount = wordCount,
                turnDurationSeconds = durationSeconds,
                selectedPacks = selectedPacks,
                skipPenalty = skipPenalty,
                maxSkipsPerTurn = maxSkips
            )
        )
    }

    private fun startGameWithDefaults(
        teamCount: Int = 2,
        wordCount: Int = 3,
        skipPenalty: Int = 0,
        maxSkips: Int = 5,
        durationSeconds: Int = 30
    ) {
        configureSettings(teamCount, wordCount, skipPenalty, maxSkips, durationSeconds)
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
    fun `updateWordCount updates settings`() {
        viewModel.updateWordCount(50)

        assertEquals(50, viewModel.settings.value.wordCount)
    }

    @Test
    fun `updateTurnDuration updates settings`() {
        viewModel.updateTurnDuration(45)

        assertEquals(45, viewModel.settings.value.turnDurationSeconds)
    }

    @Test
    fun `updateMaxSkips updates settings`() {
        viewModel.updateMaxSkips(7)

        assertEquals(7, viewModel.settings.value.maxSkipsPerTurn)
    }

    @Test
    fun `toggleWordPack adds new pack to selection`() {
        configureSettings(selectedPacks = listOf("default"))

        viewModel.toggleWordPack(testPackId)

        assertEquals(listOf("default", testPackId), viewModel.settings.value.selectedPacks)
    }

    @Test
    fun `toggleWordPack removes pack from selection when multiple are selected`() {
        configureSettings(selectedPacks = listOf("default", testPackId))

        viewModel.toggleWordPack(testPackId)

        assertEquals(listOf("default"), viewModel.settings.value.selectedPacks)
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
    fun `initializeTeams assigns unique ids 0 through N-1`() {
        viewModel.updateTeamCount(4)

        viewModel.initializeTeams()

        assertEquals(listOf(0, 1, 2, 3), viewModel.teams.value.map { it.id })
    }

    @Test
    fun `updateTeamName mutates only target team`() {
        viewModel.updateTeamCount(3)
        viewModel.initializeTeams()

        viewModel.updateTeamName(teamId = 1, name = "Победители")

        val teams = viewModel.teams.value
        assertEquals("Команда 1", teams[0].name)
        assertEquals("Победители", teams[1].name)
        assertEquals("Команда 3", teams[2].name)
    }

    @Test
    fun `updateTeamColor stores ARGB Int`() {
        viewModel.updateTeamCount(2)
        viewModel.initializeTeams()

        viewModel.updateTeamColor(teamId = 0, color = Color.Magenta)

        assertEquals(Color.Magenta.toArgb(), viewModel.teams.value[0].colorArgb)
    }

    // ───── getTotalAvailableWords ─────

    @Test
    fun `getTotalAvailableWords returns count of distinct words across packs`() {
        configureSettings(selectedPacks = listOf(testPackId))

        assertEquals(testWords.size, viewModel.getTotalAvailableWords())
    }

    @Test
    fun `getTotalAvailableWords returns zero for unknown packs`() {
        configureSettings(selectedPacks = listOf("unknown"))

        assertEquals(0, viewModel.getTotalAvailableWords())
    }

    // ───── startGame ─────

    @Test
    fun `startGame does nothing when no words available`() {
        configureSettings(selectedPacks = listOf("unknown"))
        viewModel.initializeTeams()

        viewModel.startGame()

        assertNull(viewModel.gameState.value)
    }

    @Test
    fun `startGame initializes state with EXPLAIN round and READY_TO_START phase`() {
        startGameWithDefaults()

        val state = viewModel.gameState.value
        assertNotNull(state)
        assertEquals(HatRound.EXPLAIN, state!!.currentRound)
        assertEquals(HatGamePhase.READY_TO_START, state.phase)
        assertEquals(0, state.currentTeamIndex)
    }

    @Test
    fun `startGame populates allWords and remainingWords with same elements`() {
        startGameWithDefaults(wordCount = 3)

        val state = viewModel.gameState.value!!
        assertEquals(state.allWords.toSet(), state.remainingWords.toSet())
        assertTrue(state.allWords.all { it in testWords })
    }

    @Test
    fun `startGame caps word count at wordCount setting`() {
        startGameWithDefaults(wordCount = 2)

        val state = viewModel.gameState.value!!
        assertEquals(2, state.allWords.size)
        assertEquals(2, state.remainingWords.size)
    }

    @Test
    fun `startGame assigns maxSkips to each team`() {
        startGameWithDefaults(teamCount = 3, maxSkips = 4)

        val state = viewModel.gameState.value!!
        assertEquals(
            mapOf(0 to 4, 1 to 4, 2 to 4),
            state.teamSkipsLeft
        )
    }

    @Test
    fun `startGame initializes remaining time to full duration`() {
        startGameWithDefaults(durationSeconds = 45)

        assertEquals(45, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    // ───── startTurn ─────

    @Test
    fun `startTurn moves phase to PLAYING and sets current word`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults()

        viewModel.startTurn()
        runCurrent()

        val state = viewModel.gameState.value!!
        assertEquals(HatGamePhase.PLAYING, state.phase)
        assertNotNull(state.currentWord)
        assertTrue(state.currentWord in testWords)
    }

    @Test
    fun `startTurn uses full duration when no previous time saved`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 60)

        viewModel.startTurn()
        runCurrent()

        assertEquals(60, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    // ───── guessWord ─────

    @Test
    fun `guessWord awards 1 point to current team in current round`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults()
        viewModel.startTurn()
        runCurrent()
        val wordBefore = viewModel.gameState.value!!.currentWord!!

        viewModel.guessWord()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.teams[0].scores[HatRound.EXPLAIN])
        assertEquals(0, state.teams[1].totalScore)
        assertTrue(wordBefore in state.guessedInTurn)
        assertFalse(wordBefore in state.remainingWords)
    }

    @Test
    fun `guessWord advances to next word in remainingWords`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults()
        viewModel.startTurn()
        runCurrent()
        val initialRemaining = viewModel.gameState.value!!.remainingWords.size

        viewModel.guessWord()

        assertEquals(initialRemaining - 1, viewModel.gameState.value!!.remainingWords.size)
    }

    @Test
    fun `guessWord ends turn when no words left`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn()
        runCurrent()

        viewModel.guessWord()

        assertEquals(HatGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }

    // ───── skipWord ─────

    @Test
    fun `skipWord decrements team skip counter`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(maxSkips = 3)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(2, viewModel.gameState.value!!.teamSkipsLeft[0])
    }

    @Test
    fun `skipWord removes word from allWords permanently`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults()
        viewModel.startTurn()
        runCurrent()
        val skipped = viewModel.gameState.value!!.currentWord!!

        viewModel.skipWord()

        val state = viewModel.gameState.value!!
        assertFalse("$skipped должно быть удалено из allWords", skipped in state.allWords)
        assertFalse("$skipped должно быть удалено из remainingWords", skipped in state.remainingWords)
        assertTrue(skipped in state.skippedInTurn)
    }

    @Test
    fun `skipWord deducts penalty when skipPenalty greater than zero`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(skipPenalty = 2)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(-2, viewModel.gameState.value!!.teams[0].scores[HatRound.EXPLAIN])
    }

    @Test
    fun `skipWord does not deduct points when skipPenalty is zero`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(skipPenalty = 0)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(emptyMap<HatRound, Int>(), viewModel.gameState.value!!.teams[0].scores)
    }

    @Test
    fun `skipWord is no-op when team has no skips left`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(maxSkips = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.skipWord()
        val stateAfterFirstSkip = viewModel.gameState.value!!

        viewModel.skipWord()

        assertEquals(stateAfterFirstSkip, viewModel.gameState.value)
    }

    @Test
    fun `skipWord ends turn when no words left`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn()
        runCurrent()

        viewModel.skipWord()

        assertEquals(HatGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }

    // ───── nextTeam ─────

    @Test
    fun `nextTeam switches to next team when words remain`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(teamCount = 2, wordCount = 3)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()

        viewModel.nextTeam()

        val state = viewModel.gameState.value!!
        assertEquals(1, state.currentTeamIndex)
        assertEquals(HatGamePhase.READY_TO_START, state.phase)
    }

    @Test
    fun `nextTeam resets time to full duration when switching teams`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 60)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()

        viewModel.nextTeam()

        assertEquals(60, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    @Test
    fun `nextTeam wraps from last team back to first`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(teamCount = 3, wordCount = 5)
        repeat(3) {
            viewModel.startTurn()
            runCurrent()
            viewModel.guessWord()
            viewModel.nextTeam()
        }

        assertEquals(0, viewModel.gameState.value!!.currentTeamIndex)
    }

    @Test
    fun `nextTeam keeps current team and sets ROUND_ENDED when words are exhausted`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(teamCount = 2, wordCount = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        val teamBefore = viewModel.gameState.value!!.currentTeamIndex

        viewModel.nextTeam()

        val state = viewModel.gameState.value!!
        assertEquals(teamBefore, state.currentTeamIndex)
        assertEquals(HatGamePhase.ROUND_ENDED, state.phase)
    }

    // ───── nextRound ─────

    @Test
    fun `nextRound advances EXPLAIN to PANTOMIME`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.nextTeam()

        viewModel.nextRound()

        assertEquals(HatRound.PANTOMIME, viewModel.gameState.value!!.currentRound)
    }

    @Test
    fun `nextRound advances PANTOMIME to ASSOCIATION`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn(); runCurrent(); viewModel.guessWord(); viewModel.nextTeam()
        viewModel.nextRound()
        viewModel.startTurn(); runCurrent(); viewModel.guessWord(); viewModel.nextTeam()

        viewModel.nextRound()

        assertEquals(HatRound.ASSOCIATION, viewModel.gameState.value!!.currentRound)
    }

    @Test
    fun `nextRound finishes game after ASSOCIATION`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn(); runCurrent(); viewModel.guessWord(); viewModel.nextTeam()
        viewModel.nextRound()
        viewModel.startTurn(); runCurrent(); viewModel.guessWord(); viewModel.nextTeam()
        viewModel.nextRound()
        viewModel.startTurn(); runCurrent(); viewModel.guessWord(); viewModel.nextTeam()

        viewModel.nextRound()

        assertEquals(HatGamePhase.GAME_FINISHED, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `nextRound reshuffles words from allWords, excluding skipped`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 3, maxSkips = 1)
        viewModel.startTurn()
        runCurrent()
        val skipped = viewModel.gameState.value!!.currentWord!!
        viewModel.skipWord()
        viewModel.guessWord()
        viewModel.guessWord()
        viewModel.nextTeam()

        viewModel.nextRound()

        val state = viewModel.gameState.value!!
        assertEquals(2, state.remainingWords.size)
        assertFalse(skipped in state.remainingWords)
    }

    @Test
    fun `nextRound preserves current team across round transition`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(teamCount = 2, wordCount = 1, durationSeconds = 60)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        viewModel.nextTeam()
        val teamBefore = viewModel.gameState.value!!.currentTeamIndex

        viewModel.nextRound()

        assertEquals(teamBefore, viewModel.gameState.value!!.currentTeamIndex)
    }

    // ───── Time preservation between rounds ─────

    @Test
    fun `startTurn uses preserved remaining time from previous round when below full duration`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(teamCount = 2, wordCount = 1, durationSeconds = 60)
        viewModel.startTurn()
        runCurrent()

        // Таймер отрабатывает 20 секунд, остаётся 40
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(40, viewModel.gameState.value!!.remainingTimeSeconds)

        // Слова кончились → endTurn (внутри guessWord) → TURN_ENDED, время 40 сохранено
        viewModel.guessWord()
        assertEquals(40, viewModel.gameState.value!!.remainingTimeSeconds)

        // nextTeam: слов нет → ROUND_ENDED, время сохраняется
        viewModel.nextTeam()
        assertEquals(40, viewModel.gameState.value!!.remainingTimeSeconds)

        // nextRound: переход в PANTOMIME, время сохраняется
        viewModel.nextRound()
        assertEquals(40, viewModel.gameState.value!!.remainingTimeSeconds)

        // startTurn: должен использовать 40 (не полные 60)
        viewModel.startTurn()
        runCurrent()
        assertEquals(40, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    // ───── resetGame ─────

    @Test
    fun `resetGame clears game state teams and settings`() {
        startGameWithDefaults(teamCount = 3)
        assertNotNull(viewModel.gameState.value)
        assertEquals(3, viewModel.teams.value.size)

        viewModel.resetGame()

        assertNull(viewModel.gameState.value)
        assertEquals(emptyList<Any>(), viewModel.teams.value)
        assertEquals(HatSettings(), viewModel.settings.value)
    }

    // ───── toggleLastWordAccepted ─────

    @Test
    fun `toggleLastWordAccepted accepts the remaining word and gives the team plus one`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()
        val pending = viewModel.gameState.value!!.currentWord!!
        // Кончается таймер → TURN_ENDED, currentWord остался
        advanceTimeBy(5_000)
        advanceUntilIdle()
        assertEquals(HatGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)

        viewModel.toggleLastWordAccepted()

        val state = viewModel.gameState.value!!
        assertTrue(pending in state.guessedInTurn)
        assertEquals(1, state.teams[0].scores[HatRound.EXPLAIN])
        assertFalse(pending in state.remainingWords)
    }

    @Test
    fun `toggleLastWordAccepted twice reverts to original state`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()
        val pending = viewModel.gameState.value!!.currentWord!!
        advanceTimeBy(5_000)
        advanceUntilIdle()

        viewModel.toggleLastWordAccepted()
        viewModel.toggleLastWordAccepted()

        val state = viewModel.gameState.value!!
        assertFalse(pending in state.guessedInTurn)
        assertEquals(0, state.teams[0].totalScore)
        assertTrue(pending in state.remainingWords)
    }

    @Test
    fun `toggleLastWordAccepted is no-op outside of TURN_ENDED phase`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults()
        viewModel.startTurn()
        runCurrent()
        val before = viewModel.gameState.value!!

        viewModel.toggleLastWordAccepted()

        assertEquals(before, viewModel.gameState.value)
    }

    @Test
    fun `toggleLastWordAccepted is no-op when currentWord is null`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(wordCount = 1)
        viewModel.startTurn()
        runCurrent()
        viewModel.guessWord()
        // После guess last word: phase=TURN_ENDED, currentWord=null
        val before = viewModel.gameState.value!!
        assertEquals(HatGamePhase.TURN_ENDED, before.phase)
        assertEquals(null, before.currentWord)

        viewModel.toggleLastWordAccepted()

        assertEquals(before, viewModel.gameState.value)
    }

    // ───── updateSkipPenalty ─────

    @Test
    fun `updateSkipPenalty updates settings`() {
        viewModel.updateSkipPenalty(2)

        assertEquals(2, viewModel.settings.value.skipPenalty)
    }

    // ───── Timer ─────

    @Test
    fun `timer decrements remaining time every second while PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 5)
        viewModel.startTurn()
        runCurrent()

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(4, viewModel.gameState.value!!.remainingTimeSeconds)
    }

    @Test
    fun `timer transitions to TURN_ENDED when duration elapses`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 3, wordCount = 10)
        viewModel.startTurn()
        runCurrent()

        advanceTimeBy(3_000)
        advanceUntilIdle()

        assertEquals(HatGamePhase.TURN_ENDED, viewModel.gameState.value!!.phase)
    }

    @Test
    fun `timer stops when phase leaves PLAYING`() = runTest(mainDispatcherRule.testDispatcher) {
        startGameWithDefaults(durationSeconds = 5, wordCount = 1)
        viewModel.startTurn()
        runCurrent()

        // Сгадывание последнего слова вызовет endTurn → TURN_ENDED → таймер должен остановиться
        viewModel.guessWord()
        val phaseAfterEnd = viewModel.gameState.value!!.phase
        val timeAfterEnd = viewModel.gameState.value!!.remainingTimeSeconds

        advanceTimeBy(10_000)
        advanceUntilIdle()

        assertEquals(phaseAfterEnd, viewModel.gameState.value!!.phase)
        assertEquals(timeAfterEnd, viewModel.gameState.value!!.remainingTimeSeconds)
    }
}
