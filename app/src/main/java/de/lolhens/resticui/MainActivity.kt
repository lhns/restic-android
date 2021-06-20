package de.lolhens.resticui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.lolhens.resticui.databinding.ActivityMainBinding
import de.lolhens.resticui.restic.Restic
import de.lolhens.resticui.restic.ResticStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var _restic: Restic

    val restic get() = _restic

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

        _restic = Restic(ResticStorage.fromContext(applicationContext))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.nav_menu_entry, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when {
            id == R.id.action_delete ->
                Toast.makeText(applicationContext, "Delete", Toast.LENGTH_SHORT).show()

            id == R.id.action_edit ->
                Toast.makeText(applicationContext, "Edit", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }
}