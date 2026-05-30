package com.m3games.partyinpocket.presentation.screens.crocodile

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGamePhase
import com.m3games.partyinpocket.domain.model.crocodile.CrocodileGameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrocodileGameScreen(
    viewModel: CrocodileViewModel,
    onNavigateToTurnResult: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return

    var showExitDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    DisposableEffect(state.phase) {
        val window = (context as? Activity)?.window
        if (state.phase == CrocodileGamePhase.PLAYING) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        BackHandler { showExitDialog = false }
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.exit_game_title)) },
            text = { Text(stringResource(R.string.exit_game_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.resetGame()
                    onExitGame()
                }) { Text(stringResource(R.string.exit_game_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.crocodile_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.crocodile_help_rule_1))
                    Text(stringResource(R.string.crocodile_help_rule_2))
                    Text(stringResource(R.string.crocodile_help_rule_3))
                    Text(stringResource(R.string.crocodile_help_rule_4))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.crocodile_title_short)) },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = stringResource(R.string.help),
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
                CrocodileGamePhase.READY_TO_START -> ReadyToStartContent(
                    state = state,
                    onStart = { viewModel.startTurn() }
                )
                CrocodileGamePhase.PLAYING -> PlayingContent(
                    state = state,
                    useTimer = state.settings.useTimer,
                    onGuess = { viewModel.guessWord() },
                    onSkip = { viewModel.skipWord() },
                    onFinish = { viewModel.finishTurn() }
                )
                CrocodileGamePhase.TURN_ENDED -> { onNavigateToTurnResult() }
                CrocodileGamePhase.GAME_FINISHED -> {}
            }
        }
    }
}

@Composable
private fun ReadyToStartContent(state: CrocodileGameState, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.crocodile_now_showing),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.currentPlayer.name,
            style = MaterialTheme.typography.headlineLarge,
            color = state.currentPlayer.color
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.crocodile_progress, state.totalScored, state.targetTotalWords),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.crocodile_explain_rule),
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
            Text(text = stringResource(R.string.game_start), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PlayingContent(
    state: CrocodileGameState,
    useTimer: Boolean,
    onGuess: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (useTimer) {
            Text(
                text = formatTime(state.remainingTimeSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = if (state.remainingTimeSeconds <= 10) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        } else {
            Text(
                text = state.currentPlayer.name,
                style = MaterialTheme.typography.headlineMedium,
                color = state.currentPlayer.color
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = stringResource(R.string.crocodile_turn_score, state.guessedInTurn.size),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.crocodile_skipped, state.skippedInTurn.size),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = state.currentPlayer.color.copy(alpha = 0.3f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.currentWord ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        if (state.settings.maxSkipsPerTurn > 0) {
            Text(
                text = stringResource(R.string.game_skips_left, state.currentPlayerSkipsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.currentPlayerSkipsLeft == 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                enabled = state.currentPlayerSkipsLeft > 0 || state.settings.maxSkipsPerTurn == 0
            ) {
                Text(stringResource(R.string.game_skip), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onGuess,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Text(stringResource(R.string.game_guessed), style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!useTimer) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.crocodile_finish_turn))
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
