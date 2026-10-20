package com.example.rehabilitationassistant.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockBleScanner { // імітація пошуку BLE-пристроїв

    companion object {
        const val FIRST_DEVICE_DELAY_MS = 2000L // через скільки мілісекунд знайдеться перший пристрій
        const val SCAN_TAIL_MS = 1500L // пауза між наступними кроками
    }

    private val virtualDevice = BleDevice("Rehab Hub (Virtual)", "AA:BB:CC:DD:EE:01", -48)
    private val decoyDevice = BleDevice("BLE Sensor (Mock)", "AA:BB:CC:DD:EE:02", -73)

    fun scan(): Flow<BleDevice> = flow {
        delay(FIRST_DEVICE_DELAY_MS)
        emit(virtualDevice)
        delay(SCAN_TAIL_MS)
        emit(decoyDevice)
        delay(SCAN_TAIL_MS)
    }
}