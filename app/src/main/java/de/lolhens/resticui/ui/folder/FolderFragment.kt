package de.lolhens.resticui.ui.folder

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFolderBinding
import java.util.concurrent.CompletableFuture

class FolderFragment : Fragment() {
    private lateinit var folderViewModel: FolderViewModel
    private var _binding: FragmentFolderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var _folderId: FolderConfigId
    private val folderId: FolderConfigId get() = _folderId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        folderViewModel =
            ViewModelProvider(this).get(FolderViewModel::class.java)

        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _folderId = (requireActivity() as FolderActivity).folderId
        val folder = MainActivity.instance.config.folders.find { it.first.id == folderId }

        val textView: TextView = binding.folderTextFolder
        folderViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val progressBar = binding.folderProgress
        fun updateProgress(progress: Int) {
            CompletableFuture.runAsync {
                Thread.sleep(100)
                progressBar.setProgress(progress, true)
                updateProgress(if (progress == 100) 0 else progress + 1)
            }
        }

        updateProgress(0)

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
                    .setTitle(R.string.alert_delete_folder_title)
                    .setMessage(R.string.alert_delete_folder_message)
                    .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                        MainActivity.instance.configure { config ->
                            config.copy(folders = config.folders.filterNot { it.first.id == folderId })
                        }

                        requireActivity().finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
                true
            }
            R.id.action_edit -> {
                FolderActivity.start(this, true, folderId)

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