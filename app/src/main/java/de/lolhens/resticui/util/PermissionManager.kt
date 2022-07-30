package de.lolhens.resticui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

abstract class PermissionManager {
    abstract fun hasStoragePermission(context: Context, write: Boolean): Boolean

    protected abstract fun requestStoragePermissionInternal(
        activity: ComponentActivity,
        write: Boolean
    ): CompletableFuture<Unit>

    fun requestStoragePermission(
        activity: ComponentActivity,
        write: Boolean
    ): CompletableFuture<Boolean> =
        requestStoragePermissionInternal(activity, write).thenApply {
            hasStoragePermission(activity, write)
        }

    open fun onRequestPermissionsResult(requestCode: Int) {}

    companion object {
        val instance: PermissionManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                RPermissionManager()
            } else {
                LegacyPermissionManager()
            }

        @RequiresApi(Build.VERSION_CODES.R)
        class RPermissionManager : PermissionManager() {
            override fun hasStoragePermission(context: Context, write: Boolean): Boolean =
                Environment.isExternalStorageManager()

            // https://stackoverflow.com/questions/62782648/android-11-scoped-storage-permissions
            // https://gist.github.com/Sonderman/db57d4407dfb496658786bb9c4d2fa5e
            override fun requestStoragePermissionInternal(
                activity: ComponentActivity,
                write: Boolean
            ): CompletableFuture<Unit> {
                val future = CompletableFuture<Unit>()

                val resultLauncher =
                    activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        future.complete(Unit)
                    }

                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", activity.packageName))
                    resultLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    resultLauncher.launch(intent)
                }

                return future
            }

        }

        private const val PERMISSION_REQUEST_CODE = 2290

        class LegacyPermissionManager : PermissionManager() {
            override fun hasStoragePermission(context: Context, write: Boolean): Boolean =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        (!write || ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED)

            private var legacyCallbacks: Queue<CompletableFuture<Unit>> = ConcurrentLinkedQueue()

            override fun requestStoragePermissionInternal(
                activity: ComponentActivity,
                write: Boolean
            ): CompletableFuture<Unit> {
                val future = CompletableFuture<Unit>()

                legacyCallbacks.offer(future)

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )

                return future
            }

            override fun onRequestPermissionsResult(requestCode: Int) {
                when (requestCode) {
                    PERMISSION_REQUEST_CODE ->
                        legacyCallbacks.poll()?.complete(Unit)
                }
            }
        }
    }
}
