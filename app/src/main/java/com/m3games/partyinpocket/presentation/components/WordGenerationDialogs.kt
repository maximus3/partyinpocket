package com.m3games.partyinpocket.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThemeInputDialog(
    targetWordCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var theme by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Генерация слов") },
        text = {
            Column {
                Text("Введите тему для генерации $targetWordCount слов")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = theme,
                    onValueChange = { theme = it },
                    label = { Text("Тема") },
                    placeholder = { Text("Например: животные, профессии, фильмы") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(theme) },
                enabled = theme.isNotBlank()
            ) {
                Text("Сгенерировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun WordGenerationProgressDialog(
    attempt: Int,
    currentCount: Int,
    targetCount: Int
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Генерация слов") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Попытка $attempt из 3")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Сгенерировано: $currentCount из $targetCount")
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { currentCount.toFloat() / targetCount },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
fun PartialGenerationDialog(
    generatedCount: Int,
    targetCount: Int,
    attempts: Int,
    onContinue: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Частичная генерация") },
        text = {
            Text(
                "Сгенерировано $generatedCount из $targetCount слов за $attempts попыток. " +
                        "Продолжить генерацию или использовать текущие слова?"
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Использовать")
            }
        },
        dismissButton = {
            TextButton(onClick = onContinue) {
                Text("Продолжить")
            }
        }
    )
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ошибка") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ОК")
            }
        }
    )
}
