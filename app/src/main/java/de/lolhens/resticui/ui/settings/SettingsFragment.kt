package de.lolhens.resticui.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.lolhens.resticui.BackupManager
import de.lolhens.resticui.databinding.FragmentSettingsBinding
import de.lolhens.resticui.ui.InputDialogUtil
import de.lolhens.resticui.util.DirectoryChooser
import de.lolhens.resticui.util.HostnameUtil
import android.content.Context

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var _backupManager: BackupManager? = null
    private val backupManager get() = _backupManager!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _backupManager = BackupManager.instance(requireContext())

        binding.buttonUnlock.setOnClickListener {
            backupManager.config.repos.forEach { repo ->
                val resticRepo = repo.repo(backupManager.restic)
                resticRepo.unlock()
                    .handle { message, throwable ->
                        if (throwable != null) {
                            throwable.printStackTrace()
                        } else {
                            println(message)
                        }
                    }
            }
        }

        binding.buttonCleanup.setOnClickListener {
            backupManager.restic.cleanCache()
                .handle { message, throwable ->
                    if (throwable != null) {
                        throwable.printStackTrace()
                    } else {
                        println(message)
                    }
                }
        }

        val context = requireContext()
        val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        binding.textDl.text = sharedPref?.getString("dl_path", "") ?: ""

        val directoryChooser = DirectoryChooser.newInstance()

        directoryChooser.register(this, requireContext()) { path ->
            val editor = sharedPref?.edit()
            editor?.putString("dl_path", path)
            editor?.apply()
            binding.textDl.text = path
        }

        binding.buttonDlEdit.setOnClickListener {
            directoryChooser.openDialog()
        }

        val restic = BackupManager.instance(requireContext()).restic

        binding.buttonHostnameEdit.setOnClickListener {
            InputDialogUtil.showInputTextDialog(
                requireContext(),
                requireView(),
                binding.textHostnameDescription.text.toString(),
                binding.textHostname.text.toString()
            ) { hostname ->
                binding.textHostname.text = BackupManager.instance(requireContext()).setHostname(
                    if (hostname.isBlank()) null
                    else hostname.trim()
                ) {
                    HostnameUtil.detectHostname(requireContext())
                }
            }
        }

        binding.textHostname.text = restic.hostname

        binding.buttonDnsEdit.setOnClickListener {
            InputDialogUtil.showInputTextDialog(
                requireContext(),
                requireView(),
                binding.textDnsDescription.text.toString(),
                binding.textDns.text.toString()
            ) { nameServersString ->
                val nameServers =
                    if (nameServersString.isBlank()) emptyList()
                    else nameServersString.trim().split("\\s*,\\s*".toRegex())
                binding.textDns.text = BackupManager.instance(requireContext()).setNameServers(
                    nameServers.ifEmpty { null },
                    requireContext()
                ).nameServers().joinToString(", ")
            }
        }

        binding.textDns.text = restic.nameServers.nameServers().joinToString(", ")

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _backupManager = null
        _binding = null
    }
}