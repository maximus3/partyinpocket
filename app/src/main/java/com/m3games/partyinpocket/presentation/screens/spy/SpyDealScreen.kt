package com.m3games.partyinpocket.presentation.screens.spy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.m3games.partyinpocket.domain.model.common.HiddenDealPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpyDealScreen(
    viewModel: SpyViewModel,
    onDealFinished: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val state = gameState ?: return
    val deal = state.dealState ?: return

    var showExitDialog by remember { mutableStateOf(false) }

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

    if (deal.isFinished) {
        // Триггерим переход в Discussion phase
        LaunchOnce { onDealFinished() }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.spy_deal_progress, deal.currentIndex + 1, deal.totalPlayers))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (deal.phase) {
                HiddenDealPhase.WAITING_FOR_PLAYER -> WaitingForPlayer(
                    playerName = deal.currentPlayerName ?: "",
                    onReady = { viewModel.dealReady() }
                )
                HiddenDealPhase.REVEALING_CARD -> RevealCard(
                    playerName = deal.currentPlayerName ?: "",
                    cardTitle = deal.currentCard?.title ?: "",
                    cardMain = deal.currentCard?.mainText ?: "",
                    cardSub = deal.currentCard?.subText,
                    cardHint = deal.currentCard?.hint,
                    onHide = { viewModel.dealHide() }
                )
                HiddenDealPhase.HIDING_CARD -> HidingCard(
                    isLast = deal.currentIndex == deal.totalPlayers - 1,
                    onNext = { viewModel.dealNext() }
                )
            }
        }
    }
}

@Composable
private fun WaitingForPlayer(playerName: String, onReady: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.spy_deal_pass_phone),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playerName,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.spy_deal_warning),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(stringResource(R.string.spy_deal_ready), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RevealCard(
    playerName: String,
    cardTitle: String,
    cardMain: String,
    cardSub: String?,
    cardHint: List<String>?,
    onHide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = cardMain,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                if (cardSub != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = cardSub,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }

                if (cardHint != null && cardHint.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.spy_deal_possible_locations),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(cardHint) { location ->
                            Text(
                                text = "• $location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onHide,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(stringResource(R.string.spy_deal_hide), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun HidingCard(isLast: Boolean, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.spy_deal_hidden),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.spy_deal_pass_to_next),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = if (isLast) {
                    stringResource(R.string.spy_deal_start_discussion)
                } else {
                    stringResource(R.string.spy_deal_next_player)
                },
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun LaunchOnce(block: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { block() }
}
