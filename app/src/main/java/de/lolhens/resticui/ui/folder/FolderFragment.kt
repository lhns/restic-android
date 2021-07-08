package de.lolhens.resticui.ui.folder

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.lolhens.resticui.Backup
import de.lolhens.resticui.R
import de.lolhens.resticui.config.FolderConfigId
import de.lolhens.resticui.databinding.FragmentFolderBinding
import de.lolhens.resticui.restic.ResticSnapshotId
import de.lolhens.resticui.ui.snapshot.SnapshotActivity
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletionException
import kotlin.math.roundToInt

class FolderFragment : Fragment() {
    private var _binding: FragmentFolderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backup: Backup? = null
    private val backup get() = _backup!!

    private lateinit var _folderId: FolderConfigId
    private val folderId: FolderConfigId get() = _folderId

    private var snapshotIds: List<ResticSnapshotId>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setHasOptionsMenu(true)

        _backup = Backup.instance(requireContext())

        _folderId = (requireActivity() as FolderActivity).folderId
        val config = backup.config
        val folder = config.folders.find { it.id == folderId }
        val repo = folder?.repo(config)

        if (folder != null && repo != null) {
            binding.textRepo.setText(repo.base.name)
            binding.textFolder.setText(folder.path.path)
            binding.textSchedule.setText(folder.schedule)

            val resticRepo = repo.repo(backup.restic)

            backup.observeConfig(viewLifecycleOwner) { config ->
                val folder = config.folders.find { it.id == folderId }!!

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                binding.textLastBackup.setText(
                    if (folder.lastBackup == null) ""
                    else "Last Backup on ${folder.lastBackup.format(formatter)}"
                )

                resticRepo.snapshots().handle { snapshots, throwable ->
                    requireActivity().runOnUiThread {
                        binding.progressFolderSnapshots.visibility = GONE

                        // TODO: filter hostname
                        val snapshots =
                            snapshots?.filter { it.paths.contains(folder.path) }?.reversed()
                                ?: emptyList()

                        snapshotIds = snapshots.map { it.id }
                        binding.listFolderSnapshots.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            snapshots.map { "${it.time.format(formatter)} ${it.id.short}" }
                        )

                        if (throwable != null) {
                            val throwable =
                                if (throwable is CompletionException && throwable.cause != null) throwable.cause!!
                                else throwable

                            binding.textError.setText(throwable.message)
                            binding.textError.visibility = View.VISIBLE
                        }
                    }
                }
            }

            val activeBackup = backup.activeBackup(folderId)
            activeBackup.observe(viewLifecycleOwner) { backup ->
                println(backup)

                binding.progressBackupDetails.visibility =
                    if (backup.isStarting()) VISIBLE else GONE

                binding.textBackupDetails.visibility =
                    if (!backup.isStarting() && backup.error == null) VISIBLE else GONE

                binding.textBackupError.visibility =
                    if (backup.error != null) VISIBLE else GONE

                binding.buttonBackup.visibility =
                    if (!backup.isInProgress()) VISIBLE else GONE

                binding.buttonBackupCancel.visibility =
                    if (backup.isInProgress()) VISIBLE else GONE

                if (backup.isInProgress()) {
                    if (backup.progress != null) {
                        binding.progressBackup.setProgress(
                            (backup.progress.percentDone100()).roundToInt(),
                            true
                        )

                        val details = """
                            ${backup.progress.percentDoneString()} done / ${backup.progress.timeElapsedString()} elapsed
                            ${backup.progress.files_done}${if (backup.progress.total_files != null) " / ${backup.progress.total_files}" else ""} Files
                            ${backup.progress.bytesDoneString()}${if (backup.progress.total_bytes != null) " / ${backup.progress.totalBytesString()}" else ""}
                        """.trimIndent()

                        binding.textBackupDetails.setText(details)
                    }
                } else {
                    binding.progressBackup.setProgress(0, true)

                    if (backup.error != null) {
                        binding.textBackupError.setText(backup.error)
                    } else {
                        val details = """
                            Backup completed in ${backup.progress!!.timeElapsedString()}!
                            ${backup.progress.files_done}${if (backup.progress.total_files != null) " / ${backup.progress.total_files}" else ""} Files
                            ${backup.progress.bytesDoneString()}${if (backup.progress.total_bytes != null) " / ${backup.progress.totalBytesString()}" else ""}
                        """.trimIndent()

                        binding.textBackupDetails.setText(details)
                    }
                }
            }

            binding.listFolderSnapshots.setOnItemClickListener { _, _, position, _ ->
                val snapshotId = snapshotIds?.get(position)
                if (snapshotId != null) SnapshotActivity.start(this, folder.repoId, snapshotId)
            }

            binding.buttonBackup.setOnClickListener { _ ->
                backup.backup(requireContext(), folder)
            }

            binding.buttonBackupCancel.setOnClickListener { _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alert_backup_cancel_title)
                    .setMessage(R.string.alert_backup_cancel_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        activeBackup.value?.cancel()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.nav_menu_entry, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.alert_delete_folder_title)
                    .setMessage(R.string.alert_delete_folder_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        backup.configure { config ->
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
        _backup = null
        _binding = null
    }
}