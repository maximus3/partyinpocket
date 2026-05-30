package com.m3games.partyinpocket.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3games.partyinpocket.data.SettingsRepository
import com.m3games.partyinpocket.data.api.ModelListService
import com.m3games.partyinpocket.data.api.ModelTestService
import com.m3games.partyinpocket.domain.model.AiModel
import com.m3games.partyinpocket.domain.model.AiProvider
import com.m3games.partyinpocket.domain.model.AiSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val modelListService = ModelListService()
    private val modelTestService = ModelTestService()

    val aiSettings: StateFlow<AiSettings> = settingsRepository.aiSettings

    private val _modelListState = MutableStateFlow<ModelListState>(ModelListState.Idle)
    val modelListState: StateFlow<ModelListState> = _modelListState.asStateFlow()

    private val _testResults = MutableStateFlow<Map<String, ModelTestState>>(emptyMap())
    val testResults: StateFlow<Map<String, ModelTestState>> = _testResults.asStateFlow()

    private val _isBatchTesting = MutableStateFlow(false)
    val isBatchTesting: StateFlow<Boolean> = _isBatchTesting.asStateFlow()

    private var batchJob: Job? = null

    fun saveAiSettings(settings: AiSettings) {
        settingsRepository.saveAiSettings(settings)
    }

    fun fetchModels(
        provider: AiProvider,
        customBaseUrl: String,
        token: String,
        freeOnly: Boolean
    ) {
        viewModelScope.launch {
            _modelListState.value = ModelListState.Loading
            val result = modelListService.fetchModels(provider, customBaseUrl, token, freeOnly)
            _modelListState.value = result.fold(
                onSuccess = { models ->
                    if (models.isEmpty()) {
                        ModelListState.Error("Список моделей пустой")
                    } else {
                        ModelListState.Success(models)
                    }
                },
                onFailure = { error ->
                    ModelListState.Error(error.message ?: "Не удалось загрузить модели")
                }
            )
        }
    }

    fun resetModelListState() {
        _modelListState.value = ModelListState.Idle
    }

    // ─── Тестирование моделей ───

    fun testSingleModel(baseUrl: String, token: String, modelId: String) {
        viewModelScope.launch {
            updateTestState(modelId, ModelTestState.Testing)
            val outcome = modelTestService.testModel(baseUrl, token, modelId)
            updateTestState(modelId, outcome.toState())
        }
    }

    fun testAllModels(baseUrl: String, token: String, modelIds: List<String>) {
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _isBatchTesting.value = true
            try {
                modelIds.forEach { id ->
                    updateTestState(id, ModelTestState.Testing)
                    val outcome = modelTestService.testModel(baseUrl, token, id)
                    updateTestState(id, outcome.toState())
                }
            } finally {
                _isBatchTesting.value = false
            }
        }
    }

    fun cancelBatchTesting() {
        batchJob?.cancel()
        _isBatchTesting.value = false
        // Откатываем висящие Testing назад в NotTested
        _testResults.value = _testResults.value.mapValues { (_, state) ->
            if (state is ModelTestState.Testing) ModelTestState.NotTested else state
        }
    }

    fun resetTestResults() {
        cancelBatchTesting()
        _testResults.value = emptyMap()
    }

    private fun updateTestState(modelId: String, state: ModelTestState) {
        _testResults.value = _testResults.value + (modelId to state)
    }

    private fun ModelTestService.Outcome.toState(): ModelTestState = when (this) {
        is ModelTestService.Outcome.Success -> ModelTestState.Success(durationMs)
        is ModelTestService.Outcome.Failure -> ModelTestState.Error(message)
    }

    override fun onCleared() {
        super.onCleared()
        modelListService.close()
        modelTestService.close()
    }
}

sealed class ModelTestState {
    data object NotTested : ModelTestState()
    data object Testing : ModelTestState()
    data class Success(val durationMs: Long) : ModelTestState()
    data class Error(val message: String) : ModelTestState()
}

sealed class ModelListState {
    data object Idle : ModelListState()
    data object Loading : ModelListState()
    data class Success(val models: List<AiModel>) : ModelListState()
    data class Error(val message: String) : ModelListState()
}
