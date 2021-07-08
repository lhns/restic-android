package de.lolhens.resticui

import android.app.job.JobParameters
import android.app.job.JobService
import de.lolhens.resticui.config.FolderConfig

class BackupService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val backup = Backup.instance(applicationContext)

        fun nextFolder(folders: List<FolderConfig>, callback: (() -> Unit)? = null) {
            if (folders.isEmpty()) {
                if (callback != null) callback()
            } else {
                val folder = folders.first()
                backup.backup(applicationContext, folder) {
                    nextFolder(folders.drop(1), callback)
                }
            }
        }

        nextFolder(backup.config.folders) {
            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}