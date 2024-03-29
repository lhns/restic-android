package de.lolhens.resticui.restic

import android.content.Context
import android.os.Environment
import de.lolhens.resticui.util.PermissionManager
import java.io.File

interface ResticStorage {
    companion object {
        fun fromContext(context: Context): ResticStorage = object : ResticStorage {
            private val _lib = File(context.applicationInfo.nativeLibraryDir)
            private val _cache = context.cacheDir

            override fun lib(): File = _lib
            override fun cache(): File = _cache
            override fun storage(): List<File> {
                if (!PermissionManager.instance.hasStoragePermission(context, write = true))
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