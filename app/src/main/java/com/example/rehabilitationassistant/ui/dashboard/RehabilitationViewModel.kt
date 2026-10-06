package com.example.rehabilitationassistant.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.rehabilitationassistant.core.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

// ViewModel зберігає логіку застосунку окремо від екрана
class RehabilitationViewModel : ViewModel() {

    // Константи, спільні для всього класу (до них можна звертатись із екрана)
    companion object Thresholds {
        const val START_ANGLE = 10f      // з цього кута вважаємо, що рух почався
        const val TARGET_ANGLE = 60f     // цільовий кут: досягли - повторення можна зарахувати
        const val MAX_SAFE_ANGLE = 90f   // більше цього кута - небезпечно
        const val ALPHA = 0.15f          // коефіцієнт згладжування
    }


    private val _uiState = MutableStateFlow(RehabilitationUiState())
    val uiState: StateFlow<RehabilitationUiState> = _uiState.asStateFlow()

    // filtered - згладжені значення X, Y, Z
    private var filtered = SensorData()
    // Прапорці: чи досягли ми цільового кута і чи був перевищений безпечний у поточному повторенні
    private var targetReached = false
    private var overstretched = false

    // Викликається щоразу, коли приходять нові дані від акселерометра
    fun onSensorData(raw: SensorData) {
        // Фільтр низьких частот: нове значення = старе + ALPHA * (різниця між сирим і старим)
        filtered = SensorData(
            x = filtered.x + ALPHA * (raw.x - filtered.x),
            y = filtered.y + ALPHA * (raw.y - filtered.y),
            z = filtered.z + ALPHA * (raw.z - filtered.z)
        )
        val angle = filtered.angleDegrees
        val absAngle = abs(angle)

        // update безпечно змінює стан: беремо поточний стан s і повертаємо новий
        _uiState.update { s ->
            // copy змінює тільки вказані поля, решта лишається як була
            val withData = s.copy(
                sample = raw,                           // показуємо сирі X, Y, Z
                angle = angle,                          // поточний кут
                maxAngle = maxOf(s.maxAngle, absAngle)  // запам'ятовуємо найбільший кут
            )
            // Перевіряємо кут на порогові значення і оновлюємо статус
            applyThresholds(withData, absAngle)
        }
    }

    // перевірка порогових кутів і підрахунок вдарих/невдалих спроб
    private fun applyThresholds(s: RehabilitationUiState, a: Float): RehabilitationUiState =
        when {
            // Кут більше 90° - небезпечно, запам'ятовуємо це для поточного повторення
            a > MAX_SAFE_ANGLE -> {
                overstretched = true
                s.copy(status = RehabilitationStatus.OVERSTRETCH)
            }
            // Кут від 60° до 90° - ціль досягнута
            a >= TARGET_ANGLE -> {
                targetReached = true
                s.copy(status = RehabilitationStatus.TARGET_REACHED)
            }
            a >= START_ANGLE -> {
                if (s.status == RehabilitationStatus.IDLE ||
                    s.status == RehabilitationStatus.OVERSTRETCH
                ) s.copy(status = RehabilitationStatus.MOVING) else s
            }
            else -> { // Повернення у вихідне положення (фіксація результату)
                if (s.status != RehabilitationStatus.IDLE) {  // Якщо до цього був рух (статус не IDLE), підсумовуємо повторення
                    val ok = targetReached && !overstretched
                    targetReached = false
                    overstretched = false
                    s.copy(
                        status = RehabilitationStatus.IDLE,
                        reps = s.reps + if (ok) 1 else 0,
                        failedReps = s.failedReps + if (ok) 0 else 1
                    )
                } else s
            }
        }

    // Скидання лічильників і повторний запуск сесії
    fun reset() {
        targetReached = false
        overstretched = false
        _uiState.value = RehabilitationUiState()
    }
}