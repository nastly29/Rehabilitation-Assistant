package com.example.rehabilitationassistant.ui.analytics

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rehabilitationassistant.data.AppDatabase
import com.example.rehabilitationassistant.data.SensorRecord
import com.example.rehabilitationassistant.data.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

// Посередник між датчиком і базою даних: вирішує, що зберігати, і віддає дані для графіка
class SensorViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val TAG = "Analytics"                // тег для Logcat
        private const val MAX_POINTS = 50          // скільки точок на графіку
        private const val MIN_DELTA = 1f           // мінімальна зміна кута для запису (°)
        private const val MIN_INTERVAL_MS = 500L   // не частіше ніж раз на 0,5 с

        private const val FAST_DELTA = 0.3f          // у швидкому режимі пишемо вже при зміні від 0,3 градусів
        private const val FAST_INTERVAL_MS = 100L    // і раз на 0,1 с
    }

    // Репозиторій, через який працюємо з БД (БД беремо з синглтона)
    private val repository = SensorRepository(AppDatabase.getInstance(app).sensorDao())

    // Останні 50 записів від старих до нових; оновлюється сам, коли в БД з'являється рядок
    val records: StateFlow<List<SensorRecord>> = repository.observeLast(MAX_POINTS)
        .map { it.reversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastSavedAt = 0L              // коли востаннє зберігали
    private var lastSavedValue = Float.NaN    // який кут зберегли востаннє
    private var lastAction = false            // яким був стан тривоги в останньому записі

    private var fastMode = false  // швидкий режим запису (коли активність "Швидкий рух")

    // Вмикає або вимикає швидкий режим (викликається з MainActivity за прогнозом моделі)
    fun setFastMode(enabled: Boolean) {
        fastMode = enabled
        Log.d(TAG, "Режим запису: ${if (enabled) "швидкий" else "звичайний"}")
    }

    // Викликається з MainActivity при кожному новому куті
    fun onSample(angle: Float, actionTriggered: Boolean) {
        val now = System.currentTimeMillis() // поточний час у мс
        // У швидкому режимі менший поріг зміни кута й менший інтервал між записами
        val minDelta = if (fastMode) FAST_DELTA else MIN_DELTA
        val minInterval = if (fastMode) FAST_INTERVAL_MS else MIN_INTERVAL_MS

        // Суттєва зміна: це перший запис, або кут змінився достатньо, або змінився стан тривоги
        val significant = lastSavedValue.isNaN() ||
                abs(angle - lastSavedValue) >= minDelta ||
                actionTriggered != lastAction
        // Не суттєво або ще рано: нічого не зберігаємо
        if (!significant || now - lastSavedAt < minInterval) return

        // Запам'ятовуємо, що саме зараз зберегли, для наступного порівняння
        lastSavedAt = now
        lastSavedValue = angle
        lastAction = actionTriggered

        viewModelScope.launch(Dispatchers.IO) {
            repository.save(
                SensorRecord(timestamp = now, sensorValue = angle, actionTriggered = actionTriggered)
            )
            Log.d(TAG, "Збережено: кут %.0f°, актуатор=%b".format(angle, actionTriggered))
        }
    }

    // Кнопка "Очистити історію": видаляє всі записи
    fun clear() {
        viewModelScope.launch(Dispatchers.IO) { repository.clear() }
    }
}