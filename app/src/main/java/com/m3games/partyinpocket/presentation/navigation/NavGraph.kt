package com.m3games.partyinpocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m3games.partyinpocket.data.SettingsRepository
import com.m3games.partyinpocket.presentation.navigation.safePopBackStack
import com.m3games.partyinpocket.domain.model.hat.HatGamePhase
import com.m3games.partyinpocket.presentation.screens.hat.HatFinalResultScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatGameScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatRoundResultScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatSetupScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatTeamsScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatTurnResultScreen
import com.m3games.partyinpocket.presentation.screens.hat.HatViewModel
import com.m3games.partyinpocket.presentation.screens.home.HomeScreen
import com.m3games.partyinpocket.presentation.screens.settings.SettingsScreen
import com.m3games.partyinpocket.presentation.screens.settings.SettingsViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val hatViewModel: HatViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onGameSelected = { gameId ->
                    when (gameId) {
                        "hat" -> navController.navigate(Screen.HatSetup.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            val aiSettings by settingsViewModel.aiSettings.collectAsState()

            SettingsScreen(
                aiSettings = aiSettings,
                onSaveSettings = { settings ->
                    settingsViewModel.saveAiSettings(settings)
                },
                onNavigateBack = {
                    navController.safePopBackStack()
                }
            )
        }

        // Hat Game Flow
        composable(Screen.HatSetup.route) {
            val aiSettings by settingsViewModel.aiSettings.collectAsState()

            HatSetupScreen(
                viewModel = hatViewModel,
                aiSettings = aiSettings,
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToTeams = {
                    navController.navigate(Screen.HatTeams.route) {
                        popUpTo(Screen.HatSetup.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.HatTeams.route) {
            HatTeamsScreen(
                viewModel = hatViewModel,
                onNavigateBack = { navController.safePopBackStack() },
                onStartGame = {
                    navController.navigate(Screen.HatGame.route) {
                        popUpTo(Screen.HatSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.HatGame.route) {
            val gameState by hatViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    HatGamePhase.TURN_ENDED -> {
                        navController.navigate(Screen.HatTurnResult.route) {
                            popUpTo(Screen.HatGame.route) { inclusive = false }
                        }
                    }
                    HatGamePhase.ROUND_ENDED -> {
                        navController.navigate(Screen.HatRoundResult.route) {
                            popUpTo(Screen.HatGame.route) { inclusive = false }
                        }
                    }
                    HatGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.HatFinalResult.route) {
                            popUpTo(Screen.HatGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            HatGameScreen(
                viewModel = hatViewModel,
                onNavigateToTurnResult = {},
                onNavigateToRoundResult = {},
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.HatTurnResult.route) {
            HatTurnResultScreen(
                viewModel = hatViewModel,
                onNext = {
                    if (navController.currentDestination?.route == Screen.HatTurnResult.route) {
                        navController.popBackStack(Screen.HatGame.route, inclusive = false)
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.HatRoundResult.route) {
            val gameState by hatViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    HatGamePhase.READY_TO_START -> {
                        if (navController.currentDestination?.route == Screen.HatRoundResult.route) {
                            navController.popBackStack(Screen.HatGame.route, inclusive = false)
                        }
                    }
                    HatGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.HatFinalResult.route) {
                            popUpTo(Screen.HatGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            HatRoundResultScreen(
                viewModel = hatViewModel,
                onNext = {},
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.HatFinalResult.route) {
            HatFinalResultScreen(
                viewModel = hatViewModel,
                onPlayAgain = {
                    navController.navigate(Screen.HatSetup.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
