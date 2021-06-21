package de.lolhens.resticui.ui.folder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lolhens.resticui.databinding.ActivityFolderBinding

class FolderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}