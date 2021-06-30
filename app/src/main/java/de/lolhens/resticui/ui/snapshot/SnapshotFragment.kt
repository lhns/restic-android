package de.lolhens.resticui.ui.snapshot

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.FragmentSnapshotBinding
import de.lolhens.resticui.restic.ResticSnapshotId

class SnapshotFragment : Fragment() {
    private var _binding: FragmentSnapshotBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


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

        val activity = requireActivity() as SnapshotActivity
        _repoId = activity.repoId
        _snapshotId = activity.snapshotId

        binding.textSnapshotId.setText(snapshotId.toString())

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alert_delete_snapshot_title)
                    .setMessage(R.string.alert_delete_snapshot_message)
                    .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                        val repo = MainActivity.instance.config.repos.find { it.base.id == repoId }
                        if (repo != null) {
                            val resticRepo = repo.repo(MainActivity.instance.restic)
                            resticRepo.forget(listOf(snapshotId)).handle { _, throwable ->
                                if (throwable == null) {
                                    MainActivity.instance.configure { config ->
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
        _binding = null
    }
}