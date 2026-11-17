package com.example.rehabilitationassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DAO: тут описуємо, які дії можна робити з таблицею (Room сам напише код)
@Dao
interface SensorDao {
    // Записати один рядок у таблицю
    @Insert
    suspend fun insert(record: SensorRecord)

    // Взяти останні N записів: спочатку найновіші (DESC), LIMIT :n - не більше N штук
    // Flow сам надсилає новий список після кожної зміни таблиці, тому графік оновлюється сам
    @Query("SELECT * FROM sensor_records ORDER BY timestamp DESC LIMIT :n")
    fun observeLast(n: Int): Flow<List<SensorRecord>>

    // Видалити всі рядки з таблиці (кнопка "Очистити історію")
    @Query("DELETE FROM sensor_records")
    suspend fun clear()
}