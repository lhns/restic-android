package de.lolhens.resticui.restic

import android.Manifest
import android.content.Context
import android.os.Environment
import de.lolhens.resticui.Permissions
import java.io.File

interface ResticStorage {
    companion object {
        fun fromContext(context: Context): ResticStorage = object : ResticStorage {
            private val _lib = File(context.applicationInfo.nativeLibraryDir)
            override fun lib(): File = _lib
            override fun cache(): File = context.cacheDir
            override fun storage(): List<File> {
                if (!Permissions.granted(context, Manifest.permission.READ_EXTERNAL_STORAGE))
                    return emptyList()

                val state = Environment.getExternalStorageState()
                if (!(Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state))
                    return emptyList()

                val externalStorageDirectory = Environment.getExternalStorageDirectory()

                return listOf(
                    externalStorageDirectory
                ).filter { it.exists() }
            }
        }
    }

    fun lib(): File
    fun cache(): File
    fun storage(): List<File>
}