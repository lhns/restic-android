package de.lolhens.resticui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.lolhens.resticui.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val backupManager = BackupManager.instance(applicationContext)

        if (!Permissions.hasStoragePermission(applicationContext, write = false)) {
            Permissions.requestStoragePermission(this, write = false)
                    .thenApply { granted ->
                        if (granted) {
                            backupManager.initRestic(applicationContext)
                        } else {
                            Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show()
                        }
                    }
        }

        BackupService.schedule(applicationContext)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(requestCode)
    }
}