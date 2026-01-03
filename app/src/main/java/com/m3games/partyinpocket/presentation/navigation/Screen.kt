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

    // Future games
    // data object AliasSetup : Screen("alias/setup")
    // data object CrocodileSetup : Screen("crocodile/setup")
    // data object Spy : Screen("spy/setup")
}
