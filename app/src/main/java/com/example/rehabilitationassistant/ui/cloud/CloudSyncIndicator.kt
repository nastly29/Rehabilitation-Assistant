package com.example.rehabilitationassistant.ui.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rehabilitationassistant.ui.theme.IdleGray
import com.example.rehabilitationassistant.ui.theme.MovingBlue
import com.example.rehabilitationassistant.ui.theme.SlateCardBorder
import com.example.rehabilitationassistant.ui.theme.SlateSurface
import com.example.rehabilitationassistant.ui.theme.TargetGreen
import com.example.rehabilitationassistant.ui.theme.TextPrimary
import com.example.rehabilitationassistant.ui.theme.TextSecondary
import com.example.rehabilitationassistant.ui.theme.WarningRed

// Індикатор "Хмарна синхронізація": зелений = останній запит успішний, червоний = помилка або Pending
// @Composable = функція, яка малює частину екрана; state = готові дані від ViewModel, самі нічого не вирішуємо
@Composable
fun CloudSyncIndicator(state: CloudUiState) {
    val color = when (state.status) {
        SyncStatus.SUCCESS -> TargetGreen
        SyncStatus.ERROR, SyncStatus.PENDING -> WarningRed
        SyncStatus.SENDING -> MovingBlue
        SyncStatus.IDLE -> IdleGray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Хмарна синхронізація",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(state.message, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        Text(
            "Надіслано: ${state.sentCount}  Pending: ${state.pendingCount}",
            color = color,
            fontSize = 11.sp
        )
    }
}