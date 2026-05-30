package com.m3games.partyinpocket.domain.model.spy

import com.m3games.partyinpocket.domain.model.common.HiddenDealState

data class SpyGameState(
    val settings: SpySettings,
    val players: List<SpyPlayer>,
    val location: SpyLocation,
    val possibleLocations: List<String>,
    val dealState: HiddenDealState?,
    val phase: SpyGamePhase,
    val remainingTimeSeconds: Int,
    val accusedPlayerIndex: Int? = null,
    val spyGuessedLocation: String? = null,
    val winner: SpyGameWinner? = null
) {
    val spyPlayer: SpyPlayer?
        get() = players.firstOrNull { it.role is SpyRole.Spy }

    val accusedPlayer: SpyPlayer?
        get() = accusedPlayerIndex?.let { players.getOrNull(it) }
}

enum class SpyGamePhase {
    DEALING_ROLES,
    DISCUSSION,
    VOTING,
    SPY_GUESSING,
    GAME_FINISHED
}

enum class SpyGameWinner {
    SPY,
    CIVILIANS
}
