package de.lolhens.resticui

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.lolhens.resticui.config.Config
import de.lolhens.resticui.config.ConfigManager
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.ActivityMainBinding
import de.lolhens.resticui.restic.Restic
import de.lolhens.resticui.restic.ResticStorage
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {
    companion object {
        private lateinit var _instance: MainActivity

        val instance: MainActivity get() = _instance
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var _configManager: ConfigManager
    private val configManager get() = _configManager

    private val _config: MutableLiveData<Pair<Config, Runnable>> = MutableLiveData()

    private lateinit var _restic: Restic

    val restic get() = _restic


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _instance = this

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        setupActionBarWithNavController(
            navController,
            AppBarConfiguration(
                setOf(
                    R.id.navigation_folders,
                    R.id.navigation_repos,
                    R.id.navigation_settings,
                    R.id.navigation_about
                )
            )
        )
        navView.setupWithNavController(navController)

        _configManager = ConfigManager(applicationContext)
        _config.value = Pair(configManager.readConfig(), Runnable { })
        _config.observe(this) { (config, callback) ->
            configManager.writeConfig(config)
            callback.run()
        }

        _restic = Restic(ResticStorage.fromContext(applicationContext))

        if (!Permissions.granted(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Permissions.request(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                .thenApply { granted ->
                    if (granted) {
                        _restic = Restic(ResticStorage.fromContext(applicationContext))
                    }
                }
        }
    }

    val config: Config get() = _config.value!!.first

    fun observeConfig(lifecycleOwner: LifecycleOwner, f: (Config) -> Unit) {
        _config.observe(lifecycleOwner) { (config, _) -> f(config) }
    }

    fun configure(f: (Config) -> Config): CompletableFuture<Unit> {
        val callback = CompletableFuture<Unit>()
        _config.postValue(Pair(f(config), Runnable {
            callback.complete(null)
        }))
        return callback
    }

    private val activeBackupsLock = Object()
    private var _activeBackups: Map<FolderConfigId, MutableLiveData<ActiveBackup>> = emptyMap()

    fun activeBackup(folderId: FolderConfigId): MutableLiveData<ActiveBackup> =
        synchronized(activeBackupsLock) {
            val liveData = _activeBackups.get(folderId)
            if (liveData == null) {
                val liveData = MutableLiveData<ActiveBackup>()
                _activeBackups = _activeBackups.plus(Pair(folderId, liveData))
                liveData
            } else {
                liveData
            }
        }

    /*fun cancelBackup

    fun backup(resticRepo: ResticRepo, folder: FolderConfig): CompletableFuture<ResticBackupSummary> {
        lateinit var future: CompletableFuture<ResticBackupSummary>
        future = resticRepo.backup(folder.path, { progress ->
            _resticBackupProgress.postValue(Pair(progress, future))
        })
        return future
    }*/

    //fun activeBackup(folderId: FolderConfigId): LiveData<ActiveBackup?> = throw RuntimeException()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Permissions.onRequestPermissionsResult(permissions, grantResults)
    }
}