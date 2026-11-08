package com.example.rehabilitationassistant.ui.cloud

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rehabilitationassistant.cloud.CloudRepository
import com.example.rehabilitationassistant.cloud.NetworkChecker
import com.example.rehabilitationassistant.cloud.SensorPayload
import com.example.rehabilitationassistant.core.model.SensorData
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

// Статуси синхронізації: нічого не було / йде відправка / успіх / помилка / чекає інтернет
enum class SyncStatus { IDLE, SENDING, SUCCESS, ERROR, PENDING }

// Стан для індикатора на екрані
data class CloudUiState(
    val status: SyncStatus = SyncStatus.IDLE,
    val sentCount: Int = 0,          // скільки пакетів відправлено
    val pendingCount: Int = 0,       // скільки чекає в черзі
    val message: String = "Очікування даних"
)

class CloudViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val TAG = "CloudSync"                  // тег для фільтра в Logcat
        private const val SEND_INTERVAL_MS = 5_000L // інтервал відправки
        private const val DELTA = 0.5f              // мінімальна зміна по осі
        private const val MAX_PENDING = 100         // ліміт черги
    }

    private val repository = CloudRepository( // Репозиторій, який пише в Firebase
        FirebaseDatabase.getInstance().also { // повертає об'єкт підключення до нашої бази
            runCatching { it.setLogLevel(Logger.Level.DEBUG) }
        }
    )
    private val network = NetworkChecker(app) // перевірка інтернету
    private val deviceId = "${Build.MANUFACTURER}-${Build.MODEL}" // Build це клас Android, який зберігає інформацію про сам телефон і його систему

    // Стан для екрана
    private val _uiState = MutableStateFlow(CloudUiState())
    val uiState: StateFlow<CloudUiState> = _uiState.asStateFlow()

    // Одноразові повідомлення для Toast
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var latest: SensorData? = null        // останнє значення сенсора
    private var lastQueued: SensorData? = null    // останнє значення, поставлене в чергу
    private val pending = ArrayDeque<SensorPayload>()   // черга на відправку
    private var offlineNotified = false           // щоб не показувати Toast кожні 5 с

    // Виконується при створенні ViewModel
    init {
        // Запускаємо фонову корутину, яка живе, поки живе ViewModel
        viewModelScope.launch {
            while (true) {
                delay(SEND_INTERVAL_MS) // пауза 5 с (екран не зависає)
                tick() // опрацювати чергу
            }
        }
    }

    // Викликається з MainActivity при кожному значенні датчика
    // Датчик дає дані дуже часто, тому лише запам'ятовуємо останнє
    fun onSensorData(raw: SensorData) {
        latest = raw
    }

    // Раз на 5 секунд: поставити в чергу (якщо змінилось хоча б на мільмально дозволену зміну) і спробувати відправити
    private suspend fun tick() {
        val cur = latest // беремо останнє значення
        if (cur != null && changedEnough(cur)) {  // дані є і вони змінились достатньо
            if (pending.size >= MAX_PENDING) pending.removeFirst() // черга повна: викидаємо найстаріший
            pending.addLast( // додаємо новий пакет у кінець черги
                SensorPayload(
                    deviceId = deviceId,
                    sensorType = "accelerometer",
                    x = cur.x, y = cur.y, z = cur.z
                )
            )
            lastQueued = cur  // запам'ятовуємо для наступного порівняння
        }
        flush() // відправляємо чергу (навіть якщо нового пакета нема)
    }

    // Чи змінилось значення хоча б по одній осі на мінімально дозволене або більше
    private fun changedEnough(cur: SensorData): Boolean {
        val prev = lastQueued ?: return true // ще нічого не слали: перший пакет іде завжди
        return abs(cur.x - prev.x) >= DELTA ||
                abs(cur.y - prev.y) >= DELTA ||
                abs(cur.z - prev.z) >= DELTA
    }

    // Відправка черги по порядку
    private suspend fun flush() {
        if (pending.isEmpty()) return

        // Немає інтернету: пакети лишаються в черзі, статус Pending
        if (!network.isOnline()) {
            Log.w(TAG, "Немає інтернету, у черзі: ${pending.size}")
            _uiState.update { // змінюємо лише вказані поля стану
                it.copy(
                    status = SyncStatus.PENDING,
                    pendingCount = pending.size,
                    message = "Немає інтернету. У черзі: ${pending.size}"
                )
            }
            if (!offlineNotified) { // Toast показуємо лише один раз
                offlineNotified = true
                _messages.tryEmit("Немає інтернету. Дані збережено зі статусом Pending")
            }
            return // виходимо, відправимо коли інтернет з'явиться
        }
        offlineNotified = false // інтернет є: наступного разу Toast знову можна показати

        _uiState.update { it.copy(status = SyncStatus.SENDING, message = "Відправка...") }
        while (pending.isNotEmpty()) {
            val payload = pending.first() // беремо перший пакет, але не видаляємо
            val result = repository.send(payload) // відправляємо і чекаємо відповіді
            if (result.isSuccess) {
                pending.removeFirst() // видаляємо з черги лише після успіху
                Log.d(TAG, "Відправлено: $payload")
                _uiState.update {
                    it.copy(
                        status = SyncStatus.SUCCESS,
                        sentCount = it.sentCount + 1,  // +1 до лічильника відправлених
                        pendingCount = pending.size,
                        message = "x=%.2f y=%.2f z=%.2f".format(payload.x, payload.y, payload.z)
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "невідома помилка"
                Log.e(TAG, "Помилка відправки: $err")
                _uiState.update {
                    it.copy(
                        status = SyncStatus.ERROR,
                        pendingCount = pending.size,
                        message = "Помилка: $err"
                    )
                }
                break   // зупиняємось, пакет лишається в черзі, спробуємо через 5 с
            }
        }
    }
}