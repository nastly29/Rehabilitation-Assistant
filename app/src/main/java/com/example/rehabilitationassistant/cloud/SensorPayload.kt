package com.example.rehabilitationassistant.cloud

// Об'єкт, який відправляється в хмару
data class SensorPayload(
    val deviceId: String,                               // ідентифікатор пристрою
    val sensorType: String,                             // назва сенсора
    val x: Float,                                       // прискорення по осі X
    val y: Float,                                       // прискорення по осі Y
    val z: Float,                                       // прискорення по осі Z
    val timestamp: Long = System.currentTimeMillis()    // час вимірювання в мс (фіксується при створенні)
)