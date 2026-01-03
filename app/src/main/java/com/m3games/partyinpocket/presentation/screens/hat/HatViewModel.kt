package com.m3games.partyinpocket.presentation.screens.hat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3games.partyinpocket.data.api.WordGenerationService
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.AiSettings
import com.m3games.partyinpocket.domain.model.Team
import com.m3games.partyinpocket.domain.model.WordGenerationState
import com.m3games.partyinpocket.domain.model.WordPack
import com.m3games.partyinpocket.domain.model.hat.HatGamePhase
import com.m3games.partyinpocket.domain.model.hat.HatGameState
import com.m3games.partyinpocket.domain.model.hat.HatRound
import com.m3games.partyinpocket.domain.model.hat.HatSettings
import com.m3games.partyinpocket.presentation.theme.teamColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HatViewModel : ViewModel() {
    private val wordGenerationService = WordGenerationService()
    private val _settings = MutableStateFlow(HatSettings())
    val settings: StateFlow<HatSettings> = _settings.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _gameState = MutableStateFlow<HatGameState?>(null)
    val gameState: StateFlow<HatGameState?> = _gameState.asStateFlow()

    private val _wordGenerationState = MutableStateFlow<WordGenerationState>(WordGenerationState.Idle)
    val wordGenerationState: StateFlow<WordGenerationState> = _wordGenerationState.asStateFlow()

    private var timerJob: Job? = null

    // Setup Screen
    fun updateSettings(newSettings: HatSettings) {
        _settings.value = newSettings
    }

    fun updateTeamCount(count: Int) {
        _settings.value = _settings.value.copy(teamCount = count)
    }

    fun updateWordCount(count: Int) {
        _settings.value = _settings.value.copy(wordCount = count)
    }

    fun updateTurnDuration(seconds: Int) {
        _settings.value = _settings.value.copy(turnDurationSeconds = seconds)
    }

    fun updateMaxSkips(skips: Int) {
        _settings.value = _settings.value.copy(maxSkipsPerTurn = skips)
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

    // Teams Screen
    fun initializeTeams() {
        val teamCount = _settings.value.teamCount
        val teams = List(teamCount) { index ->
            Team.create(
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

    // Game Start
    fun startGame() {
        val allWords = collectWords()
        if (allWords.isEmpty()) return

        val selectedWords = allWords.shuffled().take(_settings.value.wordCount)

        // Initialize skips for each team
        val teamSkips = _teams.value.associate { team ->
            team.id to _settings.value.maxSkipsPerTurn
        }

        _gameState.value = HatGameState(
            settings = _settings.value,
            teams = _teams.value,
            allWords = selectedWords,
            remainingWords = selectedWords.shuffled(),
            currentRound = HatRound.EXPLAIN,
            currentTeamIndex = 0,
            currentWord = null,
            teamSkipsLeft = teamSkips,
            phase = HatGamePhase.READY_TO_START,
            remainingTimeSeconds = _settings.value.turnDurationSeconds
        )
    }

    private fun collectWords(): List<String> {
        val packs = PresetWordPacks.getByIds(_settings.value.selectedPacks)
        return packs.flatMap { it.words }.distinct()
    }

    fun getTotalAvailableWords(): Int {
        return collectWords().size
    }

    // Game Actions
    fun startTurn() {
        val state = _gameState.value ?: return

        val nextWord = state.remainingWords.firstOrNull()

        // If remainingTimeSeconds is already set (from previous round), keep it
        // Otherwise, use the default turn duration
        val timeToUse = if (state.remainingTimeSeconds > 0 && state.remainingTimeSeconds < _settings.value.turnDurationSeconds) {
            state.remainingTimeSeconds
        } else {
            _settings.value.turnDurationSeconds
        }

        _gameState.value = state.copy(
            currentWord = nextWord,
            phase = HatGamePhase.PLAYING,
            remainingTimeSeconds = timeToUse,
            guessedInTurn = emptyList(),
            skippedInTurn = emptyList()
        )

        startTimer()
    }

    fun guessWord() {
        val state = _gameState.value ?: return
        val word = state.currentWord ?: return

        val updatedWords = state.remainingWords.drop(1)
        val nextWord = updatedWords.firstOrNull()

        val updatedTeams = state.teams.mapIndexed { index, team ->
            if (index == state.currentTeamIndex) {
                team.withScore(state.currentRound, 1)
            } else {
                team
            }
        }

        _gameState.value = state.copy(
            remainingWords = updatedWords,
            currentWord = nextWord,
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

        val updatedWords = state.remainingWords.drop(1)
        val nextWord = updatedWords.firstOrNull()

        // Remove skipped word from allWords so it won't appear in next rounds
        val updatedAllWords = state.allWords.filter { it != word }

        // Update team skips
        val updatedTeamSkips = state.teamSkipsLeft.toMutableMap()
        updatedTeamSkips[state.currentTeamIndex] = state.currentTeamSkipsLeft - 1

        var updatedTeams = state.teams
        if (_settings.value.skipPenalty > 0) {
            updatedTeams = state.teams.mapIndexed { index, team ->
                if (index == state.currentTeamIndex) {
                    team.withScore(state.currentRound, -_settings.value.skipPenalty)
                } else {
                    team
                }
            }
        }

        _gameState.value = state.copy(
            allWords = updatedAllWords,
            remainingWords = updatedWords,
            currentWord = nextWord,
            skippedInTurn = state.skippedInTurn + word,
            teamSkipsLeft = updatedTeamSkips,
            teams = updatedTeams
        )

        if (updatedWords.isEmpty()) {
            endTurn()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val state = _gameState.value ?: return@launch
            var remaining = state.remainingTimeSeconds

            while (remaining > 0 && _gameState.value?.phase == HatGamePhase.PLAYING) {
                delay(1000)
                remaining--
                _gameState.value = _gameState.value?.copy(remainingTimeSeconds = remaining)
            }

            if (_gameState.value?.phase == HatGamePhase.PLAYING) {
                endTurn()
            }
        }
    }

    private fun endTurn() {
        timerJob?.cancel()
        val state = _gameState.value ?: return

        _gameState.value = state.copy(
            phase = HatGamePhase.TURN_ENDED
        )
    }

    fun nextTeam() {
        val state = _gameState.value ?: return

        // If round is over, don't switch team - keep current team for next round
        if (state.remainingWords.isEmpty()) {
            _gameState.value = state.copy(
                phase = HatGamePhase.ROUND_ENDED,
                currentWord = null,
                guessedInTurn = emptyList(),
                skippedInTurn = emptyList()
                // Keep remainingTimeSeconds for next round
            )
        } else {
            // Switch to next team and reset time to full duration
            _gameState.value = state.copy(
                currentTeamIndex = state.nextTeamIndex,
                phase = HatGamePhase.READY_TO_START,
                currentWord = null,
                guessedInTurn = emptyList(),
                skippedInTurn = emptyList(),
                remainingTimeSeconds = _settings.value.turnDurationSeconds
            )
        }
    }

    fun nextRound() {
        val state = _gameState.value ?: return
        val nextRound = state.currentRound.next()

        if (nextRound == null) {
            _gameState.value = state.copy(phase = HatGamePhase.GAME_FINISHED)
        } else {
            _gameState.value = state.copy(
                currentRound = nextRound,
                remainingWords = state.allWords.shuffled(),
                // Keep current team and remaining time from previous round
                phase = HatGamePhase.READY_TO_START,
                currentWord = null,
                guessedInTurn = emptyList(),
                skippedInTurn = emptyList()
            )
        }
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = null
        _teams.value = emptyList()
        _settings.value = HatSettings()
    }

    // Word Generation
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
                        attempts = 6, // Previous 3 + current 3
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

        // Add to selected packs
        val currentPacks = _settings.value.selectedPacks.toMutableList()
        if (!currentPacks.contains(packId)) {
            currentPacks.add(packId)
        }
        _settings.value = _settings.value.copy(selectedPacks = currentPacks)

        // Store the word pack temporarily (in a real app, you'd save to database)
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
