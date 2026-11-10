package com.example.rehabilitationassistant.actuator

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

// Вміє вмикати вібромотор і ліхтарик телефона
class SmartActuator(context: Context) {

    companion object {
        const val TAG = "Actuator" // тег, за яким шукаємо наші записи в Logcat
    }

    // applicationContext живе стільки ж, скільки застосунок, тому не "тримає" екран у пам'яті
    private val app = context.applicationContext

    // Вібромотор
    private val vibrator: Vibrator? =
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator

    // Системний сервіс камер (через нього керуємо ліхтариком)
    private val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // Чи є в телефоні спалах і вібромотор
    val hasFlash: Boolean = app.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    val hasVibrator: Boolean = vibrator?.hasVibrator() == true

    // Номер камери, у якої є спалах (для ліхтарика треба знати саме цю камеру)
    // Якщо спалаху немає або сталась помилка, буде null
    private val torchCameraId: String? = if (hasFlash) {
        try {
            // Перебираємо всі камери й беремо першу, у якої є спалах
            cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            null // не вдалось отримати доступ до камер: вважаємо, що спалаху немає
        }
    } else null

    // Тривожний вібросигнал: три короткі імпульси
    fun triggerAlarm() {
        val v = vibrator
        // Вібромотора немає: пишемо попередження й виходимо, застосунок не падає
        if (v == null || !v.hasVibrator()) {
            Log.w(TAG, "Вібромотора немає, сигнал пропущено")
            return
        }
        // Пауза 0, вібро 300, пауза 150, вібро 300, пауза 150, вібро 300
        val pattern = longArrayOf(0, 300, 150, 300, 150, 300)
        v.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 = зробити один раз, без повтору
        Log.d(TAG, "Вібросигнал подано")
    }

    // Вмикає або вимикає ліхтарик
    // Повертає true, якщо вдалось на справжньому ліхтарику, false, якщо ні
    fun toggleFlashlight(status: Boolean): Boolean {
        val id = torchCameraId
        // Спалаху немає: пишемо попередження (на екрані працюватиме віртуальний ліхтарик)
        if (id == null) {
            Log.w(TAG, "Апаратного ліхтарика немає, працює лише віртуальний")
            return false
        }
        return try {
            cameraManager.setTorchMode(id, status) // вмикаємо/вимикаємо ліхтарик
            Log.d(TAG, "Ліхтарик: ${if (status) "ON" else "OFF"}")
            true
        } catch (e: CameraAccessException) {
            // Камера зайнята або недоступна
            Log.e(TAG, "Помилка доступу до камери: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            // Номер камери виявився неправильним
            Log.e(TAG, "Невірний id камери: ${e.message}")
            false
        }
    }
}