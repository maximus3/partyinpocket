package com.m3games.partyinpocket.presentation.screens.spy

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.m3games.partyinpocket.domain.model.spy.SpyGamePhase
import com.m3games.partyinpocket.domain.model.spy.SpyGameState
import com.m3games.partyinpocket.domain.model.spy.SpyGameWinner
import com.m3games.partyinpocket.domain.model.spy.SpyRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpyGameScreen(
    viewModel: SpyViewModel,
    onPlayAgain: () -> Unit,
    onNavigateHome: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return

    var showExitDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    DisposableEffect(state.phase) {
        val window = (context as? Activity)?.window
        if (state.phase == SpyGamePhase.DISCUSSION) {
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(spyPhaseTitle(state.phase)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (state.phase) {
                SpyGamePhase.DISCUSSION -> DiscussionContent(
                    state = state,
                    onFinishEarly = { viewModel.finishDiscussionEarly() }
                )
                SpyGamePhase.VOTING -> VotingContent(
                    state = state,
                    onAccuse = { viewModel.accusePlayer(it) }
                )
                SpyGamePhase.SPY_GUESSING -> SpyGuessingContent(
                    state = state,
                    onGuess = { viewModel.spyGuessLocation(it) }
                )
                SpyGamePhase.GAME_FINISHED -> ResultContent(
                    state = state,
                    onPlayAgain = {
                        viewModel.resetGame()
                        onPlayAgain()
                    },
                    onHome = {
                        viewModel.resetGame()
                        onNavigateHome()
                    }
                )
                SpyGamePhase.DEALING_ROLES -> {} // handled by SpyDealScreen
            }
        }
    }
}

@Composable
private fun spyPhaseTitle(phase: SpyGamePhase): String = when (phase) {
    SpyGamePhase.DEALING_ROLES -> stringResource(R.string.spy_phase_dealing)
    SpyGamePhase.DISCUSSION -> stringResource(R.string.spy_phase_discussion)
    SpyGamePhase.VOTING -> stringResource(R.string.spy_phase_voting)
    SpyGamePhase.SPY_GUESSING -> stringResource(R.string.spy_phase_spy_guessing)
    SpyGamePhase.GAME_FINISHED -> stringResource(R.string.spy_phase_finished)
}

@Composable
private fun DiscussionContent(
    state: SpyGameState,
    onFinishEarly: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTime(state.remainingTimeSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = if (state.remainingTimeSeconds <= 30) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.spy_discussion_hint),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onFinishEarly,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(stringResource(R.string.spy_finish_discussion))
        }
    }
}

@Composable
private fun VotingContent(
    state: SpyGameState,
    onAccuse: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.spy_voting_hint),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.players) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(onClick = { onAccuse(player.index) }) {
                            Text(stringResource(R.string.spy_accuse))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpyGuessingContent(
    state: SpyGameState,
    onGuess: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.spy_caught_text, state.accusedPlayer?.name ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.spy_guess_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.possibleLocations) { location ->
                OutlinedButton(
                    onClick = { onGuess(location) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null
                        )
                        Text(text = location)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: SpyGameState,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (state.winner) {
                SpyGameWinner.SPY -> stringResource(R.string.spy_winner_spy)
                SpyGameWinner.CIVILIANS -> stringResource(R.string.spy_winner_civilians)
                null -> ""
            },
            style = MaterialTheme.typography.headlineLarge,
            color = when (state.winner) {
                SpyGameWinner.SPY -> MaterialTheme.colorScheme.error
                SpyGameWinner.CIVILIANS -> MaterialTheme.colorScheme.primary
                null -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.spy_result_location_was),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = state.location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.spy_result_spy_was, state.spyPlayer?.name ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (state.accusedPlayer != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.spy_result_accused_was, state.accusedPlayer!!.name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (state.spyGuessedLocation != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.spy_result_spy_guessed, state.spyGuessedLocation!!),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.spy_result_player_roles),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        state.players.forEach { player ->
            val (label, isSpy) = when (val role = player.role) {
                SpyRole.Spy -> stringResource(R.string.spy_label_spy) to true
                is SpyRole.Civilian -> {
                    val roleText = role.role?.let { " — $it" } ?: ""
                    "${role.location}$roleText" to false
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSpy) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.final_result_play_again))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.final_result_menu))
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
