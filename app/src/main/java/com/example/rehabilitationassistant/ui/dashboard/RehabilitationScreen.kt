package com.example.rehabilitationassistant.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rehabilitationassistant.ui.theme.*
import kotlin.math.abs

private const val ARC_MAX_ANGLE = 120f // Максимальний кут, який відображається на дузі (далі дуга вже заповнена повністю)

// Головний екран застосунку
// state - всі дані для показу (кут, статус, лічильники), sensorAvailable (чи є сенсор)
// onReset - функція, яка викликається при натисканні на кнопку скидання
@Composable
fun RehabilitationScreen(
    state: RehabilitationUiState,
    sensorAvailable: Boolean,
    onReset: () -> Unit
) {
    val absAngle = abs(state.angle).coerceIn(0f, ARC_MAX_ANGLE)
    val animatedColor by animateColorAsState( // Плавна зміна кольору за 300 мс при переході між станами
        targetValue = state.statusColor,
        animationSpec = tween(durationMillis = 300),
        label = "ColorAnimation"
    )

    Column( // Основна колонка: усі елементи йдуть зверху вниз
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Заголовок
        Text(
            text = "Rehabilitation Assistant",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        if (!sensorAvailable) {
            Text("Сенсор недоступний", color = WarningRed, fontSize = 14.sp)
        }

        Row( // Три значки з поточними координатами X, Y, Z
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CoordinateBadge(label = "X", value = state.sample.x)
            CoordinateBadge(label = "Y", value = state.sample.y)
            CoordinateBadge(label = "Z", value = state.sample.z)
        }

        Spacer(modifier = Modifier.height(8.dp))

        AngleArc( // Кругова дуга, яка показує кут нахилу (графічний індикатор)
            absAngle = absAngle,
            angleText = "%.0f°".format(state.angle),
            color = animatedColor
        )

        // Статус вправи
        Surface(
            shape = CircleShape,
            color = animatedColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = state.statusText,
                color = animatedColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Картка зі статистикою сеансу
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "СТАТИСТИКА СЕСІЇ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                StatRow(label = "Виконано повторень", value = "${state.reps}", valueColor = TargetGreen)
                StatRow(label = "Незарахованих", value = "${state.failedReps}", valueColor = WarningRed)
                StatRow(label = "Макс. амплітуда", value = "%.0f°".format(state.maxAngle), valueColor = TextPrimary)

                Divider(color = SlateCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Ціль: ${RehabilitationViewModel.TARGET_ANGLE.toInt()}° (Безпечний макс.: ${RehabilitationViewModel.MAX_SAFE_ANGLE.toInt()}°)",
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary, fontSize = 13.sp)
                )
            }
        }

        // Кнопка скидання
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
        ) {
            Text("Скинути сеанс", fontWeight = FontWeight.Medium)
        }
    }
}

// Невеликий значок з назвою осі та її значенням, наприклад "X  0.00"
@Composable
private fun CoordinateBadge(label: String, value: Float) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SlateSurface)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$label  %.2f".format(value),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Один рядок статистики: назва зліва, значення справа
@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 15.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// Півколо, яке заповнюється залежно від кута нахилу
@Composable
private fun AngleArc(absAngle: Float, angleText: String, color: Color) {
    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            // Сіра фон-дуга
            drawArc(
                color = ArcTrackDark,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Активна неонова дуга
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = (absAngle / ARC_MAX_ANGLE) * 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = angleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )
            Text(
                text = "КУТ НАХИЛУ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )
        }
    }
}