package de.lolhens.resticui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.lolhens.resticui.config.Config
import de.lolhens.resticui.config.ConfigManager
import de.lolhens.resticui.databinding.ActivityMainBinding
import de.lolhens.resticui.restic.Restic
import de.lolhens.resticui.restic.ResticStorage

class MainActivity : AppCompatActivity() {
    companion object {
        private lateinit var _instance: MainActivity

        val instance: MainActivity get() = _instance
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var _configManager: ConfigManager
    private val configManager get() = _configManager

    private lateinit var _config: MutableLiveData<Config>
    val config: MutableLiveData<Config> get() = _config

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
        _config = MutableLiveData(configManager.readConfig())

        config.observe(this) { config ->
            configManager.writeConfig(config)
        }

        _restic = Restic(ResticStorage.fromContext(applicationContext))
    }

    fun configure(f: (Config) -> Config) {
        config.postValue(f(config.value!!))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Permissions.onRequestPermissionsResult(permissions, grantResults)
    }
}