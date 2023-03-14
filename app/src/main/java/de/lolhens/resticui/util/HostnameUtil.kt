package de.lolhens.resticui.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object HostnameUtil {
    private const val DEFAULT_HOSTNAME = "android-device"

    fun detectHostname(context: Context): String {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            // Some Devices do not have a BluetoothAdapter e.g. the Android Emulator
            if (bluetoothAdapter != null) {
                return bluetoothAdapter.name
            }
        }

        return DEFAULT_HOSTNAME
    }
}