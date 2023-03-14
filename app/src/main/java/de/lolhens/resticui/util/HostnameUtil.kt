package de.lolhens.resticui.util

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object HostnameUtil {
    private const val DEFAULT_HOSTNAME = "android-device"

    fun detectHostname(context: Context): String {
        // Some Devices do not have a BluetoothAdapter e.g. the Android Emulator. They should not pass the permission check
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
                .adapter.name
        }

        return DEFAULT_HOSTNAME
    }
}