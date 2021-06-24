package de.lolhens.resticui.ui.folder

import android.R
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.lolhens.resticui.databinding.ActivityFolderBinding

class FolderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.home -> {
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