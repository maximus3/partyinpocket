package com.m3games.partyinpocket.presentation.screens.alias

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliasTurnResultScreen(
    viewModel: AliasViewModel,
    onNext: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return

    var buttonEnabled by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(3) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repeat(3) {
            delay(1000)
            secondsLeft--
        }
        buttonEnabled = true
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

    val turnDelta = state.guessedInTurn.size - state.skippedInTurn.size * state.settings.skipPenalty

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.turn_result_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.currentTeam.name,
                style = MaterialTheme.typography.headlineMedium,
                color = state.currentTeam.color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.alias_turn_delta, turnDelta),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.alias_total_score, state.currentTeam.score),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.guessedInTurn.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.turn_result_guessed),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(state.guessedInTurn) { word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                if (state.skippedInTurn.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.turn_result_skipped),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.skippedInTurn) { word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.teams.forEach { team ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = team.color
                        )
                        Text(
                            text = "${team.score}",
                            style = MaterialTheme.typography.titleMedium,
                            color = team.color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.turn_result_pass_phone),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.nextTeam()
                    onNext()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = buttonEnabled
            ) {
                Text(
                    if (buttonEnabled) {
                        stringResource(R.string.turn_result_next)
                    } else {
                        stringResource(R.string.turn_result_waiting, secondsLeft)
                    }
                )
            }
        }
    }
}
