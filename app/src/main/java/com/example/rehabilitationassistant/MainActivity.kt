package com.example.rehabilitationassistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.rehabilitationassistant.sensor.AccelerometerDataSource
import com.example.rehabilitationassistant.ui.ble.BleScreen
import com.example.rehabilitationassistant.ui.ble.BleViewModel
import com.example.rehabilitationassistant.ui.cloud.CloudSyncIndicator
import com.example.rehabilitationassistant.ui.cloud.CloudViewModel
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationScreen
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationStatus
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationViewModel
import com.example.rehabilitationassistant.ui.theme.*

// Головна точка входу в застосунок
class MainActivity : ComponentActivity() {

    // Три ViewModel: by viewModels() створює їх і зберігає при повороті екрана
    private val viewModel: RehabilitationViewModel by viewModels()   // вправа
    private val bleViewModel: BleViewModel by viewModels()           // Bluetooth
    private val cloudViewModel: CloudViewModel by viewModels()   // Змінна для хмарної ViewModel: керує відправкою даних датчика в хмару
    private lateinit var dataSource: AccelerometerDataSource         // джерело даних акселерометра

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Кожне значення датчика йде і в логіку вправи, і в хмарний сервіс
        dataSource = AccelerometerDataSource(this) {
            viewModel.onSensorData(it)        // для вправи (кут, повторення)
            cloudViewModel.onSensorData(it)   // для хмари (запам'ятати останнє)
        }

        setContent {
            RehabilitationAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.uiState.collectAsState()
                    val bleState by bleViewModel.uiState.collectAsState()

                    // Стан хмарної синхронізації зі ViewModel; при кожній зміні екран оновлюється сам
                    val cloudState by cloudViewModel.uiState.collectAsState()

                    var tab by remember { mutableIntStateOf(0) }
                    val context = LocalContext.current

                    // Слухаємо повідомлення від CloudViewModel і показуємо Toast, коли немає інтернету
                    LaunchedEffect(Unit) {
                        cloudViewModel.messages.collect {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Передача даних вправи на BLE-хаб: лічильники повторень
                    LaunchedEffect(state.reps, state.failedReps) {
                        if (state.reps + state.failedReps > 0) {
                            bleViewModel.sendTelemetry("REPS:${state.reps};FAILED:${state.failedReps}")
                        }
                    }
                    // Тривожне повідомлення при перевищенні безпечного кута
                    LaunchedEffect(state.status) {
                        if (state.status == RehabilitationStatus.OVERSTRETCH) {
                            bleViewModel.sendTelemetry("ALERT:OVERSTRETCH")
                        }
                    }

                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        TabRow(
                            selectedTabIndex = tab,
                            containerColor = SlateBackground,
                            contentColor = MovingBlue
                        ) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Вправа") })
                            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("BLE-хаб") })
                        }
                        CloudSyncIndicator(cloudState)   // видно на обох вкладках
                        if (tab == 0) {
                            RehabilitationScreen(
                                state = state,
                                sensorAvailable = dataSource.isAvailable,
                                onReset = viewModel::reset
                            )
                        } else {
                            BleScreen(
                                state = bleState,
                                onScan = bleViewModel::startScan,
                                onConnect = bleViewModel::connect,
                                onDisconnect = bleViewModel::disconnect,
                                onSend = bleViewModel::sendData
                            )
                        }
                    }
                }
            }
        }
    }

    // Запуск зчитування акселерометра при активізації екрана
    override fun onResume() {
        super.onResume()
        dataSource.start()
    }

    // Зупинка зчитування для збереження батареї
    override fun onPause() {
        super.onPause()
        dataSource.stop()
    }
}