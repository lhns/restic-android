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

    private var callbacks: Map<String, CompletableFuture<Boolean>> = emptyMap()

    fun onRequestPermissionsResult(permissions: Array<out String>, grantResults: IntArray) {
        val permission = permissions[0]
        val granted =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        val callback = callbacks.get(permission)
        if (callback != null) {
            synchronized(this) {
                callbacks = callbacks.minus(permission)
            }

            callback.complete(granted)
        }
    }

    fun request(
        activity: Activity,
        permission: String
    ): CompletableFuture<Boolean> =
        if (granted(activity, permission)) {
            CompletableFuture.completedFuture(true)
        } else {
            // Requesting the permission
            val future = CompletableFuture<Boolean>()

            synchronized(this) {
                callbacks = callbacks.plus(Pair(permission, future))
            }

            ActivityCompat.requestPermissions(activity, arrayOf(permission), 0)

            future
        }
}