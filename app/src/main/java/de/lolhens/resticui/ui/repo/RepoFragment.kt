package de.lolhens.resticui.ui.repo

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.RepoConfigId
import de.lolhens.resticui.databinding.FragmentRepoBinding
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletionException


class RepoFragment : Fragment() {
    private var _binding: FragmentRepoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var _repoId: RepoConfigId
    private val repoId: RepoConfigId get() = _repoId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRepoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _repoId = (requireActivity() as RepoActivity).repoId
        val repo = MainActivity.instance.config.repos.find { it.base.id == repoId }!!

        binding.textRepoName.setText(repo.base.name)

        val resticRepo = repo.repo(MainActivity.instance.restic)
        binding.textRepoUrl.setText(resticRepo.repository())

        resticRepo.snapshots().handle { snapshots, throwable ->
            requireActivity().runOnUiThread {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                binding.progressRepoSnapshots.visibility = GONE

                if (throwable == null) {
                    binding.listRepoSnapshots.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        snapshots.map { "${it.time.format(formatter)} ${it.id.short}\n${it.hostname} ${it.paths[0]}" }
                    )
                } else {
                    val throwable =
                        if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                        else throwable

                    binding.textError.setText(throwable.message)
                    binding.textError.visibility = VISIBLE
                }
            }
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alert_delete_repo_title)
                    .setMessage(R.string.alert_delete_repo_message)
                    .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                        MainActivity.instance.configure { config ->
                            config.copy(repos = config.repos.filterNot { it.base.id == repoId })
                        }

                        requireActivity().finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            R.id.action_edit -> {
                RepoActivity.start(this, true, repoId)

                requireActivity().finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}