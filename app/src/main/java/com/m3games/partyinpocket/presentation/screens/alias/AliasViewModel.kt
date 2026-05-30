package com.m3games.partyinpocket.presentation.screens.alias

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3games.partyinpocket.data.api.WordGenerationService
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.AiSettings
import com.m3games.partyinpocket.domain.model.Player
import com.m3games.partyinpocket.domain.model.WordGenerationState
import com.m3games.partyinpocket.domain.model.WordPack
import com.m3games.partyinpocket.domain.model.alias.AliasGamePhase
import com.m3games.partyinpocket.domain.model.alias.AliasGameState
import com.m3games.partyinpocket.domain.model.alias.AliasSettings
import com.m3games.partyinpocket.domain.model.alias.AliasWinCondition
import com.m3games.partyinpocket.domain.model.alias.AliasWord
import com.m3games.partyinpocket.presentation.theme.teamColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AliasViewModel : ViewModel() {
    private val wordGenerationService = WordGenerationService()

    private val _settings = MutableStateFlow(AliasSettings())
    val settings: StateFlow<AliasSettings> = _settings.asStateFlow()

    private val _teams = MutableStateFlow<List<Player>>(emptyList())
    val teams: StateFlow<List<Player>> = _teams.asStateFlow()

    private val _gameState = MutableStateFlow<AliasGameState?>(null)
    val gameState: StateFlow<AliasGameState?> = _gameState.asStateFlow()

    private val _wordGenerationState = MutableStateFlow<WordGenerationState>(WordGenerationState.Idle)
    val wordGenerationState: StateFlow<WordGenerationState> = _wordGenerationState.asStateFlow()

    private var timerJob: Job? = null

    // ─── Setup ───

    fun updateSettings(newSettings: AliasSettings) {
        _settings.value = newSettings
    }

    fun updateTeamCount(count: Int) {
        _settings.value = _settings.value.copy(teamCount = count)
    }

    fun updateTurnDuration(seconds: Int) {
        _settings.value = _settings.value.copy(turnDurationSeconds = seconds)
    }

    fun updateMaxSkips(skips: Int) {
        _settings.value = _settings.value.copy(maxSkipsPerTurn = skips)
    }

    fun updateSkipPenalty(penalty: Int) {
        _settings.value = _settings.value.copy(skipPenalty = penalty)
    }

    fun updateWinCondition(condition: AliasWinCondition) {
        _settings.value = _settings.value.copy(winCondition = condition)
    }

    fun updateTargetScore(score: Int) {
        _settings.value = _settings.value.copy(targetScore = score)
    }

    fun updateMaxRounds(rounds: Int) {
        _settings.value = _settings.value.copy(maxRounds = rounds)
    }

    fun toggleWordPack(packId: String) {
        val currentPacks = _settings.value.selectedPacks.toMutableList()
        if (currentPacks.contains(packId)) {
            if (currentPacks.size > 1) {
                currentPacks.remove(packId)
            }
        } else {
            currentPacks.add(packId)
        }
        _settings.value = _settings.value.copy(selectedPacks = currentPacks)
    }

    // ─── Teams ───

    fun initializeTeams() {
        val teamCount = _settings.value.teamCount
        val teams = List(teamCount) { index ->
            Player.create(
                id = index,
                name = "Команда ${index + 1}",
                color = teamColors[index % teamColors.size]
            )
        }
        _teams.value = teams
    }

    fun updateTeamName(teamId: Int, name: String) {
        _teams.value = _teams.value.map { team ->
            if (team.id == teamId) team.copy(name = name) else team
        }
    }

    fun updateTeamColor(teamId: Int, color: Color) {
        _teams.value = _teams.value.map { team ->
            if (team.id == teamId) team.copy(colorArgb = color.toArgb()) else team
        }
    }

    // ─── Game ───

    fun startGame() {
        val allWords = collectAliasWords()
        if (allWords.isEmpty()) return

        val teamSkips = _teams.value.associate { team ->
            team.id to _settings.value.maxSkipsPerTurn
        }

        _gameState.value = AliasGameState(
            settings = _settings.value,
            teams = _teams.value,
            remainingWords = allWords.shuffled(),
            currentWord = null,
            currentTeamIndex = 0,
            roundNumber = 1,
            teamSkipsLeft = teamSkips,
            phase = AliasGamePhase.READY_TO_START,
            remainingTimeSeconds = _settings.value.turnDurationSeconds
        )
    }

    private fun collectAliasWords(): List<AliasWord> {
        val packs = PresetWordPacks.getByIds(_settings.value.selectedPacks)
        return packs.flatMap { it.words }.distinct().map { AliasWord(it) }
    }

    fun getTotalAvailableWords(): Int = collectAliasWords().size

    fun startTurn() {
        val state = _gameState.value ?: return

        val nextWord = state.remainingWords.firstOrNull()

        // Refresh skips for the team starting its turn (classic Alias: skips reset every turn)
        val refreshedSkips = state.teamSkipsLeft.toMutableMap()
        refreshedSkips[state.currentTeamIndex] = _settings.value.maxSkipsPerTurn

        _gameState.value = state.copy(
            currentWord = nextWord,
            phase = AliasGamePhase.PLAYING,
            remainingTimeSeconds = _settings.value.turnDurationSeconds,
            teamSkipsLeft = refreshedSkips,
            guessedInTurn = emptyList(),
            skippedInTurn = emptyList()
        )

        startTimer()
    }

    fun guessWord() {
        val state = _gameState.value ?: return
        val word = state.currentWord ?: return

        val updatedWords = state.remainingWords.drop(1)
        val updatedTeams = state.teams.mapIndexed { index, team ->
            if (index == state.currentTeamIndex) team.withScore(1) else team
        }

        _gameState.value = state.copy(
            remainingWords = updatedWords,
            currentWord = updatedWords.firstOrNull(),
            guessedInTurn = state.guessedInTurn + word,
            teams = updatedTeams
        )

        if (updatedWords.isEmpty()) {
            endTurn()
        }
    }

    fun skipWord() {
        val state = _gameState.value ?: return
        val word = state.currentWord ?: return
        if (state.currentTeamSkipsLeft <= 0) return

        // Skipped words go to the bottom of the deck so another team may still get them.
        val updatedWords = state.remainingWords.drop(1) + word

        val skips = state.teamSkipsLeft.toMutableMap()
        skips[state.currentTeamIndex] = state.currentTeamSkipsLeft - 1

        var updatedTeams = state.teams
        if (_settings.value.skipPenalty > 0) {
            updatedTeams = state.teams.mapIndexed { index, team ->
                if (index == state.currentTeamIndex) team.withScore(-_settings.value.skipPenalty) else team
            }
        }

        _gameState.value = state.copy(
            remainingWords = updatedWords,
            currentWord = updatedWords.firstOrNull(),
            skippedInTurn = state.skippedInTurn + word,
            teamSkipsLeft = skips,
            teams = updatedTeams
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val state = _gameState.value ?: return@launch
            var remaining = state.remainingTimeSeconds

            while (remaining > 0 && _gameState.value?.phase == AliasGamePhase.PLAYING) {
                delay(1000)
                remaining--
                _gameState.value = _gameState.value?.copy(remainingTimeSeconds = remaining)
            }

            if (_gameState.value?.phase == AliasGamePhase.PLAYING) {
                endTurn()
            }
        }
    }

    private fun endTurn() {
        timerJob?.cancel()
        val state = _gameState.value ?: return
        _gameState.value = state.copy(phase = AliasGamePhase.TURN_ENDED)
    }

    /**
     * Включает/выключает зачёт слова, оставшегося в конце хода.
     */
    fun toggleLastWordAccepted() {
        val state = _gameState.value ?: return
        if (state.phase != AliasGamePhase.TURN_ENDED) return
        val pending = state.currentWord ?: return

        if (pending in state.guessedInTurn) {
            val updatedTeams = state.teams.mapIndexed { index, team ->
                if (index == state.currentTeamIndex) team.withScore(-1) else team
            }
            _gameState.value = state.copy(
                guessedInTurn = state.guessedInTurn.filterNot { it == pending },
                remainingWords = listOf(pending) + state.remainingWords,
                teams = updatedTeams
            )
        } else {
            val updatedTeams = state.teams.mapIndexed { index, team ->
                if (index == state.currentTeamIndex) team.withScore(1) else team
            }
            _gameState.value = state.copy(
                guessedInTurn = state.guessedInTurn + pending,
                remainingWords = state.remainingWords.drop(1),
                teams = updatedTeams
            )
        }
    }

    fun nextTeam() {
        val state = _gameState.value ?: return

        val isLastTeam = state.isLastTeamInRound
        val newRoundNumber = if (isLastTeam) state.roundNumber + 1 else state.roundNumber

        val noWordsLeft = state.remainingWords.isEmpty()
        val byScoreFinish = isLastTeam &&
            state.settings.winCondition == AliasWinCondition.BY_SCORE &&
            state.anyTeamReachedTarget
        val byRoundsFinish = isLastTeam &&
            state.settings.winCondition == AliasWinCondition.BY_ROUNDS &&
            state.roundNumber >= state.settings.maxRounds

        if (noWordsLeft || byScoreFinish || byRoundsFinish) {
            _gameState.value = state.copy(phase = AliasGamePhase.GAME_FINISHED)
            return
        }

        _gameState.value = state.copy(
            currentTeamIndex = state.nextTeamIndex,
            roundNumber = newRoundNumber,
            phase = AliasGamePhase.READY_TO_START,
            currentWord = null,
            guessedInTurn = emptyList(),
            skippedInTurn = emptyList(),
            remainingTimeSeconds = _settings.value.turnDurationSeconds,
            remainingWords = state.remainingWords.shuffled()
        )
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = null
        _teams.value = emptyList()
        _settings.value = AliasSettings()
    }

    // ─── Word Generation (reuses Hat's flow) ───

    fun startWordGeneration(theme: String, targetCount: Int, aiSettings: AiSettings) {
        viewModelScope.launch {
            _wordGenerationState.value = WordGenerationState.Loading(1, 0)

            val result = wordGenerationService.generateWordsWithRetry(
                theme = theme,
                targetCount = targetCount,
                settings = aiSettings,
                maxAttempts = 3
            ) { attempt, currentCount ->
                _wordGenerationState.value = WordGenerationState.Loading(attempt, currentCount)
            }

            if (result.isSuccess) {
                val (words, isComplete) = result.getOrNull()!!
                if (isComplete || words.size >= targetCount) {
                    _wordGenerationState.value = WordGenerationState.Success(words)
                } else {
                    _wordGenerationState.value = WordGenerationState.PartialSuccess(
                        words = words,
                        attempts = 3,
                        targetCount = targetCount
                    )
                }
            } else {
                _wordGenerationState.value = WordGenerationState.Error(
                    result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    fun continueWordGeneration(currentWords: List<String>, targetCount: Int, theme: String, aiSettings: AiSettings) {
        viewModelScope.launch {
            _wordGenerationState.value = WordGenerationState.Loading(1, currentWords.size)

            val result = wordGenerationService.generateWordsWithRetry(
                theme = theme,
                targetCount = targetCount,
                settings = aiSettings,
                maxAttempts = 3
            ) { attempt, currentCount ->
                _wordGenerationState.value = WordGenerationState.Loading(attempt, currentCount + currentWords.size)
            }

            if (result.isSuccess) {
                val (newWords, isComplete) = result.getOrNull()!!
                val allWords = (currentWords + newWords).distinct()
                if (isComplete || allWords.size >= targetCount) {
                    _wordGenerationState.value = WordGenerationState.Success(allWords)
                } else {
                    _wordGenerationState.value = WordGenerationState.PartialSuccess(
                        words = allWords,
                        attempts = 6,
                        targetCount = targetCount
                    )
                }
            } else {
                _wordGenerationState.value = WordGenerationState.Error(
                    result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                )
            }
        }
    }

    fun saveGeneratedWordPack(name: String, words: List<String>) {
        val packId = "generated_${System.currentTimeMillis()}"
        val wordPack = WordPack(
            id = packId,
            name = name,
            description = "Сгенерированный набор",
            words = words
        )

        val currentPacks = _settings.value.selectedPacks.toMutableList()
        if (!currentPacks.contains(packId)) {
            currentPacks.add(packId)
        }
        _settings.value = _settings.value.copy(selectedPacks = currentPacks)

        PresetWordPacks.addGeneratedPack(wordPack)

        resetWordGenerationState()
    }

    fun resetWordGenerationState() {
        _wordGenerationState.value = WordGenerationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        wordGenerationService.close()
    }
}
