package com.m3games.partyinpocket.presentation.screens.hat

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R
import com.m3games.partyinpocket.domain.model.hat.HatGamePhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatGameScreen(
    viewModel: HatViewModel,
    onNavigateToTurnResult: () -> Unit,
    onNavigateToRoundResult: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return

    var showExitDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Keep screen on during game play
    val context = LocalContext.current
    DisposableEffect(state.phase) {
        val window = (context as? Activity)?.window
        if (state.phase == HatGamePhase.PLAYING) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back button press
    BackHandler {
        showExitDialog = true
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        BackHandler {
            showExitDialog = false
        }

        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выход из игры") },
            text = { Text("Вы уверены, что хотите выйти? Прогресс игры не сохранится.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.resetGame()
                        onExitGame()
                    }
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Help dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Как играть в Шляпу") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Правила игры:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("• Один игрок из команды объясняет слова остальным")
                    Text("• Нажмите \"Угадал\" если слово угадали")
                    Text("• Нажмите \"Пропустить\" чтобы перейти к следующему слову")
                    Text("• У вас ограниченное время и количество пропусков")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(state.currentRound.titleRes) + ":",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(stringResource(state.currentRound.rulesRes))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(state.currentRound.titleRes) +
                        " - Раунд ${state.currentRound.number}"
                    )
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = "Помощь",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (state.phase) {
                HatGamePhase.READY_TO_START -> ReadyToStartContent(
                    state = state,
                    onStart = { viewModel.startTurn() }
                )
                HatGamePhase.PLAYING -> PlayingContent(
                    state = state,
                    onGuess = { viewModel.guessWord() },
                    onSkip = { viewModel.skipWord() }
                )
                HatGamePhase.TURN_ENDED -> {
                    onNavigateToTurnResult()
                }
                HatGamePhase.ROUND_ENDED -> {
                    onNavigateToRoundResult()
                }
                HatGamePhase.GAME_FINISHED -> {
                    // Handled by navigation
                }
            }
        }
    }
}

@Composable
private fun ReadyToStartContent(
    state: com.m3games.partyinpocket.domain.model.hat.HatGameState,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(state.currentRound.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.currentTeam.name,
            style = MaterialTheme.typography.headlineLarge,
            color = state.currentTeam.color
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.game_ready),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(state.currentRound.rulesRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(64.dp)
        ) {
            Text(
                text = stringResource(R.string.game_start),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun PlayingContent(
    state: com.m3games.partyinpocket.domain.model.hat.HatGameState,
    onGuess: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        Text(
            text = formatTime(state.remainingTimeSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = if (state.remainingTimeSeconds <= 10) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Team Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = state.currentTeam.name,
                style = MaterialTheme.typography.titleMedium,
                color = state.currentTeam.color
            )
            Text(
                text = stringResource(
                    R.string.game_score,
                    state.guessedInTurn.size,
                    state.skippedInTurn.size
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Words Left
        Text(
            text = stringResource(R.string.game_words_left, state.remainingWords.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = state.currentTeam.color.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.currentWord ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        // Skips Left
        if (state.settings.maxSkipsPerTurn > 0) {
            Text(
                text = stringResource(R.string.game_skips_left, state.currentTeamSkipsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.currentTeamSkipsLeft == 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                enabled = state.currentTeamSkipsLeft > 0 || state.settings.maxSkipsPerTurn == 0
            ) {
                Text(
                    text = stringResource(R.string.game_skip),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onGuess,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Text(
                    text = stringResource(R.string.game_guessed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
