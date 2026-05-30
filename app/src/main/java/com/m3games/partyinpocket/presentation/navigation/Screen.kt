package com.m3games.partyinpocket.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")

    // Hat Game
    data object HatSetup : Screen("hat/setup")
    data object HatTeams : Screen("hat/teams")
    data object HatGame : Screen("hat/game")
    data object HatTurnResult : Screen("hat/turn_result")
    data object HatRoundResult : Screen("hat/round_result")
    data object HatFinalResult : Screen("hat/final_result")

    // Alias
    data object AliasSetup : Screen("alias/setup")
    data object AliasTeams : Screen("alias/teams")
    data object AliasGame : Screen("alias/game")
    data object AliasTurnResult : Screen("alias/turn_result")
    data object AliasFinalResult : Screen("alias/final_result")

    // Crocodile
    data object CrocodileSetup : Screen("crocodile/setup")
    data object CrocodilePlayers : Screen("crocodile/players")
    data object CrocodileGame : Screen("crocodile/game")
    data object CrocodileTurnResult : Screen("crocodile/turn_result")
    data object CrocodileFinalResult : Screen("crocodile/final_result")

    // Spy
    data object SpySetup : Screen("spy/setup")
    data object SpyPlayers : Screen("spy/players")
    data object SpyDeal : Screen("spy/deal")
    data object SpyGame : Screen("spy/game")
}
