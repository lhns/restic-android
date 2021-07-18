package de.lolhens.resticui.ui.folder

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.lolhens.resticui.BackupManager
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfig
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFolderEditBinding
import de.lolhens.resticui.ui.Formatters
import java.io.File
import java.time.Duration

class FolderEditFragment : Fragment() {
    companion object {
        val schedules = arrayOf(
            Pair("Manual", -1),
            Pair("Hourly", 60),
            Pair("Daily", 24 * 60),
            Pair("Weekly", 7 * 24 * 60),
            Pair("Monthly", 30 * 24 * 60)
        )

        val retainProfiles = arrayOf(
            -1,
            1,
            2,
            6,
            1 * 24,
            3 * 24,
            5 * 24,
            10 * 24,
            30 * 24,
            60 * 24,
            90 * 24,
            120 * 24,
            365 * 24
        )
    }

    private var _binding: FragmentFolderEditBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backupManager: BackupManager? = null
    private val backupManager get() = _backupManager!!

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

        _backupManager = BackupManager.instance(requireContext())

        _folderId = (requireActivity() as FolderActivity).folderId
        val config = backupManager.config
        val folder = config.folders.find { it.id == folderId }
        val folderRepo = folder?.repo(config)

        binding.spinnerRepo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            backupManager.config.repos.map { it.base.name }
        )

        binding.spinnerSchedule.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            schedules.map { it.first }
        )
        binding.spinnerSchedule.setSelection(1)

        binding.spinnerRetainWithin.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            retainProfiles.map { hours ->
                if (hours == -1) "Always" else Formatters.durationDaysHours(
                    Duration.ofHours(hours.toLong())
                )
            }
        )
        binding.spinnerRetainWithin.setSelection(0)

        val directoryPicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = result.data!!.data!!
                val path =
                    FileUtil.getFullPathFromTreeUri(
                        requireContext(),
                        uri
                    ) ?: ASFUriHelper.getPath(
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
            binding.spinnerRepo.setSelection(backupManager.config.repos.indexOfFirst { it.base.id == folderRepo.base.id })
            binding.editFolder.setText(folder.path.path)
            binding.spinnerSchedule.setSelection(schedules.indexOfFirst { it.first == folder.schedule })
            val scheduleIndex = retainProfiles.indexOfFirst {
                it.toLong() == folder.keepWithin?.toHours()
            }
            binding.spinnerRetainWithin.setSelection(if (scheduleIndex == -1) 0 else scheduleIndex)
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
                    backupManager.config.repos.find { it.base.name == selectedRepoName }
                val path = binding.editFolder.text.toString()
                val schedule = binding.spinnerSchedule.selectedItem.toString()
                val keepWithin =
                    if (retainProfiles[binding.spinnerRetainWithin.selectedItemPosition] < 0) null
                    else Duration.ofHours(retainProfiles[binding.spinnerRetainWithin.selectedItemPosition].toLong())

                if (
                    repo != null &&
                    path.isNotEmpty()
                ) {
                    val prevFolder = backupManager.config.folders.find { it.id == folderId }

                    val folder = FolderConfig(
                        folderId,
                        repo.base.id,
                        File(path),
                        schedule,
                        prevFolder?.keepLast,
                        keepWithin,
                        prevFolder?.history ?: emptyList()
                    )

                    backupManager.configure { config ->
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
        _backupManager = null
        _binding = null
    }
}