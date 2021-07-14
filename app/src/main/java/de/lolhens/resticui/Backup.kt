package de.lolhens.resticui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import de.lolhens.resticui.config.Config
import de.lolhens.resticui.config.ConfigManager
import de.lolhens.resticui.config.FolderConfig
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.restic.*
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.math.roundToInt

class Backup private constructor(context: Context) {
    companion object {
        private var _instance: Backup? = null

        fun instance(context: Context): Backup = _instance ?: Backup(context)
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
        _restic = Restic(ResticStorage.fromContext(context))
    }

    val notificationChannelId = "RESTIC_BACKUP_PROGRESS"

    private fun backupProgressNotification(
        context: Context,
        activeBackup: ActiveBackup,
        progress: ResticBackupProgress?,
        doneNotification: Boolean = false
    ) {
        when {
            progress != null -> {
                notificationManager(context).notify(
                    activeBackup.notificationId,
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setContentTitle(context.resources.getString(R.string.notification_backup_title))
                        .setContentTitle("${context.resources.getString(R.string.notification_backup_progress_message)} ${progress.percentDoneString()}")
                        .setSmallIcon(R.drawable.outline_cloud_24)
                        .setProgress(100, progress.percentDone100().roundToInt(), false)
                        .setOngoing(true)
                        .build()
                )
            }
            doneNotification -> {
                notificationManager(context).notify(
                    activeBackup.notificationId,
                    NotificationCompat.Builder(context, notificationChannelId)
                        .setContentTitle(context.resources.getString(R.string.notification_backup_title))
                        .setContentTitle(context.resources.getString(R.string.notification_backup_done_message))
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
        callback: (() -> Unit)? = null
    ): Boolean {
        val repo = folder.repo(config) ?: return false

        val resticRepo = repo.repo(restic)

        val activeBackupLiveData = activeBackup(folder.id)
        if (activeBackupLiveData.value?.isInProgress() == true) return false

        val activeBackup = ActiveBackup.create()
        activeBackupLiveData.postValue(activeBackup)

        backupProgressNotification(context, activeBackup, ResticBackupProgress.zero())

        resticRepo.backup(
            ResticRepo.hostname,
            folder.path,
            { progress ->
                activeBackupLiveData.postValue(activeBackupLiveData.value!!.copy(progress = progress))
                backupProgressNotification(context, activeBackup, progress)
            },
            activeBackup.cancelFuture
        ).handle { summary, throwable ->
            val throwable = if (throwable == null) null else {
                if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                else throwable
            }

            val error =
                if (throwable == null) {
                    val now = ZonedDateTime.now()

                    configure { config ->
                        config.copy(folders = config.folders.map { folder ->
                            if (folder.id == folder.id) folder.copy(lastBackup = now)
                            else folder
                        })
                    }

                    null
                } else if (throwable is ResticException && throwable.cancelled) {
                    ""
                } else {
                    throwable.message
                }

            activeBackupLiveData.postValue(
                activeBackupLiveData.value!!.copy(
                    summary = summary,
                    error = error
                )
            )

            backupProgressNotification(context, activeBackup, null, doneNotification = true)

            fun removeOldBackups(callback: () -> Unit) {
                if (removeOld && throwable == null && (folder.keepLast != null || folder.keepWithin != null)) {
                    resticRepo.forget(
                        folder.keepLast,
                        folder.keepWithin,
                        prune = false // TODO: prune
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

        return true
    }
}