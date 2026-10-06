package com.example.rehabilitationassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.rehabilitationassistant.sensor.AccelerometerDataSource
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationScreen
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationViewModel
import com.example.rehabilitationassistant.ui.theme.RehabilitationAssistantTheme

// Головна точка входу в застосунок
class MainActivity : ComponentActivity() {

    // Ініціалізація ViewModel та джерела даних акселерометра
    private val viewModel: RehabilitationViewModel by viewModels()
    private lateinit var dataSource: AccelerometerDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataSource = AccelerometerDataSource(this) { viewModel.onSensorData(it) }

        setContent {
            RehabilitationAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Підписка на стан інтерфейсу та передача даних у графічний екран
                    val state by viewModel.uiState.collectAsState()
                    RehabilitationScreen(
                        state = state,
                        sensorAvailable = dataSource.isAvailable,
                        onReset = viewModel::reset
                    )
                }
            }
        }
    }

    // Запуск зчитування даних акселерометра при активізації екрана
    override fun onResume() {
        super.onResume()
        dataSource.start()
    }

    // Зупинка зчитування для зберігання батереї при згортанні застосунку
    override fun onPause() {
        super.onPause()
        dataSource.stop()
    }
}