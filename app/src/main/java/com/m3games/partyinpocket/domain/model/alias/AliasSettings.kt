package com.m3games.partyinpocket.domain.model.alias

data class AliasSettings(
    val teamCount: Int = 2,
    val turnDurationSeconds: Int = 60,
    val selectedPacks: List<String> = listOf("default"),
    val winCondition: AliasWinCondition = AliasWinCondition.BY_SCORE,
    val targetScore: Int = 30,
    val maxRounds: Int = 5,
    val skipPenalty: Int = 1,
    val maxSkipsPerTurn: Int = 5
)

enum class AliasWinCondition {
    BY_SCORE,
    BY_ROUNDS
}
