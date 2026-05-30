package com.m3games.partyinpocket.domain.model.alias

import com.m3games.partyinpocket.domain.model.Player

data class AliasGameState(
    val settings: AliasSettings,
    val teams: List<Player>,
    val remainingWords: List<AliasWord>,
    val currentWord: AliasWord?,
    val currentTeamIndex: Int,
    val roundNumber: Int,
    val guessedInTurn: List<AliasWord> = emptyList(),
    val skippedInTurn: List<AliasWord> = emptyList(),
    val teamSkipsLeft: Map<Int, Int>,
    val phase: AliasGamePhase,
    val remainingTimeSeconds: Int
) {
    val currentTeam: Player
        get() = teams[currentTeamIndex]

    val currentTeamSkipsLeft: Int
        get() = teamSkipsLeft[currentTeamIndex] ?: 0

    val nextTeamIndex: Int
        get() = (currentTeamIndex + 1) % teams.size

    val isLastTeamInRound: Boolean
        get() = currentTeamIndex == teams.size - 1

    val anyTeamReachedTarget: Boolean
        get() = teams.any { it.score >= settings.targetScore }
}

enum class AliasGamePhase {
    READY_TO_START,
    PLAYING,
    TURN_ENDED,
    GAME_FINISHED
}
