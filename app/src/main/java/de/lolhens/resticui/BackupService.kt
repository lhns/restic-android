package de.lolhens.resticui

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import de.lolhens.resticui.config.FolderConfig
import java.time.ZonedDateTime

class BackupService : JobService() {
    companion object {
        fun schedule(context: Context) {
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            //jobScheduler.cancel(0)
            println("LIST JOBS:")
            //jobScheduler.
            val contains = jobScheduler.allPendingJobs.any { job ->
                val name = job.service.className
                println(name)
                name == BackupService::class.java.name
            }
            println(contains)
            if (!contains) {
                val serviceComponent = ComponentName(context, BackupService::class.java)
                val builder = JobInfo.Builder(0, serviceComponent)
                //builder.setMinimumLatency(2 * 60 * 1000L)
                //builder.setOverrideDeadline(3 * 60 * 1000L)
                builder.setPersisted(true)
                builder.setPeriodic(60 * 60 * 1000L, 30 * 1000L)

                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                //builder.setRequiresCharging(true)
                jobScheduler.schedule(builder.build())
            }
            jobScheduler.allPendingJobs.forEach { job ->
                println(job.service.className)
            }
        }

        fun startBackup(context: Context, callback: (() -> Unit)? = null) {
            val backupManager = BackupManager.instance(context)

            fun nextFolder(folders: List<FolderConfig>, callback: (() -> Unit)? = null) {
                if (folders.isEmpty()) {
                    if (callback != null) callback()
                } else {
                    val folder = folders.first()
                    fun next() = nextFolder(folders.drop(1), callback)

                    val now = ZonedDateTime.now()

                    val started =
                        folder.shouldBackup(now) && backupManager.backup(
                            context,
                            folder,
                            removeOld = true,
                            scheduled = true
                        ) {
                            next()
                        } != null
                    if (!started) {
                        next()
                    }
                }
            }

            nextFolder(backupManager.config.folders, callback)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE)
                as? WifiManager
            ?: throw IllegalStateException("Could not get system Context.WIFI_SERVICE")
        val createWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "restic")
        createWifiLock.setReferenceCounted(false)
        createWifiLock.acquire()

        startBackup(applicationContext) {
            createWifiLock.release()
            jobFinished(params, false)
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        val backupManager = BackupManager.instance(applicationContext)
        backupManager.currentlyActiveBackups().forEach { it.cancel() }

        // Wait for all backups to be cancelled to make sure the notification is dismissed
        while (backupManager.currentlyActiveBackups().isNotEmpty()) {
            Thread.sleep(100)
        }
        return true
    }
}