package com.m3games.partyinpocket.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.domain.model.AiProvider
import com.m3games.partyinpocket.domain.model.AiSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val aiSettings by viewModel.aiSettings.collectAsState()
    val modelListState by viewModel.modelListState.collectAsState()

    var provider by remember { mutableStateOf(aiSettings.provider) }
    var baseUrl by remember { mutableStateOf(aiSettings.baseUrl) }
    var model by remember { mutableStateOf(aiSettings.model) }
    var token by remember { mutableStateOf(aiSettings.token) }
    var useFreeOnly by remember { mutableStateOf(aiSettings.useFreeOnly) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    // Автозагрузка при открытии экрана и при значимых изменениях.
    // Намеренно НЕ ребиндим на token, чтобы не дёргать сервис на каждый набранный символ.
    LaunchedEffect(provider, useFreeOnly) {
        viewModel.resetModelListState()
        viewModel.resetTestResults()
        if (canAutoFetch(provider, baseUrl, token, useFreeOnly)) {
            viewModel.fetchModels(provider, baseUrl, token, useFreeOnly)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки ИИ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Провайдер — выпадающий список
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Провайдер", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = providerMenuExpanded,
                    onExpandedChange = { providerMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false }
                    ) {
                        AiProvider.all.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName) },
                                onClick = {
                                    val previousProvider = provider
                                    provider = p
                                    providerMenuExpanded = false
                                    if (p != AiProvider.Custom) {
                                        baseUrl = p.defaultBaseUrl
                                        // Замена модели только если она дефолтная от другого провайдера
                                        if (model.isBlank() ||
                                            AiProvider.all.any { it.defaultModel == model }
                                        ) {
                                            model = p.defaultModel
                                        }
                                    } else if (previousProvider != AiProvider.Custom) {
                                        // При переходе в Custom не затираем уже введённый baseUrl
                                        if (baseUrl == previousProvider.defaultBaseUrl) baseUrl = ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Base URL
            if (provider == AiProvider.Custom) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (chat completions)") },
                    placeholder = { Text("https://example.com/v1/chat/completions") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.fetchModels(provider, baseUrl, token, useFreeOnly)
                            },
                            enabled = baseUrl.isNotBlank() && token.isNotBlank()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить список моделей")
                        }
                    }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Base URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = provider.defaultBaseUrl,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Free-only — только OpenRouter
            if (provider.supportsFreeFilter) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Только бесплатные модели", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Показывает только модели с :free или нулевой ценой",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useFreeOnly,
                        onCheckedChange = { useFreeOnly = it }
                    )
                }
            }

            // Модель
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Модель", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model ID") },
                    placeholder = {
                        Text(provider.defaultModel.ifBlank { "Введите ID модели или выберите из списка" })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    viewModel.fetchModels(provider, baseUrl, token, useFreeOnly)
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить список моделей")
                            }
                            IconButton(
                                onClick = { showModelPicker = true },
                                enabled = modelListState is ModelListState.Success
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать из списка")
                            }
                        }
                    }
                )

                ModelListStatus(
                    state = modelListState,
                    provider = provider,
                    tokenIsBlank = token.isBlank(),
                    customBaseUrlIsValid = provider != AiProvider.Custom || baseUrl.contains("/chat/completions"),
                    onPick = { showModelPicker = true }
                )
            }

            // Токен
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("API Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (modelListState !is ModelListState.Success) {
                        IconButton(
                            onClick = {
                                viewModel.fetchModels(provider, baseUrl, token, useFreeOnly)
                            },
                            enabled = canAutoFetch(provider, baseUrl, token, useFreeOnly)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Загрузить список моделей")
                        }
                    }
                }
            )

            // Подсказка по OpenRouter
            if (provider == AiProvider.OpenRouter) {
                Text(
                    text = buildAnnotatedString {
                        append("Получить бесплатный API ключ на ")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) { append("OpenRouter") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://openrouter.ai/keys")
                    }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Как получить API ключ:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "1. Нажмите на ссылку OpenRouter выше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "2. Зарегистрируйтесь или войдите в аккаунт",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "3. Нажмите кнопку \"Create Key\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "4. Скопируйте созданный ключ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "5. Вставьте его в поле \"API Token\" выше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveAiSettings(
                        AiSettings(
                            provider = provider,
                            baseUrl = baseUrl,
                            model = model,
                            token = token,
                            useFreeOnly = useFreeOnly
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }

    if (showModelPicker) {
        val testResults by viewModel.testResults.collectAsState()
        val isBatchTesting by viewModel.isBatchTesting.collectAsState()

        ModelPickerDialog(
            state = modelListState,
            testResults = testResults,
            isBatchTesting = isBatchTesting,
            canTest = baseUrl.isNotBlank() && token.isNotBlank(),
            showTestAll = provider == AiProvider.OpenRouter && useFreeOnly,
            onDismiss = { showModelPicker = false },
            onPick = { picked ->
                model = picked
                showModelPicker = false
            },
            onRetry = {
                viewModel.fetchModels(provider, baseUrl, token, useFreeOnly)
            },
            onTestSingle = { modelId ->
                viewModel.testSingleModel(baseUrl, token, modelId)
            },
            onTestAll = { modelIds ->
                viewModel.testAllModels(baseUrl, token, modelIds)
            },
            onStopTesting = { viewModel.cancelBatchTesting() }
        )
    }
}

/**
 * Условие, при котором имеет смысл дернуть API за списком моделей.
 *
 * - Custom: нужен baseUrl с /chat/completions + токен.
 * - OpenRouter без freeOnly: /models публичный, токен не нужен.
 * - OpenRouter с freeOnly: /models/user требует токен.
 * - Остальные: требуют токен.
 */
private fun canAutoFetch(
    provider: AiProvider,
    baseUrl: String,
    token: String,
    freeOnly: Boolean
): Boolean = when (provider) {
    AiProvider.Custom -> baseUrl.contains("/chat/completions") && token.isNotBlank()
    AiProvider.OpenRouter -> if (freeOnly) token.isNotBlank() else true
    else -> token.isNotBlank()
}

@Composable
private fun ModelListStatus(
    state: ModelListState,
    provider: AiProvider,
    tokenIsBlank: Boolean,
    customBaseUrlIsValid: Boolean,
    onPick: () -> Unit
) {
    val (text, color, clickable) = when (state) {
        ModelListState.Loading -> Triple(
            "Загрузка списка моделей…",
            MaterialTheme.colorScheme.onSurfaceVariant,
            false
        )
        is ModelListState.Success -> Triple(
            "Доступно ${state.models.size} моделей · нажмите чтобы выбрать",
            MaterialTheme.colorScheme.primary,
            true
        )
        is ModelListState.Error -> Triple(
            "Не удалось загрузить список: ${state.message}. Введите модель вручную.",
            MaterialTheme.colorScheme.error,
            false
        )
        ModelListState.Idle -> {
            val needsTokenForOpenRouter = provider == AiProvider.OpenRouter && tokenIsBlank
            // OpenRouter без freeOnly грузится автоматически даже без токена
            val msg = when {
                provider == AiProvider.Custom && !customBaseUrlIsValid ->
                    "Укажите Base URL с /chat/completions, чтобы загрузить список моделей."
                tokenIsBlank && provider != AiProvider.OpenRouter ->
                    "Введите API ключ — список загрузится автоматически. Либо введите модель вручную."
                needsTokenForOpenRouter ->
                    "Введите API ключ — список загрузится автоматически. Либо введите модель вручную."
                else ->
                    "Список ещё не загружен."
            }
            Triple(msg, MaterialTheme.colorScheme.onSurfaceVariant, false)
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = if (clickable) Modifier.clickable { onPick() } else Modifier
    )
}

@Composable
private fun ModelPickerDialog(
    state: ModelListState,
    testResults: Map<String, ModelTestState>,
    isBatchTesting: Boolean,
    canTest: Boolean,
    showTestAll: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
    onTestSingle: (String) -> Unit,
    onTestAll: (List<String>) -> Unit,
    onStopTesting: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор модели") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state is ModelListState.Success) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Поиск") },
                        placeholder = { Text("Название или ID модели") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (showTestAll) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isBatchTesting) {
                                OutlinedButton(
                                    onClick = onStopTesting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Остановить")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onTestAll(state.models.map { it.id }) },
                                    modifier = Modifier.weight(1f),
                                    enabled = canTest
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Проверить все")
                                }
                            }
                        }
                        if (!canTest) {
                            Text(
                                text = "Для проверки нужен API токен",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                when (state) {
                    ModelListState.Idle, ModelListState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ModelListState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = onRetry) {
                                Text("Попробовать ещё раз")
                            }
                        }
                    }
                    is ModelListState.Success -> {
                        val filtered = remember(query, state.models) {
                            if (query.isBlank()) {
                                state.models
                            } else {
                                state.models.filter {
                                    it.displayName.contains(query, ignoreCase = true) ||
                                        it.id.contains(query, ignoreCase = true)
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            Text(
                                text = "Ничего не найдено",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                            ) {
                                items(filtered) { aiModel ->
                                    ModelRow(
                                        displayName = aiModel.displayName,
                                        id = aiModel.id,
                                        testState = testResults[aiModel.id] ?: ModelTestState.NotTested,
                                        canTest = canTest,
                                        onPick = { onPick(aiModel.id) },
                                        onTest = { onTestSingle(aiModel.id) }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun ModelRow(
    displayName: String,
    id: String,
    testState: ModelTestState,
    canTest: Boolean,
    onPick: () -> Unit,
    onTest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onPick() }
                .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        TestStateIndicator(
            state = testState,
            canTest = canTest,
            onTest = onTest
        )
    }
}

@Composable
private fun TestStateIndicator(
    state: ModelTestState,
    canTest: Boolean,
    onTest: () -> Unit
) {
    when (state) {
        ModelTestState.NotTested -> {
            IconButton(onClick = onTest, enabled = canTest) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Проверить модель",
                    tint = if (canTest) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
        ModelTestState.Testing -> {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        is ModelTestState.Success -> {
            Row(
                modifier = Modifier
                    .clickable(enabled = canTest) { onTest() }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is ModelTestState.Error -> {
            Row(
                modifier = Modifier
                    .clickable(enabled = canTest) { onTest() }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms} мс"
    else -> "${"%.1f".format(ms / 1000.0)} с"
}
