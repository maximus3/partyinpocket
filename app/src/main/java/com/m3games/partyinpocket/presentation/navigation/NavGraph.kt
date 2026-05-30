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
import com.m3games.partyinpocket.domain.model.alias.AliasGamePhase
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGamePhase
import com.m3games.partyinpocket.domain.model.hat.HatGamePhase
import com.m3games.partyinpocket.domain.model.spy.SpyGamePhase
import com.m3games.partyinpocket.presentation.screens.alias.AliasFinalResultScreen
import com.m3games.partyinpocket.presentation.screens.alias.AliasGameScreen
import com.m3games.partyinpocket.presentation.screens.alias.AliasSetupScreen
import com.m3games.partyinpocket.presentation.screens.alias.AliasTeamsScreen
import com.m3games.partyinpocket.presentation.screens.alias.AliasTurnResultScreen
import com.m3games.partyinpocket.presentation.screens.alias.AliasViewModel
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodileFinalResultScreen
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodileGameScreen
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodilePlayersScreen
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodileSetupScreen
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodileTurnResultScreen
import com.m3games.partyinpocket.presentation.screens.crocodile.CrocodileViewModel
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
import com.m3games.partyinpocket.presentation.screens.spy.SpyDealScreen
import com.m3games.partyinpocket.presentation.screens.spy.SpyGameScreen
import com.m3games.partyinpocket.presentation.screens.spy.SpyPlayersScreen
import com.m3games.partyinpocket.presentation.screens.spy.SpySetupScreen
import com.m3games.partyinpocket.presentation.screens.spy.SpyViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }
    val hatViewModel: HatViewModel = viewModel()
    val aliasViewModel: AliasViewModel = viewModel()
    val crocodileViewModel: CrocodileViewModel = viewModel()
    val spyViewModel: SpyViewModel = viewModel()

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
                        "alias" -> navController.navigate(Screen.AliasSetup.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                        "crocodile" -> navController.navigate(Screen.CrocodileSetup.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                        "spy" -> navController.navigate(Screen.SpySetup.route) {
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
                onSaveSettings = { settings -> settingsViewModel.saveAiSettings(settings) },
                onNavigateBack = { navController.safePopBackStack() }
            )
        }

        // ─── Hat Game Flow ───

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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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

        // ─── Alias Game Flow ───

        composable(Screen.AliasSetup.route) {
            val aiSettings by settingsViewModel.aiSettings.collectAsState()

            AliasSetupScreen(
                viewModel = aliasViewModel,
                aiSettings = aiSettings,
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToTeams = {
                    navController.navigate(Screen.AliasTeams.route) {
                        popUpTo(Screen.AliasSetup.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AliasTeams.route) {
            AliasTeamsScreen(
                viewModel = aliasViewModel,
                onNavigateBack = { navController.safePopBackStack() },
                onStartGame = {
                    navController.navigate(Screen.AliasGame.route) {
                        popUpTo(Screen.AliasSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AliasGame.route) {
            val gameState by aliasViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    AliasGamePhase.TURN_ENDED -> {
                        navController.navigate(Screen.AliasTurnResult.route) {
                            popUpTo(Screen.AliasGame.route) { inclusive = false }
                        }
                    }
                    AliasGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.AliasFinalResult.route) {
                            popUpTo(Screen.AliasGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            AliasGameScreen(
                viewModel = aliasViewModel,
                onNavigateToTurnResult = {},
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.AliasTurnResult.route) {
            val gameState by aliasViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    AliasGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.AliasFinalResult.route) {
                            popUpTo(Screen.AliasGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            AliasTurnResultScreen(
                viewModel = aliasViewModel,
                onNext = {
                    if (navController.currentDestination?.route == Screen.AliasTurnResult.route) {
                        navController.popBackStack(Screen.AliasGame.route, inclusive = false)
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.AliasFinalResult.route) {
            AliasFinalResultScreen(
                viewModel = aliasViewModel,
                onPlayAgain = {
                    navController.navigate(Screen.AliasSetup.route) {
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

        // ─── Crocodile Game Flow ───

        composable(Screen.CrocodileSetup.route) {
            val aiSettings by settingsViewModel.aiSettings.collectAsState()

            CrocodileSetupScreen(
                viewModel = crocodileViewModel,
                aiSettings = aiSettings,
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToPlayers = {
                    navController.navigate(Screen.CrocodilePlayers.route) {
                        popUpTo(Screen.CrocodileSetup.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.CrocodilePlayers.route) {
            CrocodilePlayersScreen(
                viewModel = crocodileViewModel,
                onNavigateBack = { navController.safePopBackStack() },
                onStartGame = {
                    navController.navigate(Screen.CrocodileGame.route) {
                        popUpTo(Screen.CrocodileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CrocodileGame.route) {
            val gameState by crocodileViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    CrocodileGamePhase.TURN_ENDED -> {
                        navController.navigate(Screen.CrocodileTurnResult.route) {
                            popUpTo(Screen.CrocodileGame.route) { inclusive = false }
                        }
                    }
                    CrocodileGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.CrocodileFinalResult.route) {
                            popUpTo(Screen.CrocodileGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            CrocodileGameScreen(
                viewModel = crocodileViewModel,
                onNavigateToTurnResult = {},
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.CrocodileTurnResult.route) {
            val gameState by crocodileViewModel.gameState.collectAsState()

            LaunchedEffect(gameState?.phase) {
                when (gameState?.phase) {
                    CrocodileGamePhase.GAME_FINISHED -> {
                        navController.navigate(Screen.CrocodileFinalResult.route) {
                            popUpTo(Screen.CrocodileGame.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            CrocodileTurnResultScreen(
                viewModel = crocodileViewModel,
                onNext = {
                    if (navController.currentDestination?.route == Screen.CrocodileTurnResult.route) {
                        navController.popBackStack(Screen.CrocodileGame.route, inclusive = false)
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.CrocodileFinalResult.route) {
            CrocodileFinalResultScreen(
                viewModel = crocodileViewModel,
                onPlayAgain = {
                    navController.navigate(Screen.CrocodileSetup.route) {
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

        // ─── Spy Game Flow ───

        composable(Screen.SpySetup.route) {
            val aiSettings by settingsViewModel.aiSettings.collectAsState()

            SpySetupScreen(
                viewModel = spyViewModel,
                aiSettings = aiSettings,
                onNavigateBack = { navController.safePopBackStack() },
                onNavigateToPlayers = {
                    navController.navigate(Screen.SpyPlayers.route) {
                        popUpTo(Screen.SpySetup.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.SpyPlayers.route) {
            SpyPlayersScreen(
                viewModel = spyViewModel,
                onNavigateBack = { navController.safePopBackStack() },
                onStartGame = {
                    navController.navigate(Screen.SpyDeal.route) {
                        popUpTo(Screen.SpySetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SpyDeal.route) {
            SpyDealScreen(
                viewModel = spyViewModel,
                onDealFinished = {
                    navController.navigate(Screen.SpyGame.route) {
                        popUpTo(Screen.SpyDeal.route) { inclusive = true }
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.SpyGame.route) {
            SpyGameScreen(
                viewModel = spyViewModel,
                onPlayAgain = {
                    navController.navigate(Screen.SpySetup.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
    }
}
