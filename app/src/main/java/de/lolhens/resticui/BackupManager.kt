package de.lolhens.resticui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import de.lolhens.resticui.config.*
import de.lolhens.resticui.restic.Restic
import de.lolhens.resticui.restic.ResticException
import de.lolhens.resticui.restic.ResticNameServers
import de.lolhens.resticui.restic.ResticStorage
import de.lolhens.resticui.ui.folder.FolderActivity
import de.lolhens.resticui.util.HostnameUtil
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.math.roundToInt

class BackupManager private constructor(context: Context) {
    companion object {
        private var _instance: BackupManager? = null

        fun instance(context: Context): BackupManager = _instance ?: BackupManager(context)
    }

    private fun notificationManager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val configManager: ConfigManager = ConfigManager(context)

    private val _config: MutableLiveData<Pair<Config, Runnable>> = MutableLiveData()
    val config: Config get() = _config.value!!.first
    fun observeConfig(lifecycleOwner: LifecycleOwner, f: (Config) -> Unit) =
        _config.observe(lifecycleOwner) { (config, _) -> f(config) }

    fun configure(f: (Config) -> Config): CompletableFuture<Unit> {
        val callback = CompletableFuture<Unit>()
        _config.postValue(Pair(f(config), Runnable {
            callback.complete(null)
        }))
        return callback
    }

    private lateinit var _restic: Restic
    val restic get() = _restic
    fun initRestic(context: Context) {
        val nameServers = config.nameServers
        val resticNameServers =
            if (nameServers != null)
                ResticNameServers.fromList(nameServers)
            else
                ResticNameServers.fromContext(context)
        _restic = Restic(
            ResticStorage.fromContext(context),
            hostname = config.hostname ?: HostnameUtil.detectHostname(context),
            nameServers = resticNameServers
        )
    }

    fun setHostname(hostname: String?, defaultHostname: () -> String): String {
        configure { config ->
            config.copy(hostname = hostname)
        }
        val hostname = hostname ?: defaultHostname()
        _restic = _restic.withHostname(hostname)
        return hostname
    }

    fun setNameServers(nameServers: List<String>?, context: Context): ResticNameServers {
        configure { config ->
            config.copy(nameServers = nameServers)
        }
        val nameServers =
            if (nameServers != null)
                ResticNameServers.fromList(nameServers)
            else
                ResticNameServers.fromContext(context)
        _restic = _restic.withNameServers(nameServers)
        return nameServers
    }

    val notificationChannelId = "RESTIC_BACKUP_PROGRESS"
    private var lastMillis = 0L

    private fun updateNotification(
        context: Context,
        folderConfigId: FolderConfigId,
        activeBackup: ActiveBackup,
        doneNotification: Boolean = true,
        errorNotification: Boolean = true
    ) {
        fun pendingIntent() = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            FolderActivity.intent(context, false, folderConfigId),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        when {
            activeBackup.inProgress -> {
                // reduce number of notification updates
                val nowMillis = System.currentTimeMillis()
                if ((nowMillis - lastMillis) < 300)
                    return
                else
                    lastMillis = nowMillis

                val progress = activeBackup.progress?.percentDoneString() ?: "0%"

                notificationManager(context).notify(
                    activeBackup.notificationId,
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setContentIntent(pendingIntent())
                        .setSubText(progress)
                        .setContentTitle("${context.resources.getString(R.string.notification_backup_progress_message)} $progress")
                        .setContentText(
                            if (activeBackup.progress == null) null
                            else "${activeBackup.progress.timeElapsedString()} elapsed"
                        )
                        .setSmallIcon(R.drawable.outline_cloud_24)
                        .setProgress(
                            100,
                            activeBackup.progress?.percentDone100()?.roundToInt() ?: 0,
                            activeBackup.isStarting()
                        )
                        .setOngoing(true)
                        .build()
                )
            }
            activeBackup.error != null && errorNotification -> {
                notificationManager(context).notify(
                    activeBackup.notificationId,
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setContentIntent(pendingIntent())
                        .setContentTitle(
                            "${context.resources.getString(R.string.notification_backup_failed_message)}\n${activeBackup.error}"
                        )
                        .setSmallIcon(R.drawable.outline_cloud_error_24)
                        .build()
                )
            }
            activeBackup.summary != null && doneNotification -> {
                var contentTitle = ""
                for (folder in config.folders) {
                    if (folder.id == folderConfigId) {
                        contentTitle = "${folder.path}"
                        break
                    }
                }
                val details = if (activeBackup.progress == null) "" else {
                    val ofTotal =
                        if (activeBackup.progress.total_files != null) "/${activeBackup.progress.total_files}" else ""

                    val unmodifiedNewChanged = listOf(
                        if (activeBackup.summary.files_unmodified != 0L) "U:${activeBackup.summary.files_unmodified}" else "",
                        if (activeBackup.summary.files_unmodified != 0L) "N:${activeBackup.summary.files_new}" else "",
                        if (activeBackup.summary.files_unmodified != 0L) "C:${activeBackup.summary.files_changed}" else ""
                    ).filter { it.isNotEmpty() }.joinToString("/")

                    listOf(
                        activeBackup.progress.timeElapsedString(),
                        "${activeBackup.progress.files_done}$ofTotal Files ($unmodifiedNewChanged)",
                        "${activeBackup.progress.bytesDoneString()}${if (activeBackup.progress.total_bytes != null) "/${activeBackup.progress.totalBytesString()}" else ""}"
                    ).joinToString(" | ")
                }
                notificationManager(context).notify(
                    activeBackup.notificationId,
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setContentIntent(pendingIntent())
                        .setSubText("100%")
                        .setContentTitle(contentTitle)
                        .setContentText(details)
                        .setSmallIcon(R.drawable.outline_cloud_done_24)
                        .build()
                )
            }
            else -> {
                notificationManager(context).cancel(activeBackup.notificationId)
            }
        }
    }

    init {
        _instance = this

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val title = context.resources.getString(R.string.notification_channel_backup)
            val channel = NotificationChannel(
                notificationChannelId,
                title,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = title
            notificationManager(context).createNotificationChannel(channel)
        }

        _config.value = Pair(configManager.readConfig(context), Runnable { })
        _config.observeForever { (config, callback) ->
            configManager.writeConfig(context, config)
            callback.run()
        }

        initRestic(context)
    }

    private val activeBackupsLock = Object()
    private var _activeBackups: Map<FolderConfigId, MutableLiveData<ActiveBackup>> = emptyMap()

    fun currentlyActiveBackups(): List<ActiveBackup> =
        synchronized(activeBackupsLock) {
            _activeBackups.values.map { it.value }.filterNotNull()
        }

    fun activeBackup(folderId: FolderConfigId): MutableLiveData<ActiveBackup> =
        synchronized(activeBackupsLock) {
            val liveData = _activeBackups[folderId]
            if (liveData == null) {
                val liveData = MutableLiveData<ActiveBackup>()
                _activeBackups = _activeBackups.plus(Pair(folderId, liveData))
                liveData
            } else {
                liveData
            }
        }

    fun backup(
        context: Context,
        folder: FolderConfig,
        removeOld: Boolean,
        scheduled: Boolean,
        callback: (() -> Unit)? = null
    ): ActiveBackup? {
        val repo = folder.repo(config) ?: return null

        val resticRepo = repo.repo(restic)

        val activeBackupLiveData = activeBackup(folder.id)
        if (activeBackupLiveData.value?.inProgress == true) return null

        val activeBackup = ActiveBackup.create()
        activeBackupLiveData.postValue(activeBackup)

        updateNotification(context, folder.id, activeBackup)

        resticRepo.backup(
            listOf(folder.path),
            { progress ->
                val activeBackupProgress = activeBackupLiveData.value!!.progress(progress)
                activeBackupLiveData.postValue(activeBackupProgress)
                updateNotification(context, folder.id, activeBackupProgress)
            },
            activeBackup.cancelFuture
        ).handle { summary, throwable ->
            val throwable =
                if (throwable == null) null
                else if (throwable is CompletionException && throwable.cause != null) throwable.cause
                else throwable

            val cancelled = throwable is ResticException && throwable.cancelled

            val errorMessage =
                if (throwable == null || cancelled) null
                else throwable.message

            val historyEntry = BackupHistoryEntry(
                timestamp = ZonedDateTime.now(),
                duration = HourDuration(Duration.ZERO),
                scheduled = scheduled,
                cancelled = cancelled,
                snapshotId = summary?.snapshot_id,
                errorMessage = errorMessage
            )

            configure { config ->
                config.copy(folders = config.folders.map { folder ->
                    if (folder.id == folder.id) folder.plusHistoryEntry(historyEntry)
                    else folder
                })
            }

            val finishedActiveBackup = activeBackupLiveData.value!!.finish(summary, errorMessage)
            activeBackupLiveData.postValue(finishedActiveBackup)
            updateNotification(context, folder.id, finishedActiveBackup)

            fun removeOldBackups(callback: () -> Unit) {
                if (removeOld && throwable == null && (folder.keepLast != null || folder.keepWithin != null)) {
                    resticRepo.forget(
                        folder.keepLast,
                        folder.keepWithin?.duration,
                        prune = true
                    ).handle { _, _ ->
                        callback()
                    }
                } else {
                    callback()
                }
            }

            removeOldBackups {
                resticRepo.unlock().handle { _, _ ->
                    if (callback != null) callback()
                }
            }
        }

        return activeBackup
    }
}