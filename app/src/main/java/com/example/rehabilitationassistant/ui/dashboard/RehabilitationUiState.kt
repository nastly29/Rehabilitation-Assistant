package com.example.rehabilitationassistant.ui.dashboard

import androidx.compose.ui.graphics.Color
import com.example.rehabilitationassistant.core.model.SensorData
import com.example.rehabilitationassistant.ui.theme.IdleGray
import com.example.rehabilitationassistant.ui.theme.MovingBlue
import com.example.rehabilitationassistant.ui.theme.TargetGreen
import com.example.rehabilitationassistant.ui.theme.WarningRed

// Стани вправи
enum class RehabilitationStatus(val label: String, val color: Color) {
    IDLE("Вихідне положення", IdleGray),
    MOVING("Рух до цілі", MovingBlue),
    TARGET_REACHED("Ціль досягнута - повертайтесь", TargetGreen),
    OVERSTRETCH("УВАГА: перевищено безпечний кут!", WarningRed)
}

// Стан екрана: усі дані, які потрібно показати користувачу
// data class автоматично створює copy(), який використовується у ViewModel для оновлення окремих полів
data class RehabilitationUiState(
    val sample: SensorData = SensorData(),   // поточні значення X, Y, Z з акселерометра
    val angle: Float = 0f,                   // кут нахилу після фільтрації, у градусах
    val maxAngle: Float = 0f,                // найбільший кут за весь сеанс
    val status: RehabilitationStatus = RehabilitationStatus.IDLE, // поточний стан вправи
    val reps: Int = 0,                       // кількість зарахованих повторень
    val failedReps: Int = 0                  // кількість незарахованих повторень
) {
    val statusText: String get() = status.label
    val statusColor: Color get() = status.color
}