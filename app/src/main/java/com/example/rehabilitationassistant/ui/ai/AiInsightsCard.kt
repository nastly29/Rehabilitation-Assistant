package com.example.rehabilitationassistant.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rehabilitationassistant.ml.ActivityClassifier
import kotlin.math.roundToInt

// Блок "AI Insights": прогноз моделі в реальному часі
// @Composable означає, що це функція-малювальник інтерфейсу
// Вона лише показує дані зі state, самі обчислення тут не робляться
@Composable
fun AiInsightsCard(state: AiUiState, modifier: Modifier = Modifier) {

    Card(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("AI Insights", style = MaterialTheme.typography.titleSmall)

            // Головний рядок: "Поточна активність"
            // Поки вікно не накопичилось, показуємо просто "Накопичення даних..."
            Text(
                text = if (state.ready)
                    "Поточна активність: ${state.label} (${(state.confidence * 100).roundToInt()}%)"
                else state.label,
                style = MaterialTheme.typography.titleMedium
            )

            // Ймовірності всіх класів смужками (показуємо, коли є результат моделі)
            if (state.ready) {
                // проходимо по чотирьох назвах класів; i - номер класу, name - його назва
                ActivityClassifier.LABELS.forEachIndexed { i, name ->
                    // ймовірність цього класу; getOrElse - щоб не було помилки, якщо даних ще немає
                    val p = state.probabilities.getOrElse(i) { 0f }
                    // один рядок: назва - смужка - відсотки
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                        // смужка: p від 0 до 1 показує, наскільки вона заповнена; weight(1f) розтягує її на вільне місце
                        LinearProgressIndicator(progress = { p }, modifier = Modifier.weight(1f).height(6.dp))
                        Text(
                            "${(p * 100).roundToInt()}%",
                            Modifier.width(44.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}