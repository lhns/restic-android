package de.lolhens.resticui.ui.snapshot

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.ActivitySnapshotBinding
import de.lolhens.resticui.restic.ResticSnapshotId

class SnapshotActivity : AppCompatActivity() {
    companion object {
        fun start(fragment: Fragment, repoId: RepoConfigId, snapshotId: ResticSnapshotId) {
            val intent = Intent(fragment.requireContext(), SnapshotActivity::class.java)
            intent.putExtra("repoId", repoId.toString())
            intent.putExtra("snapshotId", snapshotId.toString())
            fragment.startActivity(intent)
        }
    }

    private lateinit var _repoId: RepoConfigId
    val repoId: RepoConfigId get() = _repoId

    private lateinit var _snapshotId: ResticSnapshotId
    val snapshotId: ResticSnapshotId get() = _snapshotId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val binding = ActivitySnapshotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _repoId = RepoConfigId.fromString(intent.extras!!.getString("repoId")!!)
        _snapshotId = ResticSnapshotId.fromString(intent.extras!!.getString("snapshotId")!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
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