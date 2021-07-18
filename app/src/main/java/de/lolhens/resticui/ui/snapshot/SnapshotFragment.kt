package de.lolhens.resticui.ui.snapshot

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import de.lolhens.resticui.BackupManager
import de.lolhens.resticui.R
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.FragmentSnapshotBinding
import de.lolhens.resticui.restic.ResticSnapshotId
import de.lolhens.resticui.ui.Formatters

class SnapshotFragment : Fragment() {
    private var _binding: FragmentSnapshotBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backupManager: BackupManager? = null
    private val backupManager get() = _backupManager!!

    private lateinit var _repoId: RepoConfigId
    private val repoId: RepoConfigId get() = _repoId

    private lateinit var _snapshotId: ResticSnapshotId
    private val snapshotId: ResticSnapshotId get() = _snapshotId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSnapshotBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _backupManager = BackupManager.instance(requireContext())

        val activity = requireActivity() as SnapshotActivity
        _repoId = activity.repoId
        _snapshotId = activity.snapshotId

        binding.textSnapshotId.text = snapshotId.short
        binding.textSnapshotIdLong.text = snapshotId.id

        val repo = backupManager.config.repos.find { it.base.id == repoId }

        if (repo != null) {
            val resticRepo = repo.repo(backupManager.restic)

            resticRepo.cat(snapshotId).handle { snapshot, throwable ->
                requireActivity().runOnUiThread {
                    binding.progressSnapshot.visibility = GONE

                    if (snapshot != null) {
                        binding.textTime.text = "Created on ${Formatters.dateTime(snapshot.time)}"
                        binding.textHostname.text = snapshot.hostname
                        binding.textPath.text = snapshot.paths[0].path
                    } else {
                        throwable?.printStackTrace()
                    }
                }
            }
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alert_delete_snapshot_title)
                    .setMessage(R.string.alert_delete_snapshot_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val repo = backupManager.config.repos.find { it.base.id == repoId }
                        if (repo != null) {
                            val resticRepo = repo.repo(backupManager.restic)

                            item.isEnabled = false
                            binding.progressSnapshotDelete.visibility = VISIBLE

                            resticRepo.forget(listOf(snapshotId), prune = true)
                                .handle { _, throwable ->
                                    if (throwable == null) {
                                        backupManager.configure { config ->
                                            config
                                        }

                                        requireActivity().finish()
                                    } else {
                                        throwable.printStackTrace()
                                    }
                                }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _backupManager = null
        _binding = null
    }
}