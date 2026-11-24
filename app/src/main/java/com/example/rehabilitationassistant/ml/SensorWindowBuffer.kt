package com.example.rehabilitationassistant.ml

// Кільцевий буфер: зберігає останні 20 вимірів, новий стирає найстаріший
// Викликається лише з головного потоку
class SensorWindowBuffer(private val size: Int = ActivityClassifier.WINDOW_SIZE) {

    private val data = FloatArray(size * 3) // x, y, z підряд для кожного виміру (всього 60 чисел)
    private var head = 0                    // номер місця, куди писати наступний вимір (0..19)
    private var count = 0                   // скільки вимірів уже накопичено (максимум 20)

    val isFull: Boolean get() = count == size // чи вже є повне вікно (модель можна запускати)

    // Додати вимір
    fun add(x: Float, y: Float, z: Float) {
        val i = head * 3                    // перетворюємо номер виміру на позицію в масиві (кожен вимір займає 3 клітинки)
        data[i] = x
        data[i + 1] = y
        data[i + 2] = z
        head = (head + 1) % size
        if (count < size) count++           // рахуємо, поки буфер не заповнився
    }

    // Копія вікна від найстарішого виміру до найновішого (формат входу моделі)
    fun snapshot(): FloatArray {
        val out = FloatArray(size * 3)
        for (k in 0 until size) {
            val src = ((head + k) % size) * 3
            out[k * 3] = data[src]
            out[k * 3 + 1] = data[src + 1]
            out[k * 3 + 2] = data[src + 2]
        }
        return out
    }
}