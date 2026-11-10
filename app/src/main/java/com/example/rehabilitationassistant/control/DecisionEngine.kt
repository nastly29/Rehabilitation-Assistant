package com.example.rehabilitationassistant.control

// Блок прийняття рішення: IF значення > поріг THEN активувати актуатор
class DecisionEngine(
    private val threshold: Float,          // критичний кут
    private val hysteresis: Float = 5f     // вимикаємо не на 90°, а на 85°, щоб не мигало біля порога
) {
    // Три можливі відповіді блоку
    enum class Decision {
        NONE,        // нічого не змінювати
        ACTIVATE,    // увімкнути сигналізацію (вібрація + ліхтарик)
        DEACTIVATE   // вимкнути сигналізацію
    }

    private var active = false // чи сигналізація зараз увімкнена

    // Отримує поточний кут і повертає рішення
    // Рішення видається лише при ПЕРЕХОДІ через поріг, а не на кожне значення
    fun evaluate(value: Float): Decision = when {
        // Не гуде і кут більший за 90°: вмикаємо
        !active && value > threshold -> { active = true; Decision.ACTIVATE }
        // Гуде і кут став менший за 85° (90 - 5): вимикаємо
        active && value < threshold - hysteresis -> { active = false; Decision.DEACTIVATE }
        // Усе інше (кут між 85° і 90°, або вже гуде, або ще тихо): нічого не змінюємо
        else -> Decision.NONE
    }
}