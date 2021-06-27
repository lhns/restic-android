package de.lolhens.resticui.ui.folder

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.ActivityFolderBinding

class FolderActivity : AppCompatActivity() {
    companion object {
        fun start(fragment: Fragment, edit: Boolean, id: FolderConfigId) {
            val intent = Intent(fragment.requireContext(), FolderActivity::class.java)
            intent.putExtra("edit", edit)
            intent.putExtra("id", id.toString())
            fragment.startActivity(intent)
        }
    }

    private lateinit var _folderId: FolderConfigId
    val folderId: FolderConfigId get() = _folderId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val edit = intent.extras!!.getBoolean("edit")
        _folderId = FolderConfigId.fromString(intent.extras!!.getString("id")!!)

        if (edit) {
            val navController = findNavController(R.id.nav_host_fragment_activity_folder)
            navController.navigate(R.id.navigation_folder_edit)
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