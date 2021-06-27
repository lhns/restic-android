package de.lolhens.resticui.ui.folder

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.ArrayAdapter
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

    val FolderSelect = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFolderEditBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _folderId = (requireActivity() as FolderActivity).folderId
        val folder = MainActivity.instance.config.folders.find { it.first.id == folderId }

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

        binding.buttonFolderSelect.setOnClickListener { view ->
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(i, "Choose directory"), FolderSelect)

        }

        if (folder != null) {
            binding.spinnerRepo.setSelection(MainActivity.instance.config.repos.indexOfFirst { it.base.id == folder.second.base.id })
            binding.editFolder.setText(folder.first.path.path)
            binding.spinnerSchedule.setSelection(schedules.indexOfFirst { it == folder.first.schedule })
        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            FolderSelect -> {
                val uri = data!!.data!!
                val path = ASFUriHelper.getPath(
                    requireContext(),
                    DocumentsContract.buildDocumentUriUsingTree(
                        uri,
                        DocumentsContract.getTreeDocumentId(uri)
                    )
                )
                binding.editFolder.setText(path)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.getItemId()) {
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
                    val folder = Pair(
                        FolderConfig(
                            folderId,
                            File(path),
                            schedule
                        ),
                        repo
                    )

                    MainActivity.instance.configure { config ->
                        config.copy(folders = config.folders.filterNot { it.first.id == folderId }
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