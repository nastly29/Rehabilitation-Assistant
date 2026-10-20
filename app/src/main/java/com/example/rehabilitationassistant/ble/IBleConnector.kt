package com.example.rehabilitationassistant.ble

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// перелік того, що вміє BLE-з'єднання, без реалізації
// UI залежить лише від нього, тому Mock легко замінити справжнім кодом
interface IBleConnector {
    val connectionState: StateFlow<BleConnectionState> // Потік стану з'єднання, постійно зберігає та транслює поточний стан пристрою
    val incoming: SharedFlow<String> // Потік вхідних повідомлень, використовується для передачі одноразових подій та сповіщень
    suspend fun connect(device: BleDevice) // асинхронна функція для ініціалізації підключення до обраного пристрою BleDevice
    suspend fun disconnect()
    suspend fun sendData(data: String): Boolean // для надсилання текстового пакета даних/команди на пристрій
}