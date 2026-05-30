package com.m3games.partyinpocket.presentation.screens.spy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3games.partyinpocket.data.spy.PresetSpyLocations
import com.m3games.partyinpocket.domain.model.common.HiddenCard
import com.m3games.partyinpocket.domain.model.common.HiddenDealState
import com.m3games.partyinpocket.domain.model.spy.SpyGamePhase
import com.m3games.partyinpocket.domain.model.spy.SpyGameState
import com.m3games.partyinpocket.domain.model.spy.SpyGameWinner
import com.m3games.partyinpocket.domain.model.spy.SpyLocation
import com.m3games.partyinpocket.domain.model.spy.SpyPlayer
import com.m3games.partyinpocket.domain.model.spy.SpyRole
import com.m3games.partyinpocket.domain.model.spy.SpySettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpyViewModel : ViewModel() {
    private val _settings = MutableStateFlow(SpySettings())
    val settings: StateFlow<SpySettings> = _settings.asStateFlow()

    private val _playerNames = MutableStateFlow<List<String>>(emptyList())
    val playerNames: StateFlow<List<String>> = _playerNames.asStateFlow()

    private val _gameState = MutableStateFlow<SpyGameState?>(null)
    val gameState: StateFlow<SpyGameState?> = _gameState.asStateFlow()

    private var timerJob: Job? = null

    // ─── Setup ───

    fun updateSettings(newSettings: SpySettings) {
        _settings.value = newSettings
    }

    fun updatePlayerCount(count: Int) {
        _settings.value = _settings.value.copy(playerCount = count)
    }

    fun updateDiscussionDuration(seconds: Int) {
        _settings.value = _settings.value.copy(discussionDurationSeconds = seconds)
    }

    fun updateUseRoles(enabled: Boolean) {
        _settings.value = _settings.value.copy(useRoles = enabled)
    }

    fun toggleLocationPack(packId: String) {
        val currentPacks = _settings.value.selectedLocationPacks.toMutableList()
        if (currentPacks.contains(packId)) {
            if (currentPacks.size > 1) {
                currentPacks.remove(packId)
            }
        } else {
            currentPacks.add(packId)
        }
        _settings.value = _settings.value.copy(selectedLocationPacks = currentPacks)
    }

    // ─── Players ───

    fun initializePlayers() {
        val playerCount = _settings.value.playerCount
        _playerNames.value = List(playerCount) { index -> "Игрок ${index + 1}" }
    }

    fun updatePlayerName(index: Int, name: String) {
        _playerNames.value = _playerNames.value.mapIndexed { i, currentName ->
            if (i == index) name else currentName
        }
    }

    // ─── Game ───

    fun startGame() {
        val locationPacks = PresetSpyLocations.getByIds(_settings.value.selectedLocationPacks)
        val allLocations = locationPacks.flatMap { it.locations }
        if (allLocations.isEmpty()) return
        if (_playerNames.value.size < 3) return

        val chosenLocation = allLocations.random()
        val spyIndex = _playerNames.value.indices.random()

        val rolesPool = if (_settings.value.useRoles) chosenLocation.roles.shuffled().toMutableList() else mutableListOf()

        val players = _playerNames.value.mapIndexed { index, name ->
            val role: SpyRole = if (index == spyIndex) {
                SpyRole.Spy
            } else {
                val pickedRole = rolesPool.removeFirstOrNull()
                SpyRole.Civilian(location = chosenLocation.name, role = pickedRole)
            }
            SpyPlayer(index = index, name = name, role = role)
        }

        val possibleLocations = allLocations.map { it.name }.distinct().sorted()
        val cards = players.map { player ->
            when (val role = player.role) {
                SpyRole.Spy -> HiddenCard(
                    title = "Роль",
                    mainText = "Шпион",
                    subText = "Угадай локацию и не выдай себя",
                    hint = possibleLocations
                )
                is SpyRole.Civilian -> HiddenCard(
                    title = "Локация",
                    mainText = role.location,
                    subText = role.role,
                    hint = null
                )
            }
        }

        val dealState = HiddenDealState(
            playerNames = _playerNames.value,
            cards = cards
        )

        _gameState.value = SpyGameState(
            settings = _settings.value,
            players = players,
            location = chosenLocation,
            possibleLocations = possibleLocations,
            dealState = dealState,
            phase = SpyGamePhase.DEALING_ROLES,
            remainingTimeSeconds = _settings.value.discussionDurationSeconds
        )
    }

    fun dealReady() {
        val state = _gameState.value ?: return
        val deal = state.dealState ?: return
        _gameState.value = state.copy(dealState = deal.ready())
    }

    fun dealHide() {
        val state = _gameState.value ?: return
        val deal = state.dealState ?: return
        _gameState.value = state.copy(dealState = deal.hide())
    }

    fun dealNext() {
        val state = _gameState.value ?: return
        val deal = state.dealState ?: return
        val nextDeal = deal.advance()
        if (nextDeal.isFinished) {
            _gameState.value = state.copy(
                dealState = nextDeal,
                phase = SpyGamePhase.DISCUSSION
            )
            startTimer()
        } else {
            _gameState.value = state.copy(dealState = nextDeal)
        }
    }

    fun finishDiscussionEarly() {
        val state = _gameState.value ?: return
        if (state.phase != SpyGamePhase.DISCUSSION) return
        timerJob?.cancel()
        _gameState.value = state.copy(phase = SpyGamePhase.VOTING)
    }

    fun accusePlayer(playerIndex: Int) {
        val state = _gameState.value ?: return
        if (state.phase != SpyGamePhase.VOTING) return
        val accused = state.players.getOrNull(playerIndex) ?: return

        if (accused.role is SpyRole.Spy) {
            _gameState.value = state.copy(
                accusedPlayerIndex = playerIndex,
                phase = SpyGamePhase.SPY_GUESSING
            )
        } else {
            _gameState.value = state.copy(
                accusedPlayerIndex = playerIndex,
                phase = SpyGamePhase.GAME_FINISHED,
                winner = SpyGameWinner.SPY
            )
        }
    }

    fun spyGuessLocation(guessedLocation: String) {
        val state = _gameState.value ?: return
        if (state.phase != SpyGamePhase.SPY_GUESSING) return

        val winner = if (guessedLocation == state.location.name) {
            SpyGameWinner.SPY
        } else {
            SpyGameWinner.CIVILIANS
        }

        _gameState.value = state.copy(
            phase = SpyGamePhase.GAME_FINISHED,
            spyGuessedLocation = guessedLocation,
            winner = winner
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val state = _gameState.value ?: return@launch
            var remaining = state.remainingTimeSeconds

            while (remaining > 0 && _gameState.value?.phase == SpyGamePhase.DISCUSSION) {
                delay(1000)
                remaining--
                _gameState.value = _gameState.value?.copy(remainingTimeSeconds = remaining)
            }

            if (_gameState.value?.phase == SpyGamePhase.DISCUSSION) {
                _gameState.value = _gameState.value?.copy(phase = SpyGamePhase.VOTING)
            }
        }
    }

    fun resetGame() {
        timerJob?.cancel()
        _gameState.value = null
        _playerNames.value = emptyList()
        _settings.value = SpySettings()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
