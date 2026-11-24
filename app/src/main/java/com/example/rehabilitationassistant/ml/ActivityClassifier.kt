package com.example.rehabilitationassistant.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Результат класифікації: назва класу, впевненість і ймовірності всіх класів
data class Prediction(
    val label: String,                  // який стан вгадала модель
    val confidence: Float,              // наскільки впевнена (0..1)
    val probabilities: List<Float>      // ймовірності всіх 4 класів, для смужок на екрані
)

// ML-Wrapper: завантажує модель у пам'ять і робить прогноз. Решта програми викликає лише classify()
class ActivityClassifier(context: Context) : AutoCloseable {

    companion object {
        const val MODEL_FILE = "activity_model.tflite"   // файл моделі в assets
        const val WINDOW_SIZE = 20                       // скільки вимірів у вікні (як у Colab)
        const val INPUT_SIZE = WINDOW_SIZE * 3           // 60 чисел: x, y, z для кожного виміру

        const val REST = "Спокій"
        const val SLOW_MOVE = "Повільний рух"
        const val FAST_MOVE = "Швидкий рух"
        const val ANOMALY = "Аномалія"
        // порядок такий самий, як при навчанні: модель віддає номер 0..3, а за номером беремо назву
        val LABELS = listOf(REST, SLOW_MOVE, FAST_MOVE, ANOMALY)
    }

    // Interpreter - запускає модель
    private val interpreter = Interpreter(loadModelFile(context))

    // Читаємо файл моделі з assets у буфер пам'яті
    private fun loadModelFile(context: Context): ByteBuffer {
        val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
    }

    // Приймає вікно з 60 чисел, повертає прогноз
    fun classify(sensorWindow: FloatArray): Prediction {
        // перевірка: якщо чисел не 60, модель не зможе працювати
        require(sensorWindow.size == INPUT_SIZE) { "Потрібно $INPUT_SIZE чисел" }
        val input = arrayOf(sensorWindow)                    // вхід
        val output = Array(1) { FloatArray(LABELS.size) }    // сюди модель запише ймовірності
        interpreter.run(input, output)                       // запуск моделі
        val probs = output[0]
        val best = probs.indices.maxByOrNull { probs[it] } ?: 0  // номер класу з найбільшою ймовірністю
        return Prediction(LABELS[best], probs[best], probs.toList())
    }

    // Звільняємо пам'ять, коли модель більше не потрібна
    override fun close() = interpreter.close()
}