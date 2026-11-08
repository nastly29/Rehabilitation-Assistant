package com.example.rehabilitationassistant.cloud

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// Репозиторій: єдине місце, де дані йдуть у Firebase Realtime Database
class CloudRepository(private val db: FirebaseDatabase) {

    companion object {
        private const val TIMEOUT_MS = 10_000L // Скільки максимум чекаємо відповіді сервера: 10 секунд (у мілісекундах)
    }

    // Відправляє один пакет у хмару
    suspend fun send(payload: SensorPayload): Result<Unit> = try {
        val device = payload.deviceId.replace(Regex("[.\$#\\[\\]/]"), "_") // Firebase не приймає . $ # [ ] / у ключах, міняємо їх на "_"
        val ref = db.getReference("sensors").child(device).child(payload.timestamp.toString()) // Адреса запису: sensors / пристрій / час (повторна відправка не створює дубль)

        // setValue завершується, коли сервер підтвердив запис; таймаут захищає від "зависання"
        val finished = withTimeoutOrNull(TIMEOUT_MS) {
            ref.setValue(payload.toMap()).await()
            true
        }
        // null = не дочекались (таймаут), інакше успіх
        if (finished == null) Result.failure(IOException("Таймаут ${TIMEOUT_MS / 1000} с"))
        else Result.success(Unit)
    } catch (e: CancellationException) {
        throw e                       // скасування корутини не вважаємо помилкою
    } catch (e: Exception) {
        Result.failure(e)             // будь-яка інша помилка (немає прав, збій мережі)
    }
}

// Явне перетворення в Map, щоб структура запису в базі була передбачуваною
private fun SensorPayload.toMap(): Map<String, Any> = mapOf(
    "deviceId" to deviceId,
    "sensorType" to sensorType,
    "x" to x.toString().toDouble(),
    "y" to y.toString().toDouble(),
    "z" to z.toString().toDouble(),
    "timestamp" to timestamp
)