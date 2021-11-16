package de.lolhens.resticui

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue


object Permissions {
    // https://stackoverflow.com/questions/62782648/android-11-scoped-storage-permissions
    fun hasStoragePermission(context: Context, write: Boolean): Boolean =
            if (SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                        context,
                        READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                        (!write || ContextCompat.checkSelfPermission(
                                context,
                                WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED)
            }

    private const val PERMISSION_REQUEST_CODE = 2290

    private var legacyCallbacks: Queue<CompletableFuture<Unit>> = ConcurrentLinkedQueue()

    fun requestStoragePermission(activity: ComponentActivity, write: Boolean): CompletableFuture<Boolean> {
        val future = CompletableFuture<Unit>()

        if (SDK_INT >= Build.VERSION_CODES.R) {
            val resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
        } else {
            //below android 11
            legacyCallbacks.offer(future)

            ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
            )
        }

        return future.thenApply {
            hasStoragePermission(activity, write)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE ->
                legacyCallbacks.poll()?.complete(Unit)
        }
    }
}