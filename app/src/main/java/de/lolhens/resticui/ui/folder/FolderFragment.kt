package de.lolhens.resticui.ui.folder

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.lolhens.resticui.MainActivity
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFolderBinding
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletionException
import kotlin.math.roundToInt

class FolderFragment : Fragment() {
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
        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _folderId = (requireActivity() as FolderActivity).folderId
        val config = MainActivity.instance.config
        val folder = config.folders.find { it.id == folderId }!!
        val folderRepo = folder.repo(config)

        if (folderRepo != null) {
            binding.textRepo.setText(folderRepo.base.name)
            binding.textFolder.setText(folder.path.path)
            binding.textSchedule.setText(folder.schedule)

            val resticRepo = folderRepo.repo(MainActivity.instance.restic)

            resticRepo.snapshots().handle { snapshots, throwable ->
                requireActivity().runOnUiThread {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                    binding.progressFolderSnapshots.visibility = View.GONE

                    if (throwable == null) {
                        val snapshots = snapshots.filter { it.paths.contains(folder.path) }

                        binding.listFolderSnapshots.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            snapshots.map { "${it.time.format(formatter)} ${it.id.short}\n${it.hostname} ${it.paths[0]}" }
                        )
                    } else {
                        val throwable =
                            if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                            else throwable

                        binding.textError.setText(throwable.message)
                        binding.textError.visibility = View.VISIBLE
                    }
                }
            }

            binding.buttonBackup.setOnClickListener { view ->
                binding.buttonBackup.isEnabled = false
                binding.progressBackupDetails.visibility = VISIBLE

                resticRepo.backup(folder.path) { progress ->
                    requireActivity().runOnUiThread {
                        binding.progressBackupDetails.visibility = GONE

                        binding.progressBackup.setProgress(progress.percent_done.roundToInt(), true)

                        val details = """
                            ${progress.percentDoneString()} done / ${progress.timeElapsedString()} elapsed
                            ${progress.files_done}${if (progress.total_files != null) " / ${progress.total_files}" else ""} Files
                            ${progress.bytesDoneString()}${if (progress.total_bytes != null) " / ${progress.totalBytesString()}" else ""}
                        """.trimIndent()

                        binding.textBackupError.visibility = GONE
                        binding.textBackupDetails.setText(details)
                        binding.textBackupDetails.visibility = VISIBLE
                    }
                }.handle { summary, throwable ->
                    requireActivity().runOnUiThread {
                        binding.buttonBackup.isEnabled = true

                        if (throwable != null) {
                            val throwable =
                                if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                                else throwable

                            binding.textBackupDetails.visibility = GONE
                            binding.textBackupError.setText(throwable.message)
                            binding.textBackupError.visibility = VISIBLE
                        }
                    }
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
                    .setTitle(R.string.alert_delete_folder_title)
                    .setMessage(R.string.alert_delete_folder_message)
                    .setPositiveButton(android.R.string.ok) { dialog, buttonId ->
                        MainActivity.instance.configure { config ->
                            config.copy(folders = config.folders.filterNot { it.id == folderId })
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