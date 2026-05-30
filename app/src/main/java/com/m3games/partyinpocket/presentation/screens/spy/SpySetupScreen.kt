package com.m3games.partyinpocket.presentation.screens.spy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R
import com.m3games.partyinpocket.data.spy.PresetSpyLocations
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpySetupScreen(
    viewModel: SpyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayers: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allPacks = PresetSpyLocations.getAll()

    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.spy_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text(
                    text = "${stringResource(R.string.spy_players_count)}: ${settings.playerCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.playerCount.toFloat(),
                    onValueChange = { viewModel.updatePlayerCount(it.roundToInt()) },
                    valueRange = 3f..10f,
                    steps = 6
                )
            }

            Column {
                Text(
                    text = "${stringResource(R.string.spy_discussion_time)}: ${settings.discussionDurationSeconds / 60}:${(settings.discussionDurationSeconds % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.discussionDurationSeconds.toFloat(),
                    onValueChange = { viewModel.updateDiscussionDuration(it.roundToInt()) },
                    valueRange = 120f..900f,
                    steps = 12
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.spy_use_roles),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.spy_use_roles_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useRoles,
                    onCheckedChange = { viewModel.updateUseRoles(it) }
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.spy_location_packs),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPacks.forEach { pack ->
                        FilterChip(
                            selected = settings.selectedLocationPacks.contains(pack.id),
                            onClick = { viewModel.toggleLocationPack(pack.id) },
                            label = { Text(pack.name) }
                        )
                    }
                }
                val totalLocations = PresetSpyLocations.getByIds(settings.selectedLocationPacks)
                    .sumOf { it.locations.size }
                Text(
                    text = stringResource(R.string.spy_locations_available, totalLocations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.initializePlayers()
                    onNavigateToPlayers()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.setup_next))
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.spy_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.spy_help_rule_1))
                    Text(stringResource(R.string.spy_help_rule_2))
                    Text(stringResource(R.string.spy_help_rule_3))
                    Text(stringResource(R.string.spy_help_rule_4))
                    Text(stringResource(R.string.spy_help_rule_5))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}
