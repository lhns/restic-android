package de.lolhens.resticui.ui.folder

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfig
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFolderEditBinding
import java.io.File

class FolderEditFragment : Fragment() {
    private var _binding: FragmentFolderEditBinding? = null

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
        _binding = FragmentFolderEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _folderId = (requireActivity() as FolderActivity).folderId
        val config = MainActivity.instance.config
        val folder = config.folders.find { it.id == folderId }
        val folderRepo = folder?.repo(config)

        binding.spinnerRepo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            MainActivity.instance.config.repos.map { it.base.name }
        )

        val schedules = arrayOf("Hourly", "Daily", "Weekly", "Monthly")

        binding.spinnerSchedule.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            schedules
        )
        binding.spinnerSchedule.setSelection(1)

        val directoryPicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = result.data!!.data!!
                val path = ASFUriHelper.getPath(
                    requireContext(),
                    DocumentsContract.buildDocumentUriUsingTree(
                        uri,
                        DocumentsContract.getTreeDocumentId(uri)
                    )
                )
                binding.editFolder.setText(path)
            }

        binding.buttonFolderSelect.setOnClickListener { _ ->
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            directoryPicker.launch(Intent.createChooser(i, "Choose directory"))
        }

        if (folder != null && folderRepo != null) {
            binding.spinnerRepo.setSelection(MainActivity.instance.config.repos.indexOfFirst { it.base.id == folderRepo.base.id })
            binding.editFolder.setText(folder.path.path)
            binding.spinnerSchedule.setSelection(schedules.indexOfFirst { it == folder.schedule })
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_done -> {
                val selectedRepoName = binding.spinnerRepo.selectedItem.toString()
                val repo =
                    MainActivity.instance.config.repos.find { it.base.name == selectedRepoName }
                val path = binding.editFolder.text.toString()
                val schedule = binding.spinnerSchedule.selectedItem.toString()

                if (
                    repo != null &&
                    path.length > 0
                ) {
                    val folder = FolderConfig(
                        folderId,
                        repo.base.id,
                        File(path),
                        schedule
                    )

                    MainActivity.instance.configure { config ->
                        config.copy(folders = config.folders.filterNot { it.id == folderId }
                            .plus(folder))
                    }

                    FolderActivity.start(this, false, folderId)

                    requireActivity().finish()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}