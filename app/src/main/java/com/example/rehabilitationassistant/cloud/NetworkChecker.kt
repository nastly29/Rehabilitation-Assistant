package com.example.rehabilitationassistant.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Перевірка, чи є доступ до інтернету
class NetworkChecker(context: Context) {

    private val cm = context.applicationContext  // Системний сервіс Android, який знає про стан мережі
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isOnline(): Boolean {
        // Поточна мережа (Wi-Fi чи мобільна); null = нічого не підключено, наприклад Airplane mode
        val network = cm.activeNetwork ?: return false
        // Що ця мережа вміє; null = невідомо, вважаємо що інтернету немає
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // Мережа дає вихід в інтернет та Android перевірив, що він справді працює
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}