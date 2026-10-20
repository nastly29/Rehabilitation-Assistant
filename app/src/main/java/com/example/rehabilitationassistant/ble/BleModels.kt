package com.example.rehabilitationassistant.ble

data class BleDevice(  // зберігає інформацію про виявлений Bluetooth-пристрій
    val name: String,
    val address: String,  //MAC-адреса пристрою
    val rssi: Int   //рівень сигналу в децибелах
)

enum class BleConnectionState {  // всі можливі стани, у яких може перебувати модуль зв'язку
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DATA_TRANSFER
}