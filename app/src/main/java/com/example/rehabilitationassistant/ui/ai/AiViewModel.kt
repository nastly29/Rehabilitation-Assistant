package com.example.rehabilitationassistant.ui.ai

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rehabilitationassistant.core.model.SensorData
import com.example.rehabilitationassistant.ml.ActivityClassifier
import com.example.rehabilitationassistant.ml.SensorWindowBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Що показує блок "AI Insights"
// data class з готовими значеннями за замовчуванням: до першого прогнозу показуємо "Накопичення даних..."
data class AiUiState(
    val label: String = "Накопичення даних...",   // поточна активність
    val confidence: Float = 0f,                   // впевненість моделі (0..1)
    val probabilities: List<Float> = emptyList(), // ймовірності всіх класів
    val ready: Boolean = false                    // чи вже є перший прогноз
)

class AiViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val TAG = "AiInsights"                // тег для Logcat
        private const val SAMPLE_INTERVAL_MS = 50L  // беремо вимір не частіше ніж раз на 50 мс (20 Гц)
        private const val CLASSIFY_EVERY = 5        // класифікуємо після кожних 5 нових вимірів (=0,3 с)
    }

    private val classifier = ActivityClassifier(app)  // модель
    private val buffer = SensorWindowBuffer()         // кільцевий буфер на 20 вимірів

    // _uiState змінюємо лише тут, а назовні віддаємо тільки для читання (uiState)
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private var lastSampleAt = 0L   // час останнього прийнятого виміру
    private var sinceClassify = 0   // скільки вимірів від останнього прогнозу
    private var busy = false        // чи модель зараз рахує (щоб не запускати кілька разів одночасно)

    // Викликається з MainActivity при кожному значенні акселерометра
    fun onSensorData(raw: SensorData) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSampleAt < SAMPLE_INTERVAL_MS) return  // занадто рано, пропускаємо
        lastSampleAt = now

        buffer.add(raw.x, raw.y, raw.z)
        sinceClassify++
        // Запускаємо модель, коли виконано все разом: вікно повне, пройшло 5 вимірів, модель вільна
        if (!buffer.isFull || sinceClassify < CLASSIFY_EVERY || busy) return
        sinceClassify = 0

        val window = buffer.snapshot()  // копія вікна для моделі (буфер далі продовжує наповнюватись)
        busy = true
        viewModelScope.launch {
            val p = withContext(Dispatchers.Default) { classifier.classify(window) }
            busy = false
            _uiState.value = AiUiState(p.label, p.confidence, p.probabilities, ready = true) // екран оновиться сам
            Log.d(TAG, "%s (%.0f%%)".format(p.label, p.confidence * 100))
        }
    }

    // Звільняємо модель, коли ViewModel знищується (екран закрито остаточно)
    override fun onCleared() {
        classifier.close()
    }
}