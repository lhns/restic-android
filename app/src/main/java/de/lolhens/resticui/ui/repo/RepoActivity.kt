package de.lolhens.resticui.ui.repo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import de.lolhens.resticui.R
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.ActivityRepoBinding

class RepoActivity : AppCompatActivity() {
    companion object {
        fun start(fragment: Fragment, edit: Boolean, id: RepoConfigId) {
            val intent = Intent(fragment.requireContext(), RepoActivity::class.java)
            intent.putExtra("edit", edit)
            intent.putExtra("id", id.toString())
            fragment.startActivity(intent)
        }
    }

    private lateinit var _repoId: RepoConfigId
    val repoId: RepoConfigId get() = _repoId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val edit = intent.extras!!.getBoolean("edit")
        _repoId = RepoConfigId.fromString(intent.extras!!.getString("id")!!)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityRepoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (edit) {
            val navController = findNavController(R.id.nav_host_fragment_activity_repo)
            navController.navigate(R.id.navigation_repo_edit)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onBackPressed() {
        finish()
    }
}