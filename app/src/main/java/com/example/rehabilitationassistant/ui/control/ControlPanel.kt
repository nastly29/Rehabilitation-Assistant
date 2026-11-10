package com.example.rehabilitationassistant.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rehabilitationassistant.ui.theme.*

private val LampOnColor = Color(0xFFFFC107) // жовта увімкнена лампа

// Вкладка "Керування": Manual Control + віртуальна лампа
@Composable
fun ControlPanel(
    state: ControlUiState,
    onFlashlight: (Boolean) -> Unit,
    onVibrate: () -> Unit,
    onLamp: (Boolean) -> Unit
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
            text = "Manual Control",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        // Банер автоматичного спрацювання
        if (state.alarmActive) {
            Text(
                text = "ТРИВОГА: перевищено безпечний кут. Вібрація й ліхтарик увімкнені",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarningRed)
                    .padding(12.dp)
            )
        }

        // Віртуальна лампа: колір залежить від стану (команда з хмари або ручний перемикач)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateSurface)
                .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (state.lampOn) LampOnColor else IdleGray)
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text("Віртуальна лампа", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.lampOn) "УВІМКНЕНО" else "вимкнено",
                    color = if (state.lampOn) LampOnColor else TextSecondary,
                    fontSize = 13.sp
                )
                Text(state.remoteInfo, color = TextSecondary, fontSize = 11.sp)
            }
        }

        SwitchRow("Лампа", state.lampOn, onLamp)
        SwitchRow(
            if (state.hasFlash) "Ліхтарик" else "Ліхтарик (віртуальний, апаратного немає)",
            state.flashlightOn,
            onFlashlight
        )

        Button(
            onClick = onVibrate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (state.hasVibrator) "Вібрація" else "Вібрація (мотора немає)")
        }
    }
}

// Рядок "назва + перемикач"
@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = TextPrimary, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}