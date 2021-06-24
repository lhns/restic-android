package de.lolhens.resticui.ui.repo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import de.lolhens.resticui.R
import de.lolhens.resticui.databinding.ActivityRepoBinding

class RepoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityRepoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val edit = intent.extras!!.getBoolean("edit")

        if (edit) {
            val navController = findNavController(R.id.nav_host_fragment_activity_repo)
            navController.navigate(R.id.navigation_repo_edit)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish()
    }
}