package com.example.rehabilitationassistant.ui.ble

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rehabilitationassistant.ble.BleConnectionState
import com.example.rehabilitationassistant.ble.BleDevice
import com.example.rehabilitationassistant.ble.IBleConnector
import com.example.rehabilitationassistant.ble.MockBleConnector
import com.example.rehabilitationassistant.ble.MockBleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Стан BLE-екрана
data class BleUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val connectedDevice: BleDevice? = null,
    val log: List<String> = emptyList()
) {
    val isConnected: Boolean
        get() = connectionState == BleConnectionState.CONNECTED ||
                connectionState == BleConnectionState.DATA_TRANSFER
}

class BleViewModel : ViewModel() {

    companion object {
        const val TAG = "BleMock" // тег для фільтрації в Android Studio Logcat
        private const val MAX_LOG_LINES = 200 // обмеження кількості рядків у терміналі
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // Створення екземплярів сканера та конектора
    private val scanner = MockBleScanner()
    private val connector: IBleConnector = MockBleConnector(logger = ::log, packetLossProbability = 0.0)

    private val _uiState = MutableStateFlow(BleUiState()) // Створення потоку стану
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow() // Графічний інтерфейс підписується на uiState і автоматично перемальовує екран при змінах
    private var scanJob: Job? = null // Змінна для збереження поточного корутинного завдання сканування, щоб мати змогу скасувати його

    init {
        // Стан з'єднання з конектора -> стан UI
        viewModelScope.launch {
            connector.connectionState.collect { s ->
                _uiState.update {
                    it.copy(
                        connectionState = s,
                        connectedDevice = if (s == BleConnectionState.DISCONNECTED) null else it.connectedDevice
                    )
                }
            }
        }
        // Повідомлення від пристрою
        viewModelScope.launch {
            connector.incoming.collect { log("RX <- STATUS_CHAR (notify): \"$it\"") }
        }
    }

    // Додає рядок у термінал і в Logcat
    private fun log(message: String) {
        Log.d(TAG, message)
        val line = "${timeFormat.format(Date())}  $message"
        _uiState.update { it.copy(log = (it.log + line).takeLast(MAX_LOG_LINES)) }
    }

    fun startScan() {
        val s = _uiState.value
        if (s.isScanning || s.connectionState == BleConnectionState.CONNECTING) return
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
            log("SCAN: пошук пристроїв...")
            try {
                scanner.scan().collect { device ->
                    _uiState.update { it.copy(devices = it.devices + device) }
                    log("SCAN: знайдено ${device.name} (${device.address}), RSSI ${device.rssi} dBm")
                }
                log("SCAN: завершено")
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun connect(device: BleDevice) {
        if (_uiState.value.connectionState != BleConnectionState.DISCONNECTED) return
        scanJob?.cancel()
        _uiState.update { it.copy(connectedDevice = device) }
        viewModelScope.launch { connector.connect(device) }
    }

    fun disconnect() {
        viewModelScope.launch { connector.disconnect() }
    }

    fun sendData(data: String) {
        viewModelScope.launch { connector.sendData(data) }
    }

    // Для автоматичної відправки даних вправи: нічого не робимо, якщо хаб не підключено
    fun sendTelemetry(data: String) {
        if (_uiState.value.isConnected) sendData(data)
    }
}