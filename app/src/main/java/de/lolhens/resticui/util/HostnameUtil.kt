package de.lolhens.resticui.util

import android.bluetooth.BluetoothAdapter

object HostnameUtil {
    private const val DEFAULT_HOSTNAME = "android-device"

    fun detectHostname(): String {
        try {
            val blueToothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (blueToothAdapter != null) {
                // Some Devices do not have a BluetoothAdapter e.g. the Android Emulator. For this case we use a default
                // value
                return BluetoothAdapter.getDefaultAdapter().name
            }
        } catch (e: Exception) {
            RuntimeException("Failed to get bluetooth hostname", e).printStackTrace()
        }

        return DEFAULT_HOSTNAME
    }
}