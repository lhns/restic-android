package de.lolhens.resticui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.CompletableFuture

object Permissions {
    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    fun request(
        context: Context,
        permission: String
    ): CompletableFuture<Boolean> =
        if (granted(context, permission)) {
            CompletableFuture.completedFuture(true)
        } else {
            // Requesting the permission
            val future = CompletableFuture<Boolean>()
            ActivityCompat.requestPermissions(object : Activity() {
                override fun onRequestPermissionsResult(
                    requestCode: Int,
                    permissions: Array<out String>,
                    grantResults: IntArray
                ) {
                    future.complete(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                }
            }, arrayOf(permission), 0)
            future
        }
}