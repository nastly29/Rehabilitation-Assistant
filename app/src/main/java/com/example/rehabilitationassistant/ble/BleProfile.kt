package com.example.rehabilitationassistant.ble

import java.util.UUID

object BleProfile {
    val SERVICE_UUID: UUID = UUID.fromString("0000A100-0000-1000-8000-00805F9B34FB")
    val STATUS_CHAR_UUID: UUID = UUID.fromString("0000A101-0000-1000-8000-00805F9B34FB")
    val COMMAND_CHAR_UUID: UUID = UUID.fromString("0000A102-0000-1000-8000-00805F9B34FB")
}