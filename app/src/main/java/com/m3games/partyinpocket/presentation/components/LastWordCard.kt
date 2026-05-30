package com.m3games.partyinpocket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m3games.partyinpocket.R

/**
 * Карточка для досчёта последнего слова, оставшегося на экране в конце хода.
 * По умолчанию слово скрыто, чтобы команда сама вспоминала о чём шла речь.
 * После раскрытия можно либо засчитать слово, либо откатить решение.
 *
 * @param displayWord текстовое представление пропущенного слова
 * @param isAccepted уже ли слово засчитано
 * @param onToggle колбэк для переключения зачёта
 */
@Composable
fun LastWordCard(
    displayWord: String,
    isAccepted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }
    val showWord = isAccepted || revealed

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isAccepted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isAccepted) {
                    stringResource(R.string.last_word_label_accepted)
                } else {
                    stringResource(R.string.last_word_label_missed)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isAccepted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (showWord) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayWord,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        color = if (isAccepted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    if (isAccepted) {
                        OutlinedButton(onClick = onToggle) {
                            Text(stringResource(R.string.last_word_undo))
                        }
                    } else {
                        Button(onClick = onToggle) {
                            Text(stringResource(R.string.last_word_accept))
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { revealed = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.last_word_show))
                }
            }
        }
    }
}
