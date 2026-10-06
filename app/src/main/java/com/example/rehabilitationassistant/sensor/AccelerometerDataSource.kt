package com.example.rehabilitationassistant.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.rehabilitationassistant.core.model.SensorData

// Клас для роботи з системним акселерометром через зворотний виклик onData
class AccelerometerDataSource(
    context: Context,
    private val onData: (SensorData) -> Unit
) : SensorEventListener {

    // Отримання системного сервісу SensorManager та акселерометра пристрою
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = accelerometer != null

    // Реєстрація слухача подій сенсора (викликається в onResume)
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // Відписка від подій сенсора для збереження батереї (викликається в onPause)
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // Обробка нових показників акселерометра та передача їх через onData
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            onData(SensorData(event.values[0], event.values[1], event.values[2]))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}