package com.example.rehabilitationassistant

import android.os.Bundle
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
import com.example.rehabilitationassistant.sensor.AccelerometerDataSource
import com.example.rehabilitationassistant.ui.ble.BleScreen
import com.example.rehabilitationassistant.ui.ble.BleViewModel
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationScreen
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationStatus
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationViewModel
import com.example.rehabilitationassistant.ui.theme.*

// Головна точка входу в застосунок
class MainActivity : ComponentActivity() {

    private val viewModel: RehabilitationViewModel by viewModels()
    private val bleViewModel: BleViewModel by viewModels()
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
                    val state by viewModel.uiState.collectAsState()
                    val bleState by bleViewModel.uiState.collectAsState()
                    var tab by remember { mutableIntStateOf(0) }

                    // Передача даних вправи на хаб: лічильники повторень
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

    override fun onResume() {
        super.onResume()
        dataSource.start()
    }

    override fun onPause() {
        super.onPause()
        dataSource.stop()
    }
}