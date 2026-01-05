package com.m3games.partyinpocket.presentation.screens.hat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun HatSetupScreen(
    viewModel: HatViewModel,
    aiSettings: AiSettings,
    onNavigateBack: () -> Unit,
    onNavigateToTeams: () -> Unit,
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
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Team Count
            Column {
                Text(
                    text = "${stringResource(R.string.setup_teams)}: ${settings.teamCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.teamCount.toFloat(),
                    onValueChange = { viewModel.updateTeamCount(it.roundToInt()) },
                    valueRange = 2f..10f,
                    steps = 7
                )
            }

            // Word Count
            Column {
                Text(
                    text = "${stringResource(R.string.setup_words)}: ${settings.wordCount}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.wordCount.toFloat(),
                    onValueChange = { viewModel.updateWordCount(it.roundToInt()) },
                    valueRange = 20f..100f,
                    steps = 15
                )
            }

            // Turn Duration
            Column {
                Text(
                    text = "${stringResource(R.string.setup_time)}: ${settings.turnDurationSeconds}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.turnDurationSeconds.toFloat(),
                    onValueChange = { viewModel.updateTurnDuration(it.roundToInt()) },
                    valueRange = 30f..120f,
                    steps = 8
                )
            }

            // Max Skips
            Column {
                Text(
                    text = "${stringResource(R.string.setup_skips)}: ${settings.maxSkipsPerTurn}",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.maxSkipsPerTurn.toFloat(),
                    onValueChange = { viewModel.updateMaxSkips(it.roundToInt()) },
                    valueRange = 0f..15f,
                    steps = 14
                )
            }

            // Word Packs
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

                // Generate Button
                OutlinedButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = aiSettings.token.isNotBlank()
                ) {
                    Text("Сгенерировать набор с ИИ")
                }

                if (aiSettings.token.isBlank()) {
                    val annotatedText = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        ) {
                            append("Настройте API токен в ")
                            pushStringAnnotation(
                                tag = "SETTINGS",
                                annotation = "settings"
                            )
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("настройках")
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
                            ).firstOrNull()?.let {
                                onNavigateToSettings()
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Next Button
            Button(
                onClick = {
                    viewModel.initializeTeams()
                    onNavigateToTeams()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.setup_next))
            }
        }
    }

    // Help dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Как играть в Шляпу") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Правила игры:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("• Игроки делятся на команды")
                    Text("• Каждая команда по очереди объясняет слова")
                    Text("• Один игрок из команды объясняет, остальные угадывают")
                    Text("• За каждое угаданное слово команда получает 1 очко")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Три раунда с разными правилами:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("1️⃣ Объяснение - объясняйте слова любыми словами")
                    Text("2️⃣ Пантомима - показывайте слова жестами без слов")
                    Text("3️⃣ Одно слово - объясняйте используя только одно слово")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "💡 Совет: используйте генерацию слов с ИИ для создания тематических наборов!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    // Dialogs
    if (showThemeDialog) {
        ThemeInputDialog(
            targetWordCount = settings.wordCount,
            onDismiss = { showThemeDialog = false },
            onConfirm = { theme ->
                currentTheme = theme
                showThemeDialog = false
                viewModel.startWordGeneration(theme, settings.wordCount, aiSettings)
            }
        )
    }

    when (val state = generationState) {
        is WordGenerationState.Loading -> {
            WordGenerationProgressDialog(
                attempt = state.attempt,
                currentCount = state.generatedCount,
                targetCount = settings.wordCount
            )
        }

        is WordGenerationState.Success -> {
            if (!showSuccessNameDialog) {
                showSuccessNameDialog = true
                savedPackName = currentTheme
            }

            if (showSuccessNameDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Успешно!") },
                    text = {
                        Column {
                            Text("Сгенерировано ${state.words.size} слов")
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = savedPackName,
                                onValueChange = { savedPackName = it },
                                label = { Text("Название набора") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.saveGeneratedWordPack(savedPackName, state.words)
                                showSuccessNameDialog = false
                            }
                        ) {
                            Text("Сохранить")
                        }
                    }
                )
            }
        }

        is WordGenerationState.PartialSuccess -> {
            if (showPartialNameDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Сохранение") },
                    text = {
                        Column {
                            Text("Сохранить ${state.words.size} слов?")
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = savedPackName,
                                onValueChange = { savedPackName = it },
                                label = { Text("Название набора") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.saveGeneratedWordPack(savedPackName, state.words)
                                showPartialNameDialog = false
                            }
                        ) {
                            Text("Сохранить")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showPartialNameDialog = false
                                viewModel.resetWordGenerationState()
                            }
                        ) {
                            Text("Отмена")
                        }
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
                message = "Ошибка генерации: ${state.message}",
                onDismiss = { viewModel.resetWordGenerationState() }
            )
        }

        WordGenerationState.Idle -> {}
    }
}
