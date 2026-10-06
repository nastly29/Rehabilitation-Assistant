package com.example.rehabilitationassistant.core.model

import kotlin.math.atan2
import kotlin.math.sqrt

// Модель для збереження відліку акселерометра (м/с² по осях X, Y, Z)
data class SensorData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    // Обчислення кута відхилення від вертикалі у градусах (0°..180°)
    val angleDegrees: Float
        get() = Math.toDegrees(
            atan2(sqrt(x * x + z * z).toDouble(), y.toDouble())
        ).toFloat()
}