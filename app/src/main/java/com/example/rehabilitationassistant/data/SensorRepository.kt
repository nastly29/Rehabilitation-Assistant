package com.example.rehabilitationassistant.data

import kotlinx.coroutines.flow.Flow

// Репозиторій: єдине місце, де застосунок спілкується з БД
// ViewModel не звертається до DAO напряму, а працює тільки через нього
class SensorRepository(private val dao: SensorDao) { // dao - те, через що ми працюємо з таблицею

    // Віддає останні N записів (Flow сам надсилає новий список, коли таблиця змінилась)
    fun observeLast(n: Int): Flow<List<SensorRecord>> = dao.observeLast(n)

    // Зберегти один запис у БД (suspend - викликається з корутини)
    suspend fun save(record: SensorRecord) = dao.insert(record)

    // Видалити всю історію з БД
    suspend fun clear() = dao.clear()
}