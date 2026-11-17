package com.example.rehabilitationassistant.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Entity: цей клас Room перетворює на таблицю sensor_records (один об'єкт = один рядок)
// indices - індекс по timestamp, щоб швидше сортувати записи за часом
@Entity(tableName = "sensor_records", indices = [Index("timestamp")])
data class SensorRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,                // час запису в мс
    val sensorValue: Float,             // кут у градусах
    val actionTriggered: Boolean        // чи була активна тривога
)