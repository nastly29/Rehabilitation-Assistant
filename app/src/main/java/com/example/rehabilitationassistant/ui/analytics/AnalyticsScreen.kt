package com.example.rehabilitationassistant.ui.analytics

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rehabilitationassistant.data.SensorRecord
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ALARM_ANGLE = 90f // поріг тривоги

// Екран "Аналітика": заголовок, графік і кнопка очищення
// records - список записів з БД, onClear - що робити при натисканні "Очистити історію"
@Composable
fun AnalyticsScreen(
    records: List<SensorRecord>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    // Column розставляє елементи один під одним
    Column(
        modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Аналітика", style = MaterialTheme.typography.titleLarge)
        Text("Точок на графіку: ${records.size} (максимум 50)") // скільки точок зараз на графіку

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            factory = { ctx -> LineChart(ctx).apply { setupChart() } }, // створюємо графік один раз
            update = { chart -> chart.render(records, labelColor) }     // перемальовуємо при кожній зміні records
        )

        Button(onClick = onClear) { Text("Очистити історію") } // видаляє всі записи з БД
    }
}

// Постійні налаштування графіка (виконуються один раз)
private fun LineChart.setupChart() {
    description.isEnabled = false                    // прибираємо стандартний напис у кутку
    axisRight.isEnabled = false                      // права вісь не потрібна
    xAxis.position = XAxis.XAxisPosition.BOTTOM      // підписи часу знизу
    xAxis.setDrawGridLines(false)                    // без вертикальної сітки
    xAxis.granularity = 1f                           // крок по осі X - одна точка
    axisLeft.axisMinimum = 0f                        // кут від 0°
    axisLeft.axisMaximum = 190f                      // до 190° (з запасом над 180°)
    setNoDataText("Даних ще немає")                  // напис, поки в БД порожньо
}

// Малює актуальний список точок (викликається при кожній зміні records)
private fun LineChart.render(records: List<SensorRecord>, labelColor: Int) {
    xAxis.textColor = labelColor
    axisLeft.textColor = labelColor
    legend.textColor = labelColor
    setNoDataTextColor(labelColor)

    // Записів немає: очищаємо графік і виходимо
    if (records.isEmpty()) { clear(); return }

    // Підпис осі X: замість номера точки показуємо час її запису
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String =
            records.getOrNull(value.toInt())?.let { timeFormat.format(Date(it.timestamp)) } ?: ""
    }

    // Червона лінія порогу 90°
    axisLeft.removeAllLimitLines()
    axisLeft.addLimitLine(LimitLine(ALARM_ANGLE, "Поріг 90°").apply {
        lineColor = AndroidColor.RED
        lineWidth = 1.5f
        textColor = labelColor
    })

    // Синя лінія кута: X - номер точки, Y - значення кута
    val line = LineDataSet(
        records.mapIndexed { i, r -> Entry(i.toFloat(), r.sensorValue) },
        "Кут, °" // підпис у легенді
    ).apply {
        color = AndroidColor.rgb(33, 150, 243)
        lineWidth = 2f
        setDrawCircles(false)  // без крапок на лінії
        setDrawValues(false)   // без чисел над точками
    }

    // Червоні крапки там, де була активна тривога (лінії між ними не видно)
    val alarms = LineDataSet(
        records.mapIndexedNotNull { i, r ->
            // беремо лише записи, де actionTriggered = true
            if (r.actionTriggered) Entry(i.toFloat(), r.sensorValue) else null
        },
        "Спрацювання актуаторів"
    ).apply {
        color = AndroidColor.TRANSPARENT
        setCircleColor(AndroidColor.RED)
        circleRadius = 4f
        setDrawCircleHole(false)
        setDrawValues(false)
    }

    data = LineData(line, alarms) // передаємо графіку обидві лінії
    invalidate()                  // просимо графік перемалюватись
}