package com.m3games.partyinpocket.presentation.screens.crocodile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3games.partyinpocket.R
import com.m3games.partyinpocket.data.wordpacks.PresetWordPacks
import com.m3games.partyinpocket.domain.model.AiSettings
import com.m3games.partyinpocket.domain.model.WordGenerationState
import com.m3games.partyinpocket.presentation.components.ErrorDialog
import com.m3games.partyinpocket.presentation.components.PartialGenerationDialog
import com.m3games.partyinpocket.presentation.components.ThemeInputDialog
import com.m3games.partyinpocket.presentation.components.WordGenerationProgressDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrocodileSetupScreen(
    viewModel: CrocodileViewModel,
    aiSettings: AiSettings,
    onNavigateBack: () -> Unit,
    onNavigateToPlayers: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val generationState by viewModel.wordGenerationState.collectAsState()
    val allPacks = PresetWordPacks.getAll()

    var showThemeDialog by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf("") }
    var showSuccessNameDialog by remember { mutableStateOf(false) }
    var showPartialNameDialog by remember { mutableStateOf(false) }
    var savedPackName by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.crocodile_setup_title)) },
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
                    text = "${stringResource(R.string.crocodile_players)}: ${settings.playerCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.playerCount.toFloat(),
                    onValueChange = { viewModel.updatePlayerCount(it.roundToInt()) },
                    valueRange = 2f..20f,
                    steps = 17
                )
            }

            Column {
                Text(
                    text = "${stringResource(R.string.crocodile_target_per_player)}: ${settings.targetWordsPerPlayer}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.targetWordsPerPlayer.toFloat(),
                    onValueChange = { viewModel.updateTargetWordsPerPlayer(it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.crocodile_use_timer),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = settings.useTimer,
                    onCheckedChange = { viewModel.updateUseTimer(it) }
                )
            }

            if (settings.useTimer) {
                Column {
                    Text(
                        text = "${stringResource(R.string.setup_time)}: ${settings.turnDurationSeconds}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = settings.turnDurationSeconds.toFloat(),
                        onValueChange = { viewModel.updateTurnDuration(it.roundToInt()) },
                        valueRange = 30f..180f,
                        steps = 14
                    )
                }
            }

            Column {
                Text(
                    text = "${stringResource(R.string.setup_skips_per_player)}: ${settings.maxSkipsPerTurn}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.maxSkipsPerTurn.toFloat(),
                    onValueChange = { viewModel.updateMaxSkips(it.roundToInt()) },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }

            Column {
                Text(
                    text = "${stringResource(R.string.setup_skip_penalty)}: ${settings.skipPenalty}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.skipPenalty.toFloat(),
                    onValueChange = { viewModel.updateSkipPenalty(it.roundToInt()) },
                    valueRange = 0f..3f,
                    steps = 2
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.setup_word_packs),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPacks.forEach { pack ->
                        FilterChip(
                            selected = settings.selectedPacks.contains(pack.id),
                            onClick = { viewModel.toggleWordPack(pack.id) },
                            label = { Text(pack.name) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.setup_available_words, viewModel.getTotalAvailableWords()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = aiSettings.token.isNotBlank()
                ) {
                    Text(stringResource(R.string.setup_generate_ai))
                }

                if (aiSettings.token.isBlank()) {
                    val annotatedText = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        ) {
                            append(stringResource(R.string.setup_ai_token_prompt) + " ")
                            pushStringAnnotation(tag = "SETTINGS", annotation = "settings")
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(stringResource(R.string.setup_ai_token_link))
                            }
                            pop()
                        }
                    }
                    ClickableText(
                        text = annotatedText,
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "SETTINGS",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { onNavigateToSettings() }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
            title = { Text(stringResource(R.string.crocodile_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

    if (showThemeDialog) {
        ThemeInputDialog(
            targetWordCount = settings.targetWordsPerPlayer * settings.playerCount,
            onDismiss = { showThemeDialog = false },
            onConfirm = { theme ->
                currentTheme = theme
                showThemeDialog = false
                viewModel.startWordGeneration(theme, settings.targetWordsPerPlayer * settings.playerCount, aiSettings)
            }
        )
    }

    when (val state = generationState) {
        is WordGenerationState.Loading -> {
            WordGenerationProgressDialog(
                attempt = state.attempt,
                currentCount = state.generatedCount,
                targetCount = settings.targetWordsPerPlayer * settings.playerCount
            )
        }

        is WordGenerationState.Success -> {
            if (!showSuccessNameDialog) {
                showSuccessNameDialog = true
                savedPackName = currentTheme
            }

            if (showSuccessNameDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.gen_success_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.gen_success_count, state.words.size))
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = savedPackName,
                                onValueChange = { savedPackName = it },
                                label = { Text(stringResource(R.string.gen_pack_name_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.saveGeneratedWordPack(savedPackName, state.words)
                                showSuccessNameDialog = false
                            }
                        ) { Text(stringResource(R.string.gen_save)) }
                    }
                )
            }
        }

        is WordGenerationState.PartialSuccess -> {
            if (showPartialNameDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.gen_partial_save_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.gen_partial_save_text, state.words.size))
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = savedPackName,
                                onValueChange = { savedPackName = it },
                                label = { Text(stringResource(R.string.gen_pack_name_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.saveGeneratedWordPack(savedPackName, state.words)
                                showPartialNameDialog = false
                            }
                        ) { Text(stringResource(R.string.gen_save)) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showPartialNameDialog = false
                                viewModel.resetWordGenerationState()
                            }
                        ) { Text(stringResource(R.string.cancel)) }
                    }
                )
            } else {
                PartialGenerationDialog(
                    generatedCount = state.words.size,
                    targetCount = state.targetCount,
                    attempts = state.attempts,
                    onContinue = {
                        viewModel.continueWordGeneration(
                            currentWords = state.words,
                            targetCount = state.targetCount,
                            theme = currentTheme,
                            aiSettings = aiSettings
                        )
                    },
                    onAccept = {
                        savedPackName = currentTheme
                        showPartialNameDialog = true
                    }
                )
            }
        }

        is WordGenerationState.Error -> {
            ErrorDialog(
                message = "${stringResource(R.string.gen_error_prefix)} ${state.message}",
                onDismiss = { viewModel.resetWordGenerationState() }
            )
        }

        WordGenerationState.Idle -> {}
    }
}
