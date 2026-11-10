package com.example.rehabilitationassistant.ui.control

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.rehabilitationassistant.actuator.SmartActuator
import com.example.rehabilitationassistant.control.DecisionEngine
import com.example.rehabilitationassistant.ui.dashboard.RehabilitationViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Усе, що потрібно намалювати на вкладці "Керування" (екран лише читає це)
data class ControlUiState(
    val alarmActive: Boolean = false,      // чи спрацювала автоматична аварія (від неї залежить червоний банер)
    val flashlightOn: Boolean = false,     // чи увімкнений ліхтарик (перемикач на екрані)
    val lampOn: Boolean = false,           // чи горить віртуальна лампа (команда з хмари або ручний перемикач)
    val hasFlash: Boolean = false,         // чи є справжній спалах у телефоні
    val hasVibrator: Boolean = false,      // чи є вібромотор
    val remoteInfo: String = "Очікування команди з хмари"   // текст під лампою
)

// З'єднує частини: рішення, актуатори, хмару й екран
class ControlViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val TAG = "Control"                      // тег для Logcat
        private const val PATH = "control/led_status" // адреса команди в базі
    }

    private val actuator = SmartActuator(app)   // вібрація й ліхтарик
    private val engine = DecisionEngine(threshold = RehabilitationViewModel.MAX_SAFE_ANGLE) // вирішує, коли вмикати

    // Стан екрана
    private val _uiState = MutableStateFlow(
        ControlUiState(hasFlash = actuator.hasFlash, hasVibrator = actuator.hasVibrator)
    )
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    // ДИСТАНЦІЙНЕ керування: стежимо за вузлом control/led_status
    private val ref = FirebaseDatabase.getInstance().getReference(PATH) // адреса вузла в базі

    // Слухач: Firebase сам викликає його методи, коли щось відбувається
    private val listener = object : ValueEventListener {

        // Спрацьовує при підключенні й при КОЖНІЙ зміні значення в базі (подія, а не опитування)
        override fun onDataChange(snapshot: DataSnapshot) {
            val on = parse(snapshot.value)   // "ON" перетворюємо на true, "OFF" на false
            Log.d(TAG, "Команда з хмари: ${snapshot.value} -> лампа=$on")
            // Оновлюємо лише лампу й текст, решта стану лишається
            _uiState.update { it.copy(lampOn = on, remoteInfo = "led_status = ${snapshot.value}") }
        }

        // Спрацьовує, якщо немає доступу (наприклад, правила забороняють читати)
        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Слухач скасовано: ${error.message}")
            _uiState.update { it.copy(remoteInfo = "Помилка: ${error.message}") }
        }
    }

    // Виконується один раз, коли створюється ViewModel
    init {
        ref.addValueEventListener(listener) // підписуємось на Firebase, щоб повідомляв про всі зміни
    }

    // Перетворює значення з бази на "так/ні". Розуміє ON/OFF, true/false, 1/0, а все інше вважає "ні"
    private fun parse(v: Any?): Boolean = when (v) {
        is Boolean -> v
        is String -> v.equals("ON", true) || v.equals("true", true) || v == "1"  // true = ігнорувати регістр
        is Number -> v.toInt() != 0
        else -> false
    }

    // АВТОМАТИЧНЕ керування: кут -> DecisionEngine -> актуатори
    // Викликається з MainActivity щоразу, коли змінився кут
    fun onAngle(angle: Float) {
        // Питаємо, що робити
        when (engine.evaluate(angle)) {
            // Кут став більшим за 90°: вмикаємо сигналізацію
            DecisionEngine.Decision.ACTIVATE -> {
                Log.w(TAG, "ТРИВОГА: кут %.0f° > порогу %.0f°, вмикаємо вібрацію і ліхтарик"
                    .format(angle, RehabilitationViewModel.MAX_SAFE_ANGLE))
                actuator.triggerAlarm()           // вібрація
                actuator.toggleFlashlight(true)   // ліхтарик
                // Показуємо на екрані банер "ТРИВОГА" і вмикаємо перемикач ліхтарика
                _uiState.update { it.copy(alarmActive = true, flashlightOn = true) }
            }
            // Кут повернувся нижче 85°: вимикаємо
            DecisionEngine.Decision.DEACTIVATE -> {
                Log.d(TAG, "Кут %.0f° повернувся в норму, вимикаємо ліхтарик".format(angle))
                actuator.toggleFlashlight(false)
                _uiState.update { it.copy(alarmActive = false, flashlightOn = false) }
            }
            // Нічого не змінилось: нічого не робимо
            DecisionEngine.Decision.NONE -> Unit
        }
    }

    // РУЧНЕ керування (кнопки панелі Manual Control)

    // Перемикач "Ліхтарик": вмикаємо справжній ліхтарик і запам'ятовуємо стан для екрана
    fun setFlashlight(on: Boolean) {
        actuator.toggleFlashlight(on)
        _uiState.update { it.copy(flashlightOn = on) }
    }

    // Кнопка "Вібрація": просто подати вібросигнал
    fun vibrate() = actuator.triggerAlarm()

    // Перемикач "Лампа": змінюємо лише віртуальну лампу (команда з хмари теж міняє цей стан)
    fun setLamp(on: Boolean) = _uiState.update { it.copy(lampOn = on) }

    // Викликається, коли ViewModel знищується
    override fun onCleared() {
        ref.removeEventListener(listener)
    }
}