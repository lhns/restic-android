package de.lolhens.resticui.ui.repo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.lolhens.resticui.databinding.ActivityRepoBinding

class RepoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityRepoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}