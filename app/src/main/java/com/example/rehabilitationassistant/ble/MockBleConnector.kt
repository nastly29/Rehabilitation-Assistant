package com.example.rehabilitationassistant.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MockBleConnector(
    private val logger: (String) -> Unit, // Приймає функцію logger для виводу тексту в термінал/Logcat
    private val packetLossProbability: Double = 0.0 // Приймає ймовірність втрати пакетів
) : IBleConnector { // Реалізує інтерфейс IBleConnector (зобов'язаний реалізувати всі його методи)

    companion object {
        private const val MAX_ATTEMPTS = 3 // максимальна кількість спроб відправки пакета
        private const val ACK_TIMEOUT_MS = 300L // затримка очікування підтвердження ACK у мілісекундах
    }

    private val _state = MutableStateFlow(BleConnectionState.DISCONNECTED) // внутрішній змінний потік стану
    override val connectionState: StateFlow<BleConnectionState> = _state.asStateFlow() // публічний потік (тільки для читання), на який підпишеться графічний інтерфейс для відстеження стану з'єднання

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 16) // внутрішній потік для надсилання вхідних подій/повідомлень
    override val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    private val txMutex = Mutex() // гарантує, що відправки виконуються по черзі

    override suspend fun connect(device: BleDevice) {
        if (_state.value != BleConnectionState.DISCONNECTED) return
        _state.value = BleConnectionState.CONNECTING
        logger("GATT: підключення до ${device.name} (${device.address})")
        delay(1000)
        logger("GATT: знайдено сервіс ${BleProfile.SERVICE_UUID}")
        delay(500)
        logger("GATT: Notify увімкнено на STATUS_CHAR ${BleProfile.STATUS_CHAR_UUID}") // виводимо лог про увімкнення підписки на сповіщення STATUS_CHAR
        _state.value = BleConnectionState.CONNECTED
        logger("GATT: з'єднання встановлено")
    }

    override suspend fun disconnect() {
        if (_state.value == BleConnectionState.DISCONNECTED) return
        delay(200)
        _state.value = BleConnectionState.DISCONNECTED
        logger("GATT: з'єднання закрито")
    }

    // Оголошує функцію відправки тексту data із захистом txMutex.withLock (наступна команда чекатиме завершення цієї)
    override suspend fun sendData(data: String): Boolean = txMutex.withLock {
        val connected = _state.value == BleConnectionState.CONNECTED ||
                _state.value == BleConnectionState.DATA_TRANSFER
        if (!connected) {
            logger("TX ✗ відхилено: пристрій не підключено")
            return@withLock false
        }

        _state.value = BleConnectionState.DATA_TRANSFER
        var delivered = false
        for (attempt in 1..MAX_ATTEMPTS) {
            logger("TX -> COMMAND_CHAR [спроба $attempt/$MAX_ATTEMPTS]: \"$data\" (${data.toByteArray().size} B)")
            delay(ACK_TIMEOUT_MS)
            if (Random.nextDouble() < packetLossProbability) { // Якщо випадає ймовірність помилки, то виводить лог таймауту й іде на наступну спробу
                logger("TX: таймаут, ACK не отримано")
                continue
            }
            delivered = true
            break
        }

        if (delivered) _incoming.emit("ACK:$data") else logger("TX ✗ пакет втрачено після $MAX_ATTEMPTS спроб")
        if (_state.value == BleConnectionState.DATA_TRANSFER) _state.value = BleConnectionState.CONNECTED
        delivered
    }
}