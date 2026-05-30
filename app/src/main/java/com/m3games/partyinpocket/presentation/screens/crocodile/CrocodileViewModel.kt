package com.m3games.partyinpocket.presentation.screens.crocodile

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
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGamePhase
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGameState
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileSettings
import com.m3games.partyinpocket.presentation.theme.teamColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CrocodileViewModel : ViewModel() {
    private val wordGenerationService = WordGenerationService()

    private val _settings = MutableStateFlow(CrocodileSettings())
    val settings: StateFlow<CrocodileSettings> = _settings.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _gameState = MutableStateFlow<CrocodileGameState?>(null)
    val gameState: StateFlow<CrocodileGameState?> = _gameState.asStateFlow()

    private val _wordGenerationState = MutableStateFlow<WordGenerationState>(WordGenerationState.Idle)
    val wordGenerationState: StateFlow<WordGenerationState> = _wordGenerationState.asStateFlow()

    private var timerJob: Job? = null

    // ─── Setup ───

    fun updateSettings(newSettings: CrocodileSettings) {
        _settings.value = newSettings
    }

    fun updatePlayerCount(count: Int) {
        _settings.value = _settings.value.copy(playerCount = count)
    }

    fun updateTargetWordsPerPlayer(count: Int) {
        _settings.value = _settings.value.copy(targetWordsPerPlayer = count)
    }

    fun updateTurnDuration(seconds: Int) {
        _settings.value = _settings.value.copy(turnDurationSeconds = seconds)
    }

    fun updateUseTimer(enabled: Boolean) {
        _settings.value = _settings.value.copy(useTimer = enabled)
    }

    fun updateMaxSkips(skips: Int) {
        _settings.value = _settings.value.copy(maxSkipsPerTurn = skips)
    }

    fun updateSkipPenalty(penalty: Int) {
        _settings.value = _settings.value.copy(skipPenalty = penalty)
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

    // ─── Players ───

    fun initializePlayers() {
        val playerCount = _settings.value.playerCount
        val players = List(playerCount) { index ->
            Player.create(
                id = index,
                name = "Игрок ${index + 1}",
                color = teamColors[index % teamColors.size]
            )
        }
        _players.value = players
    }

    fun updatePlayerName(playerId: Int, name: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) player.copy(name = name) else player
        }
    }

    fun updatePlayerColor(playerId: Int, color: Color) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) player.copy(colorArgb = color.toArgb()) else player
        }
    }

    // ─── Game ───

    fun startGame() {
        val allWords = collectWords()
        if (allWords.isEmpty()) return

        val playerSkips = _players.value.associate { player ->
            player.id to _settings.value.maxSkipsPerTurn
        }

        _gameState.value = CrocodileGameState(
            settings = _settings.value,
            players = _players.value,
            remainingWords = allWords.shuffled(),
            currentWord = null,
            currentPlayerIndex = 0,
            playerSkipsLeft = playerSkips,
            phase = CrocodileGamePhase.READY_TO_START,
            remainingTimeSeconds = _settings.value.turnDurationSeconds
        )
    }

    private fun collectWords(): List<String> {
        val packs = PresetWordPacks.getByIds(_settings.value.selectedPacks)
        return packs.flatMap { it.words }.distinct()
    }

    fun getTotalAvailableWords(): Int = collectWords().size

    fun startTurn() {
        val state = _gameState.value ?: return
        val nextWord = state.remainingWords.firstOrNull()

        val refreshedSkips = state.playerSkipsLeft.toMutableMap()
        refreshedSkips[state.currentPlayerIndex] = _settings.value.maxSkipsPerTurn

        _gameState.value = state.copy(
            currentWord = nextWord,
            phase = CrocodileGamePhase.PLAYING,
            remainingTimeSeconds = _settings.value.turnDurationSeconds,
            playerSkipsLeft = refreshedSkips,
            guessedInTurn = emptyList(),
            skippedInTurn = emptyList()
        )

        if (_settings.value.useTimer) {
            startTimer()
        }
    }

    fun guessWord() {
        val state = _gameState.value ?: return
        val word = state.currentWord ?: return

        val updatedWords = state.remainingWords.drop(1)
        val updatedPlayers = state.players.mapIndexed { index, player ->
            if (index == state.currentPlayerIndex) player.withScore(1) else player
        }

        _gameState.value = state.copy(
            remainingWords = updatedWords,
            currentWord = updatedWords.firstOrNull(),
            guessedInTurn = state.guessedInTurn + word,
            players = updatedPlayers
        )

        if (updatedWords.isEmpty()) {
            endTurn()
        }
    }

    fun skipWord() {
        val state = _gameState.value ?: return
        val word = state.currentWord ?: return
        if (state.currentPlayerSkipsLeft <= 0) return

        // В крокодиле пропущенные слова исключаются совсем — иначе можно бесконечно пропускать.
        val updatedWords = state.remainingWords.drop(1)

        val skips = state.playerSkipsLeft.toMutableMap()
        skips[state.currentPlayerIndex] = state.currentPlayerSkipsLeft - 1

        var updatedPlayers = state.players
        if (_settings.value.skipPenalty > 0) {
            updatedPlayers = state.players.mapIndexed { index, player ->
                if (index == state.currentPlayerIndex) player.withScore(-_settings.value.skipPenalty) else player
            }
        }

        _gameState.value = state.copy(
            remainingWords = updatedWords,
            currentWord = updatedWords.firstOrNull(),
            skippedInTurn = state.skippedInTurn + word,
            playerSkipsLeft = skips,
            players = updatedPlayers
        )

        if (updatedWords.isEmpty()) {
            endTurn()
        }
    }

    /**
     * Завершить ход досрочно по решению игрока (без таймера, например).
     */
    fun finishTurn() {
        if (_gameState.value?.phase == CrocodileGamePhase.PLAYING) {
            endTurn()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val state = _gameState.value ?: return@launch
            var remaining = state.remainingTimeSeconds

            while (remaining > 0 && _gameState.value?.phase == CrocodileGamePhase.PLAYING) {
                delay(1000)
                remaining--
                _gameState.value = _gameState.value?.copy(remainingTimeSeconds = remaining)
            }

            if (_gameState.value?.phase == CrocodileGamePhase.PLAYING) {
                endTurn()
            }
        }
    }

    private fun endTurn() {
        timerJob?.cancel()
        val state = _gameState.value ?: return
        _gameState.value = state.copy(phase = CrocodileGamePhase.TURN_ENDED)
    }

    fun nextPlayer() {
        val state = _gameState.value ?: return

        if (state.isGameFinished) {
            _gameState.value = state.copy(phase = CrocodileGamePhase.GAME_FINISHED)
            return
        }

        _gameState.value = state.copy(
            currentPlayerIndex = state.nextPlayerIndex,
            phase = CrocodileGamePhase.READY_TO_START,
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
        _players.value = emptyList()
        _settings.value = CrocodileSettings()
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
