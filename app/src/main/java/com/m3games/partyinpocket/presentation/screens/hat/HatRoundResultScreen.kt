package com.m3games.partyinpocket.presentation.screens.hat

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatRoundResultScreen(
    viewModel: HatViewModel,
    onNext: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return

    val sortedTeams = state.teams.sortedByDescending { it.totalScore }

    var buttonEnabled by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(3) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Enable button after 3 seconds with countdown
    LaunchedEffect(Unit) {
        repeat(3) {
            delay(1000)
            secondsLeft--
        }
        buttonEnabled = true
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.round_result_title, state.currentRound.number)
                    )
                }
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
                text = "Результаты",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedTeams) { team ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = team.color.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = team.color
                            )
                            Text(
                                text = "${team.totalScore}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = team.color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.nextRound()
                    onNext()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = buttonEnabled
            ) {
                Text(
                    if (buttonEnabled) {
                        stringResource(
                            if (state.currentRound.next() != null) {
                                R.string.round_result_next_round
                            } else {
                                R.string.round_result_final
                            }
                        )
                    } else {
                        "Ожидание ($secondsLeft сек)..."
                    }
                )
            }
        }
    }
}
