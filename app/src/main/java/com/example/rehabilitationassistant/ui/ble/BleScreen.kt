package com.example.rehabilitationassistant.ui.ble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rehabilitationassistant.ble.BleConnectionState
import com.example.rehabilitationassistant.ble.BleDevice
import com.example.rehabilitationassistant.ui.theme.*

// Екран керування BLE
@Composable
fun BleScreen(
    state: BleUiState,
    onScan: () -> Unit,
    onConnect: (BleDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSend: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BLE-хаб",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        ConnectionIndicator(state.connectionState, state.connectedDevice?.name)

        Button(
            onClick = onScan,
            enabled = !state.isScanning && state.connectionState != BleConnectionState.CONNECTING,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (state.isScanning) "Сканування..." else "Пошук пристроїв", fontWeight = FontWeight.Medium)
        }

        if (state.isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MovingBlue,
                trackColor = SlateCardBorder
            )
        }

        SectionTitle("ЗНАЙДЕНІ ПРИСТРОЇ")
        if (state.devices.isEmpty()) {
            Text(
                text = if (state.isScanning) "Йде пошук..." else "Пристроїв немає. Натисніть «Пошук пристроїв».",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        state.devices.forEach { device ->
            val isThis = state.connectedDevice == device && state.connectionState != BleConnectionState.DISCONNECTED
            DeviceCard(
                device = device,
                isConnected = isThis,
                busy = state.connectionState == BleConnectionState.CONNECTING,
                onClick = { if (isThis) onDisconnect() else onConnect(device) }
            )
        }

        SectionTitle("КОМАНДИ")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("RELAY:ON", "RELAY:OFF", "GET:STATUS").forEach { cmd ->
                OutlinedButton(
                    onClick = { onSend(cmd) },
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) { Text(cmd, fontSize = 12.sp, maxLines = 1) }
            }
        }

        SectionTitle("ТЕРМІНАЛ")
        Terminal(state.log)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
}

// Індикатор: кольорова крапка + текст Connected / Disconnected
@Composable
private fun ConnectionIndicator(state: BleConnectionState, deviceName: String?) {
    val (color, label) = when (state) {
        BleConnectionState.CONNECTED, BleConnectionState.DATA_TRANSFER -> TargetGreen to "Connected"
        BleConnectionState.CONNECTING -> MovingBlue to "Connecting..."
        BleConnectionState.DISCONNECTED -> IdleGray to "Disconnected"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateSurface)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (deviceName != null) Text(deviceName, color = TextSecondary, fontSize = 13.sp)
        }
        if (state == BleConnectionState.DATA_TRANSFER) {
            Spacer(Modifier.weight(1f))
            Text("передача даних", color = MovingBlue, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DeviceCard(device: BleDevice, isConnected: Boolean, busy: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateSurface)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${device.address}  ·  ${device.rssi} dBm", color = TextSecondary, fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onClick,
            enabled = !busy,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isConnected) WarningRed else MovingBlue)
        ) { Text(if (isConnected) "Відключити" else "Підключити", fontSize = 13.sp) }
    }
}

// Термінал з автопрокруткою до останнього рядка
@Composable
private fun Terminal(lines: List<String>) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF020617))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        if (lines.isEmpty()) {
            Text("> очікування подій...", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        LazyColumn(state = listState) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = when {
                        "✗" in line -> WarningRed
                        "TX" in line -> MovingBlue
                        "RX" in line -> TargetGreen
                        else -> TextSecondary
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}