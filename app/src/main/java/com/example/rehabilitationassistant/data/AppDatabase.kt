package com.example.rehabilitationassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Клас бази даних: тут вказуємо, які таблиці є в БД
// entities - список таблиць, version - версія схеми, exportSchema = false - не зберігати схему у файл
@Database(entities = [SensorRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Через цей метод отримуємо DAO для роботи з таблицею sensor_records (Room сам його реалізує)
    abstract fun sensorDao(): SensorDao

    companion object {
        // Єдиний екземпляр бази; @Volatile - щоб усі потоки бачили актуальне значення
        @Volatile private var INSTANCE: AppDatabase? = null

        // Синглтон: на весь застосунок одна база
        fun getInstance(context: Context): AppDatabase =
            // Якщо база вже створена - беремо її, інакше створюємо
            INSTANCE ?: synchronized(this) { // synchronized - щоб два потоки не створили дві бази одночасно
                // Перевіряємо ще раз, бо інший потік міг створити базу, поки ми чекали
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // контекст усього застосунку, а не одного екрана
                    AppDatabase::class.java,    // який клас бази створюємо
                    "rehab.db"                  // назва файлу бази на телефоні
                ).build().also { INSTANCE = it } // створюємо базу й запам'ятовуємо її в INSTANCE
            }
    }
}